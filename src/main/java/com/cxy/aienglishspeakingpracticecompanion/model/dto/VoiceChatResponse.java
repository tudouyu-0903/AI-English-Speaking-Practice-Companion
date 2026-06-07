package com.cxy.aienglishspeakingpracticecompanion.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoiceChatResponse implements Serializable {
    // AI 恢复的文本（用于前端在聊天框渲染文字）
    private String aiText;
    // AI 回复的语音 HTTPS 链接（用于前端控制喇叭播放声音）
    private String aiAudioUrl;
}