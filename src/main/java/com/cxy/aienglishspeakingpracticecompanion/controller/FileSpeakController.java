package com.cxy.aienglishspeakingpracticecompanion.controller;

import com.cxy.aienglishspeakingpracticecompanion.aop.AuthCheck;
import com.cxy.aienglishspeakingpracticecompanion.common.BaseResponse;
import com.cxy.aienglishspeakingpracticecompanion.common.ResultUtils;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.ConversationSummaryResponse;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.SceneOpeningRequest;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.VoiceChatRequest;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.VoiceChatResponse;
import com.cxy.aienglishspeakingpracticecompanion.excption.BusinessException;
import com.cxy.aienglishspeakingpracticecompanion.excption.ErrorCode;
import com.cxy.aienglishspeakingpracticecompanion.service.SpeakService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/voice")
public class FileSpeakController {
    @Resource
    private SpeakService speakService;

    /**
     * 用户点击录制，录制结束后整体上传音频进行对话
     */
    @PostMapping("/file-chat")
    @AuthCheck
    public ResponseEntity<BaseResponse<VoiceChatResponse>> fileChat(
            @ModelAttribute VoiceChatRequest  request) {
        MultipartFile file = request.getAudioFile();
        String sessionId = request.getSessionId();
        String scene = request.getScene();
        String customScenario = request.getCustomScenario();
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"语音不能为空");
        }
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"会话ID不能为空");
        }
        VoiceChatResponse voiceChatResponse = speakService.speakWithAI(file, sessionId, scene,customScenario);
        return ResponseEntity.ok(ResultUtils.success(voiceChatResponse));
    }

    /**
     * 获取会话摘要
     */
    @AuthCheck
    @GetMapping("/summary")
    public ResponseEntity<BaseResponse<ConversationSummaryResponse>> getSummary(@RequestParam String sessionId){
        if(sessionId== null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"会话ID不能为空");
        }
        ConversationSummaryResponse conversationSummaryResponse = speakService.ConversationSummary(sessionId);
        return ResponseEntity.ok(ResultUtils.success(conversationSummaryResponse));
    }

    /***
     * 获取场景开场
     * @param request
     * @return
     */
    @PostMapping("/scene-opening")
    @AuthCheck
    public ResponseEntity<BaseResponse<VoiceChatResponse>> sceneOpening(
            @RequestBody SceneOpeningRequest request) {
        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话ID不能为空");
        }

        VoiceChatResponse response = speakService.sceneOpening(
                request.getSessionId(),
                request.getScene(),
                request.getCustomScenario()
        );

        return ResponseEntity.ok(ResultUtils.success(response));
    }
}