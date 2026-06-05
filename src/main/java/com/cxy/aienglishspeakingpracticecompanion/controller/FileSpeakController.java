package com.cxy.aienglishspeakingpracticecompanion.controller;

import com.cxy.aienglishspeakingpracticecompanion.common.BaseResponse;
import com.cxy.aienglishspeakingpracticecompanion.common.ResultUtils;
import com.cxy.aienglishspeakingpracticecompanion.dto.VoiceChatResponse;
import com.cxy.aienglishspeakingpracticecompanion.excption.BusinessException;
import com.cxy.aienglishspeakingpracticecompanion.excption.ErrorCode;
import com.cxy.aienglishspeakingpracticecompanion.service.SpeakService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
    public ResponseEntity<BaseResponse<VoiceChatResponse>> fileChat(
            @RequestParam("audioFile") MultipartFile file,
            @RequestParam("sessionId") String sessionId) {

        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"语音不能为空");
        }
        VoiceChatResponse voiceChatResponse = speakService.speakWithAI(file, sessionId);
        return ResponseEntity.ok(ResultUtils.success(voiceChatResponse));
    }
}