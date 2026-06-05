package com.cxy.aienglishspeakingpracticecompanion.app;

import com.alibaba.dashscope.audio.qwen_asr.*;
import com.alibaba.dashscope.utils.Constants;
import com.cxy.aienglishspeakingpracticecompanion.excption.BusinessException;
import com.cxy.aienglishspeakingpracticecompanion.excption.ErrorCode;
import com.cxy.aienglishspeakingpracticecompanion.untils.OssOperation;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
@Slf4j
@Component
/***
 * 参考官方文档将语音转文字
 * https://bailian.console.aliyun.com/cn-beijing?spm=5176.12818093_47.resourceCenter.1.3f3c16d08f1t6e&tab=doc#/doc/?type=model&url=2979031
 * 语音转文字
 */
public class FileAsrApp {
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    static {
        Constants.baseHttpApiUrl = "https://dashscope.aliyuncs.com/api/v1";
    }
    @Resource
    private OssOperation ossOperation;
    /**
     * ⚡ 改造后：直接传入本地 File 对象，摆脱对 OSS 的依赖
     * @param localAudioFile 本地保存好的音频文件（从前端 MultipartFile 转化而来）
     * @return 识别出的完整英文字符串
     */
    public String transcribeLocalFile(File localAudioFile) {
        try {
            long startTime = System.currentTimeMillis();

            // 2. 💥【破局核心】：调用你的新版 OssService，真正把文件推到你的私有 Bucket
            // 这步会返回一个带 10分钟 V4 加密签名的、以 https:// 开头的合法下载链接
            log.info("开始调用自建 OssService 上传录音文件...");
            String absoluteHttpsUrl = ossOperation.uploadMultipartFile(localAudioFile);
            log.info("自建 OSS 加密外链生成成功！网络路径为: {}", absoluteHttpsUrl);

            // 3. 严格对齐非实时转写参数，塞入绝对可被外网下载的 https 链接
            QwenTranscriptionParam param = QwenTranscriptionParam.builder()
                    .apiKey(apiKey)
                    .model("qwen3-asr-flash-filetrans")
                    .fileUrl(absoluteHttpsUrl) // 👈 此时传入的链接完美符合百炼模型规范
                    .parameter("enable_itn", false)
                    .parameter("enable_words", false)
                    .build();

            QwenTranscription transcription = new QwenTranscription();

            // 4. 提交异步任务
            QwenTranscriptionResult result = transcription.asyncCall(param);
            String taskId = result.getTaskId();
            log.info("成功创建非实时语音识别任务，TaskId: {}", taskId);

            // 5. 阻塞轮询，静静等待阿里云在云端把这端音频切片、识别完毕
            result = transcription.wait(QwenTranscriptionQueryParam.FromTranscriptionParam(param, taskId));
            log.info("语音转写任务处理完毕，总耗时: {}ms", (System.currentTimeMillis() - startTime));

            // Codex 修改：DashScope 文件转写成功状态是 SUCCEEDED，不是 SUCCESS；忽略大小写避免 SDK 返回格式差异。
            if (result == null || !"SUCCEEDED".equalsIgnoreCase(String.valueOf(result.getTaskStatus()))) {
                throw new RuntimeException("阿里云百炼服务端判定任务失败。原始完整响应为: " + result);
            }

            // 7. 获取转写结果
            QwenTranscriptionTaskResult taskResult = result.getResult();
            if (taskResult == null || taskResult.getTranscriptionUrl() == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"任务状态为 SUCCEEDED，但未正确获得文字结果的 URL。");
            }

            // 8. 顺着阿里给出的临时结果 JSON 链接，清洗并抓取最终的英文字符串
            return fetchAndParseJsonResult(taskResult.getTranscriptionUrl());

        } catch (Exception e) {
            log.error("非实时语音识别链路发生故障", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"File ASR Failed: " + e.getMessage());
        }
    }

    private String fetchAndParseJsonResult(String fileUrl) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(fileUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            JsonObject rootJson = JsonParser.parseReader(reader).getAsJsonObject();
            // Codex 修改：Qwen3 ASR 文件转写结果通常返回 transcripts；同时兼容旧字段 user_transcripts。
            JsonArray transcripts = null;
            if (rootJson.has("transcripts")) {
                transcripts = rootJson.getAsJsonArray("transcripts");
            } else if (rootJson.has("user_transcripts")) {
                transcripts = rootJson.getAsJsonArray("user_transcripts");
            }
            if (transcripts != null) {
                StringBuilder textBuilder = new StringBuilder();
                for (int i = 0; i < transcripts.size(); i++) {
                    JsonObject transcript = transcripts.get(i).getAsJsonObject();
                    if (transcript.has("text")) {
                        textBuilder.append(transcript.get("text").getAsString()).append(" ");
                    }
                    // Codex 修改：兼容 sentences 分句结构，避免识别成功但解析为空。
                    if (transcript.has("sentences")) {
                        JsonArray sentences = transcript.getAsJsonArray("sentences");
                        for (int j = 0; j < sentences.size(); j++) {
                            JsonObject sentence = sentences.get(j).getAsJsonObject();
                            if (sentence.has("text")) {
                                textBuilder.append(sentence.get("text").getAsString()).append(" ");
                            }
                        }
                    }
                }
                String trim = textBuilder.toString().trim();
                return trim;
            }
            return "";
        }
    }
}
