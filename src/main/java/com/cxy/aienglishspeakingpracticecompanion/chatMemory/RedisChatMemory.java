package com.cxy.aienglishspeakingpracticecompanion.chatMemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis持久化的对话记忆
 */
@Slf4j
@Component
public class RedisChatMemory implements ChatMemory {

    private static final String keyPrefix ="speak:chat:memory:";

    @Value("${chat.memory.redis.ttl:2592000}")
    private Long ttl;

    @Value("${chat.memory.redis.max-history:100}")
    private int maxHistory;

    @Autowired
    private RedisTemplate<String, byte[]> redisTemplate;
    private static final Kryo kryo = new Kryo();

    static {
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    // ✅ 使用RedisTemplate，不需要Jedis
//    public RedisChatMemory(RedisTemplate<String, byte[]> redisTemplate) {
//        this.redisTemplate = redisTemplate;
//    }

    /***
     * 添加对话
     * @param conversationId 会话ID
     * @param messages 对话消息
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        // Codex 修改：打印记忆写入入口，确认 MessageChatMemoryAdvisor 是否真的调用了 RedisChatMemory。
        log.info("RedisChatMemory.add called, conversationId={}, incomingMessages={}", conversationId, messages.size());
        List<Message> conversationMessages = getOrCreateConversation(conversationId);
        conversationMessages.addAll(messages);


        // 限制对话历史长度
        if (conversationMessages.size() > maxHistory) {
            conversationMessages = conversationMessages.stream()
                    .skip(conversationMessages.size() - maxHistory)
                    .toList();
        }

        saveConversation(conversationId, conversationMessages);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        // Codex 修改：打印记忆读取入口，确认本轮对话是否从 Redis 拉取历史消息。
        log.info("RedisChatMemory.get called, conversationId={}, lastN={}", conversationId, lastN);
        List<Message> allMessages = getOrCreateConversation(conversationId);
        return allMessages.stream()
                .skip(Math.max(0, allMessages.size() - lastN))
                .toList();
    }

    @Override
    public void clear(String conversationId) {
        String key = getRedisKey(conversationId);
        redisTemplate.delete(key);
    }

    @SuppressWarnings("unchecked")
    private List<Message> getOrCreateConversation(String conversationId) {
        String key = getRedisKey(conversationId);
        byte[] data = redisTemplate.opsForValue().get(key);

        List<Message> messages = new ArrayList<>();
        if (data != null && data.length > 0) {
            try (Input input = new Input(data)) {
                messages = kryo.readObject(input, ArrayList.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            redisTemplate.expire(key, ttl, TimeUnit.SECONDS);
        }
        return messages;
    }

    private void saveConversation(String conversationId, List<Message> messages) {
        String key = getRedisKey(conversationId);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             Output output = new Output(baos)) {
            kryo.writeObject(output, messages);
            output.flush();
            byte[] data = baos.toByteArray();
            redisTemplate.opsForValue().set(key, data, ttl, TimeUnit.SECONDS);
            // Codex 修改：打印最终 Redis key 和写入结果，方便和 Redis 客户端里查询的 key 对齐。
            log.info("RedisChatMemory saved, key={}, totalMessages={}, bytes={}, ttlSeconds={}", key, messages.size(), data.length, ttl);
        } catch (Exception e) {
            // Codex 修改：不要吞掉 Redis 会话保存异常，否则 AI 成功但 Redis 没记录时很难排查。
            log.error("Redis 会话保存失败，key={}", key, e);
            throw new RuntimeException("Redis 会话保存失败，key=" + key, e);
        }
    }

    private String getRedisKey(String conversationId) {
        return keyPrefix + conversationId;
    }

    public List<Message> getFullHistory(String conversationId) {
        return getOrCreateConversation(conversationId);
    }

    public void updateTTL(String conversationId) {
        String key = getRedisKey(conversationId);
        redisTemplate.expire(key, ttl, TimeUnit.SECONDS);
    }

    public MemoryStats getStats(String conversationId) {
        List<Message> history = getFullHistory(conversationId);
        String key = getRedisKey(conversationId);
        Long remainingTTL = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        return new MemoryStats(
                conversationId,
                history.size(),
                ttl,
                remainingTTL != null ? remainingTTL : -1
        );
    }

    public static class MemoryStats {
        private final String conversationId;
        private final int messageCount;
        private final Long ttl;
        private final Long remainingTTL;

        public MemoryStats(String conversationId, int messageCount, Long ttl, Long remainingTTL) {
            this.conversationId = conversationId;
            this.messageCount = messageCount;
            this.ttl = ttl;
            this.remainingTTL = remainingTTL;
        }

        public String getConversationId() { return conversationId; }
        public int getMessageCount() { return messageCount; }
        public Long getTtl() { return ttl; }
        public Long getRemainingTTL() { return remainingTTL; }

        @Override
        public String toString() {
            return String.format("MemoryStats{conversationId='%s', messageCount=%d, ttl=%d, remainingTTL=%d}",
                    conversationId, messageCount, ttl, remainingTTL);
        }
    }
}
