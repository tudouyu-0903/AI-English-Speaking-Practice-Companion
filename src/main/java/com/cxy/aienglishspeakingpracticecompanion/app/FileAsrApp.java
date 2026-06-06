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
 *
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
     * 获取音频文件并转换成文本
     * @param localAudioFile 本地保存好的音频文件（从前端 MultipartFile 转化而来）
     * @return 识别出的完整英文字符串
     */
    public String transcribeLocalFile(File localAudioFile) {
        try {
            long startTime = System.currentTimeMillis();

            // 1.将录音文件存储在aliyunOSS 中
            log.info("开始调用自建 OssService 上传录音文件...");
            String absoluteHttpsUrl = ossOperation.uploadMultipartFile(localAudioFile);
            log.info("自建 OSS 加密外链生成成功！网络路径为: {}", absoluteHttpsUrl);

            // 2. 严格对齐非实时转写参数，塞入绝对可被外网下载的 https 链接
            QwenTranscriptionParam param = QwenTranscriptionParam.builder()
                    .apiKey(apiKey)
                    .model("qwen3-asr-flash-filetrans")
                    .fileUrl(absoluteHttpsUrl)
                    .parameter("enable_itn", false)
                    .parameter("enable_words", false)
                    .build();

            QwenTranscription transcription = new QwenTranscription();

            // 3. 提交异步任务
            QwenTranscriptionResult result = transcription.asyncCall(param);
            String taskId = result.getTaskId();
            log.info("成功创建非实时语音识别任务，TaskId: {}", taskId);

            // 4. 阻塞轮询，静静等待阿里云在云端把这端音频切片、识别完毕
            result = transcription.wait(QwenTranscriptionQueryParam.FromTranscriptionParam(param, taskId));
            log.info("语音转写任务处理完毕，总耗时: {}ms", (System.currentTimeMillis() - startTime));

            //5.校验任务状态
            if (result == null || !"SUCCEEDED".equalsIgnoreCase(String.valueOf(result.getTaskStatus()))) {
                throw new RuntimeException("阿里云百炼服务端判定任务失败。原始完整响应为: " + result);
            }

            // 6. 获取转写结果
            QwenTranscriptionTaskResult taskResult = result.getResult();
            if (taskResult == null || taskResult.getTranscriptionUrl() == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"任务状态为 SUCCEEDED，但未正确获得文字结果的 URL。");
            }

            // 8. 从临时结果 JSON 链接，清洗并抓取最终的英文字符串
            return fetchAndParseJsonResult(taskResult.getTranscriptionUrl());

        } catch (Exception e) {
            log.error("非实时语音识别链路发生故障", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"File ASR Failed: " + e.getMessage());
        }
    }

    /***
     *清洗并抓取最终的英文字符串
     * @param fileUrl 结果 JSON 链接
     * @return 转换后的文字
     */
    private String fetchAndParseJsonResult(String fileUrl) throws Exception {
        // 发起 HTTP GET 请求
        HttpURLConnection connection = (HttpURLConnection) new URL(fileUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            //将云端的json文件下载
            JsonObject rootJson = JsonParser.parseReader(reader).getAsJsonObject();
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
