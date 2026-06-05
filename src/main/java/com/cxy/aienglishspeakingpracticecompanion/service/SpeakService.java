package com.cxy.aienglishspeakingpracticecompanion.service;

import org.springframework.web.multipart.MultipartFile;

public interface SpeakService {
   String speakWithAI(MultipartFile file,String sessionId);
}
