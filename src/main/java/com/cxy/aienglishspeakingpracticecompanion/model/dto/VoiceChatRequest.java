package com.cxy.aienglishspeakingpracticecompanion.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoiceChatRequest {
    /***
     * 会话ID
     */
    private String sessionId;
    /***
     * 场景
     */
    private String scene;
    /***
     * 自定义场景描述
     */
    private String customScenario;
    /***
     * 音频文件
     */
    private MultipartFile audioFile;
}
