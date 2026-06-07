package com.cxy.aienglishspeakingpracticecompanion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.ConversationSummaryResponse;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.VoiceChatResponse;
import com.cxy.aienglishspeakingpracticecompanion.model.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface SpeakService {
    /***
     * 语音对话
     * @param file 用户的录音文件
     * @param sessionId 会话ID
     * @return
     */
    VoiceChatResponse speakWithAI(MultipartFile file, String sessionId,String scene,String customScenario );

    ConversationSummaryResponse ConversationSummary(String sessionId);

    /**
    * @author chenx
    * @description 针对表【user(用户信息表)】的数据库操作Service
    * @createDate 2026-06-06 19:43:15
    */

}
