package com.cxy.aienglishspeakingpracticecompanion.app;

import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/***
 * 接收大模型的文字，调用百炼，把生成的音频 URL 字符串直接返回。
 * 官方文档连接https://bailian.console.aliyun.com/cn-beijing?spm=5176.12818093_47.resourceCenter.1.3f3c16d0OiyGbJ&tab=doc#/doc/?type=model&url=2879134
 */
@Slf4j
@Component
public class TtsApp {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    static {
        Constants.baseHttpApiUrl = "https://dashscope.aliyuncs.com/api/v1";
    }

    /**
     * 将 AI 回复的文本转换为英文语音链接
     * @param textText 大模型输出的英文文本
     * @return 阿里云 OSS 上的 .wav 音频下载链接
     */
    public String textToSpeechUrl(String textText) {
        try {
            log.info("开始调用百炼 TTS 模型合成语音, 文本长度: {}", textText.length());
            
            MultiModalConversation conv = new MultiModalConversation();
            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(apiKey)
                    .model("qwen3-tts-flash")
                    .text(textText)
                    .voice(AudioParameters.Voice.CHERRY)
                    .languageType("English") //指定发音语种为英文
                    .build();

            MultiModalConversationResult result = conv.call(param);
            
            if (result == null || result.getOutput() == null || result.getOutput().getAudio() == null) {
                throw new RuntimeException("阿里云 TTS 服务未返回合法的音频结果");
            }

            String audioUrl = result.getOutput().getAudio().getUrl();
            log.info("TTS 语音合成成功！阿里临时音频下载 URL: {}", audioUrl);
            return audioUrl;
        } catch (Exception e) {
            log.error("TTS 语音合成发生链路阻断", e);
            throw new RuntimeException("TTS Synthesis Failed: " + e.getMessage());
        }
    }
}