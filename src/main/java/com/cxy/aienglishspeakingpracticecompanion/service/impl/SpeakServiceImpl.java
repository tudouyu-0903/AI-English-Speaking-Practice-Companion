package com.cxy.aienglishspeakingpracticecompanion.service.impl;

import cn.hutool.json.JSONUtil;
import com.cxy.aienglishspeakingpracticecompanion.app.FileAsrApp;
import com.cxy.aienglishspeakingpracticecompanion.app.SpeakApp;
import com.cxy.aienglishspeakingpracticecompanion.app.TtsApp;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.ConversationSummaryResponse;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.VoiceChatResponse;
import com.cxy.aienglishspeakingpracticecompanion.excption.BusinessException;
import com.cxy.aienglishspeakingpracticecompanion.excption.ErrorCode;
import com.cxy.aienglishspeakingpracticecompanion.service.SpeakService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.util.UUID;

@Service
@Slf4j
public class SpeakServiceImpl implements SpeakService {
    @Resource
    private FileAsrApp fileAsrApp;
    @Resource
    private SpeakApp speakApp;
    @Resource
    private TtsApp ttsApp;

    /***
     * 语音对话
     * @param multipartFile 用户的录音文件
     * @param sessionId 会话ID
     * @return
     */
    @Override
    public VoiceChatResponse speakWithAI(MultipartFile multipartFile, String sessionId,String scene,String customScenario ) {
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

            // 4. 将识别出的文字喂给你的带多轮记忆的大模型
            //判断场景是自定义的还是选择 场景
            String aiReplyText ;
            if (customScenario == null || customScenario.isBlank()){
                //系统预设场景
                aiReplyText = speakApp.sceneSpeak(userRecognizedText,sessionId, scene);
            }else {
                //自定义场景
                aiReplyText =  speakApp.customSpeak(userRecognizedText,sessionId,customScenario) ;
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

        } catch (Exception e) {
            log.error("对话系统全链路发生故障", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"系统内部错误: " + e.getMessage());
        } finally {
            // 释放本地服务器的临时文件
            if (localTempFile != null && localTempFile.exists()) {
                boolean deleted = localTempFile.delete();
                log.info("临时音频文件清理状态: {}", deleted);
            }
        }
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


}
