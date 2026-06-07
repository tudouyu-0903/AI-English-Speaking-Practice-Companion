package com.cxy.aienglishspeakingpracticecompanion.service.impl;

import cn.hutool.json.JSONUtil;
import com.cxy.aienglishspeakingpracticecompanion.app.FileAsrApp;
import com.cxy.aienglishspeakingpracticecompanion.app.SpeakApp;
import com.cxy.aienglishspeakingpracticecompanion.app.TtsApp;
import com.cxy.aienglishspeakingpracticecompanion.chatMemory.RedisChatMemory;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.ConversationSummaryResponse;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.VoiceChatResponse;
import com.cxy.aienglishspeakingpracticecompanion.excption.BusinessException;
import com.cxy.aienglishspeakingpracticecompanion.excption.ErrorCode;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.VoiceTranscribeResponse;
import com.cxy.aienglishspeakingpracticecompanion.service.SpeakService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SpeakServiceImpl implements SpeakService {
    @Resource
    private FileAsrApp fileAsrApp;
    @Resource
    private SpeakApp speakApp;
    @Resource
    private TtsApp ttsApp;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final long SCENE_EXPIRE_SECONDS = 30 * 24 * 60 * 60;
    @Resource
    private RedisChatMemory redisChatMemory;

    /***
     * 用户语音转文字
     * @param multipartFile
     * @param sessionId
     * @return
     */
    @Override
    public VoiceTranscribeResponse transcribeVoice(MultipartFile multipartFile, String sessionId) {
        File localTempFile = null;
        try {
            // 1. 在服务器本地创建一个临时文件，避免文件名冲突 (比如：D:/temp/voice_xyz.wav)
            String systemTempDir = System.getProperty("java.io.tmpdir");
            //保留前端上传音频的真实后缀，避免文件内容是 webm/m4a/mp3 却被保存成 wav。
            String originalFilename = multipartFile.getOriginalFilename();
            String suffix = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".wav";
            String customFileName = "voice_" + UUID.randomUUID() + suffix;
            localTempFile = new File(systemTempDir, customFileName);

            // 2. 将前端上传的流保存到这个临时文件中
            multipartFile.transferTo(localTempFile);
            log.info("临时物理文件大小: {} bytes", localTempFile.length());
            log.info("前端音频文件已成功落地到服务器本地: {}", localTempFile.getAbsolutePath());

            // 3. 扔给改版后的 FileAsrService 进行本地识别（SDK会自动把这个文件传给阿里）
            String userRecognizedText = fileAsrApp.transcribeLocalFile(localTempFile);

            if (userRecognizedText == null || userRecognizedText.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "识别结果为空");
            }

            VoiceTranscribeResponse response = new VoiceTranscribeResponse();
            response.setUserText(userRecognizedText);
            response.setAsrTaskId("asr-" + UUID.randomUUID());
            return response;
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "语音识别失败: " + e.getMessage());
        } finally {
            if (localTempFile != null && localTempFile.exists()) {
                localTempFile.delete();
            }
        }
    }

    /***
     * 将用户确认后的回答交给AI
     * @param userText
     * @param sessionId
     * @param scene
     * @param customScenario
     * @return
     */
    @Override
    public VoiceChatResponse chatWithConfirmedText(String userText, String sessionId, String scene, String customScenario) {
        // 将识别出的文字喂给你的带多轮记忆的大模型
        //判断场景是自定义的还是选择 场景
        String aiReplyText ;
        if (customScenario == null || customScenario.isBlank()){
            //系统预设场景
            aiReplyText = speakApp.sceneSpeak(userText,sessionId, scene);
        }else {
            //自定义场景
            aiReplyText =  speakApp.customSpeak(userText,sessionId,customScenario) ;
        }
        if (aiReplyText == null || aiReplyText.trim().isEmpty()){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI回复为空");
        }

        // 5. 将AI回复转换成语音
        String speechUrl = ttsApp.textToSpeechUrl(aiReplyText);
        if (speechUrl == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"语音合成失败");
        }
        return new VoiceChatResponse(aiReplyText, speechUrl);
    }

    /***
     * 会话总结
     * @param sessionId 会话ID
     * @return 返回会话总结结果
     */
    @Override
    public ConversationSummaryResponse ConversationSummary(String sessionId) {
        String summariedSpeak = speakApp.summarySpeak(sessionId);
        ConversationSummaryResponse response = JSONUtil.toBean(summariedSpeak, ConversationSummaryResponse.class);
        if (response == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI总结结果格式错误");
        }
        return response;
    }

    /**
     * 设置对话场景开场白
     * @param sessionId
     * @param scene
     * @param customScenario
     * @return
     */
    @Override
    public VoiceChatResponse sceneOpening(String sessionId, String scene, String customScenario) {
        //预设场景
        if (customScenario == null || customScenario.isBlank()) {
            String cacheKey = "scene:opening:" + scene;
            //查询缓存是否已经生成对应场景
            String cachedResult = stringRedisTemplate.opsForValue().get(cacheKey);
            //存在
            if (cachedResult != null && !cachedResult.isBlank()) {
                VoiceChatResponse cached = JSONUtil.toBean(cachedResult, VoiceChatResponse.class);
                if (cached != null) {
                    saveOpeningToMemory(sessionId, cached.getAiText());
                    return cached;
                }
            }
            //不存在
            //AI生成回答
            String aiText = speakApp.sceneOpening(sessionId, scene);
            String audioUrl = ttsApp.textToSpeechUrl(aiText);
            //返回响应
            VoiceChatResponse response = new VoiceChatResponse(aiText, audioUrl);
            //存入缓存
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(response),SCENE_EXPIRE_SECONDS,TimeUnit.SECONDS);
            return response;
        }

        // 自定义场景不缓存，或者按 prompt hash 缓存
        String aiText = speakApp.customSceneOpening(sessionId, customScenario);
        String audioUrl = ttsApp.textToSpeechUrl(aiText);
        return new VoiceChatResponse(aiText, audioUrl);
    }
    private void saveOpeningToMemory(String sessionId, String aiText) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话ID不能为空");
        }
        if (aiText == null || aiText.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI开场白为空");
        }

        redisChatMemory.add(sessionId, List.of(
                new AssistantMessage(aiText)
        ));
    }


}
