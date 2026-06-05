package com.cxy.aienglishspeakingpracticecompanion.untils;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.*;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class OssOperation {

    // 从 application.yml 中读取配置
    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Value("${aliyun.oss.region}")
    private String region; // 例如: cn-hangzhou 或 cn-beijing

    /**
     * 核心业务方法：接收前端录音文件，上传到自己的 OSS，并返回百炼 ASR 认识的 HTTPS 临时链接
     */
    public String uploadMultipartFile(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new RuntimeException("前端上传的音频文件为空");
        }

        // 1. 严格对齐官方示例：配置 V4 签名
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);

        // 2. 严格对齐官方示例：配置凭证提供者（这里为了方便 Spring 管理，使用静态凭证包装）
        CredentialsProvider credentialsProvider = new DefaultCredentialProvider(accessKeyId, accessKeySecret);

        // 3. 构建安全的实例
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .build();

        File localTempFile = null;

        try {
            // 4. 动态捕获文件名后缀（防止挂羊头卖狗肉导致阿里模型无法识别）
            String originalFilename = multipartFile.getOriginalFilename();
            String suffix = (originalFilename != null && originalFilename.contains(".")) ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : ".wav";

            // 5. 将 MultipartFile 二进制数据在后端服务器的临时目录“落地”为物理 File 对象
            String systemTempDir = System.getProperty("java.io.tmpdir");
            localTempFile = new File(systemTempDir, "voice_" + UUID.randomUUID() + suffix);
            multipartFile.transferTo(localTempFile);

            // 6. 生成云端对象完整路径（例如：english_voices/202606/unique_id.m4a）
            String objectName = "english_voices/" + UUID.randomUUID() + suffix;
            log.info("本地物理文件已就绪，正在通过自建 V4 签名协议上传至 OSS... 路径: {}", objectName);

            // 7. 创建 PutObjectRequest 对象并上传
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, localTempFile);
            ossClient.putObject(putObjectRequest);
            log.info("🔥 OSS 物理块上传大获成功！");

            // 8. 🔓【核心破局】：为非实时文件转写模型生成带有 V4 签名加密的 HTTPS 临时下载 URL
            // 设置 10 分钟过期，足够百炼模型顺着链接把音频抓取过去
            Date expiration = new Date(System.currentTimeMillis() + 10 * 60 * 1000);
            URL signedUrl = ossClient.generatePresignedUrl(bucketName, objectName, expiration);

            String finalHttpsUrl = signedUrl.toString();
            log.info("成功为 Qwen ASR 生成合法的 HTTPS 网络外链 URL: {}", finalHttpsUrl);
            return finalHttpsUrl;

        } catch (OSSException oe) {
            log.error("OSS 服务端拒绝了请求! 错误码: {}, 原因: {}", oe.getErrorCode(), oe.getErrorMessage());
            throw new RuntimeException("OSS Server Error: " + oe.getErrorMessage());
        } catch (ClientException ce) {
            log.error("本地客户端网络异常或配置错误! 原因: {}", ce.getMessage());
            throw new RuntimeException("OSS Client Error: " + ce.getMessage());
        } catch (Exception e) {
            log.error("文件落地或处理链路阻断", e);
            throw new RuntimeException("System IO Error: " + e.getMessage());
        } finally {
            // 9. 🧹 善后清理：关闭客户端并立刻粉碎服务器本地临时文件，绝不白白占用系统空间
            if (ossClient != null) {
                ossClient.shutdown();
            }
            if (localTempFile != null && localTempFile.exists()) {
                boolean deleted = localTempFile.delete();
                log.info("服务器本地临时音频清理状态: {}", deleted);
            }
        }
    }
    /**
     * 重载方法：直接接收后端的本地 File 对象并用 V4 签名上传
     */
    public String uploadMultipartFile(File localFile) {
        if (localFile == null || !localFile.exists()) {
            throw new RuntimeException("本地音频物理文件不存在");
        }

        // 1. 初始化官方 V4 签名配置（和你刚才复制的Demo完全一样）
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        CredentialsProvider credentialsProvider = new DefaultCredentialProvider(accessKeyId, accessKeySecret);

        OSS ossClient = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .build();

        try {
            // 2. 生成云端路径
            String originalFilename = localFile.getName();
            String suffix = (originalFilename.contains(".")) ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".wav";
            String objectName = "english_voices/" + UUID.randomUUID() + suffix;

            log.info("正在通过自建 V4 签名协议将 File 上传至 OSS... 路径: {}", objectName);

            // 3. 严格遵循官方示例构建 Request 并上传
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, localFile);
            ossClient.putObject(putObjectRequest);

            // 4. 生成 10分钟 加密临时网络链接
            Date expiration = new Date(System.currentTimeMillis() + 10 * 60 * 1000);
            URL signedUrl = ossClient.generatePresignedUrl(bucketName, objectName, expiration);

            return signedUrl.toString();

        } catch (Exception e) {
            log.error("OSS 交互失败", e);
            throw new RuntimeException("OSS Error: " + e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}