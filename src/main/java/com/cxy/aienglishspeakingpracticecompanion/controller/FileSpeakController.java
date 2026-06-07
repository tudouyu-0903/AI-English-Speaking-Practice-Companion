package com.cxy.aienglishspeakingpracticecompanion.controller;

import com.cxy.aienglishspeakingpracticecompanion.aop.AuthCheck;
import com.cxy.aienglishspeakingpracticecompanion.common.BaseResponse;
import com.cxy.aienglishspeakingpracticecompanion.common.ResultUtils;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.*;
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

    /***
     * 用户语音转文字
     * @param audioFile
     * @param sessionId
     * @return
     */
    @PostMapping("/transcribe")
    @AuthCheck
    public ResponseEntity<BaseResponse<VoiceTranscribeResponse>> transcribe(
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam("sessionId") String sessionId) {

        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "语音不能为空");
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话ID不能为空");
        }

        VoiceTranscribeResponse response = speakService.transcribeVoice(audioFile, sessionId);
        return ResponseEntity.ok(ResultUtils.success(response));
    }

    /***
     * AI回答用户问题
     * @param request
     * @return
     */
    @PostMapping("/text-chat")
    @AuthCheck
    public ResponseEntity<BaseResponse<VoiceChatResponse>> textChat(
            @RequestBody TextChatRequest request) {

        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话ID不能为空");
        }
        if (request.getUserText() == null || request.getUserText().isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户文本不能为空");
        }

        VoiceChatResponse response = speakService.chatWithConfirmedText(
                request.getUserText(),
                request.getSessionId(),
                request.getScene(),
                request.getCustomScenario()
        );

        return ResponseEntity.ok(ResultUtils.success(response));
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