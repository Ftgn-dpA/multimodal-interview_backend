package com.example.interview.service;

import com.example.interview.model.AiResponse;
import com.example.interview.model.InterviewRecord;
import com.example.interview.repository.AiResponseRepository;
import com.example.interview.repository.InterviewRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AiResponseService implements com.example.interview.ws.AvatarWebSocketClient.AiResponseCallback {
    
    @Autowired
    private AiResponseRepository aiResponseRepository;
    
    @Autowired
    private InterviewRecordRepository interviewRecordRepository;
    
    // 内存缓存，用于批量保存
    // 注意：只有正常结束面试才会保存到数据库，直接退出时缓存会被丢弃
    // 使用sessionId作为key，存储每轮对话的完整回复数组
    private final java.util.Map<String, java.util.List<String>> responseCache = new java.util.concurrent.ConcurrentHashMap<>();
    
    // 流式分片缓存，用于合并同一轮回复的多个片段
    // 使用sessionId_requestId作为key，存储同一轮回复的所有片段
    private final java.util.Map<String, java.util.List<String>> fragmentCache = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * 保存AI回复记录
     */
    public AiResponse saveAiResponse(Long interviewRecordId, String aiResponse) {
        System.out.println("[AiResponseService] 开始保存AI回复: recordId=" + interviewRecordId);
        
        try {
            Optional<InterviewRecord> recordOpt = interviewRecordRepository.findById(interviewRecordId);
            if (recordOpt.isPresent()) {
                InterviewRecord record = recordOpt.get();
                System.out.println("[AiResponseService] 找到面试记录: " + record.getId());
                
                AiResponse aiResponseEntity = new AiResponse(
                    record, 
                    aiResponse
                );
                
                AiResponse savedResponse = aiResponseRepository.save(aiResponseEntity);
                System.out.println("[AiResponseService] AI回复保存成功: ID=" + savedResponse.getId());
                return savedResponse;
            } else {
                System.err.println("[AiResponseService] 面试记录不存在: " + interviewRecordId);
                throw new RuntimeException("面试记录不存在: " + interviewRecordId);
            }
        } catch (Exception e) {
            System.err.println("[AiResponseService] 保存AI回复失败: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * 根据面试记录ID获取所有AI回复
     */
    public List<AiResponse> getAiResponsesByInterviewRecord(Long interviewRecordId) {
        return aiResponseRepository.findByInterviewRecordId(interviewRecordId);
    }
    
    /**
     * 根据用户ID获取所有AI回复
     */
    public List<AiResponse> getAiResponsesByUserId(Long userId) {
        return aiResponseRepository.findByUserId(userId);
    }
    
    /**
     * 获取所有AI回复
     */
    public List<AiResponse> getAllAiResponses() {
        return aiResponseRepository.findAll();
    }
    
    /**
     * 统计某个面试记录的AI回复数量
     */
    public Long countAiResponsesByInterviewRecord(Long interviewRecordId) {
        return aiResponseRepository.countByInterviewRecordId(interviewRecordId);
    }
    
    /**
     * 删除某个面试记录的所有AI回复
     */
    public void deleteAiResponsesByInterviewRecord(Long interviewRecordId) {
        List<AiResponse> responses = aiResponseRepository.findByInterviewRecordId(interviewRecordId);
        aiResponseRepository.deleteAll(responses);
    }

    /**
     * 简单测试保存方法 - 使用指定的面试记录ID
     */
    public AiResponse testSaveAiResponse(Long interviewRecordId, String aiResponse) {
        System.out.println("[AiResponseService] 开始测试保存AI回复: recordId=" + interviewRecordId);
        
        try {
            // 先查找面试记录
            Optional<InterviewRecord> recordOpt = interviewRecordRepository.findById(interviewRecordId);
            if (!recordOpt.isPresent()) {
                System.err.println("[AiResponseService] 面试记录不存在: " + interviewRecordId);
                throw new RuntimeException("面试记录不存在: " + interviewRecordId);
            }
            
            InterviewRecord record = recordOpt.get();
            System.out.println("[AiResponseService] 找到面试记录: " + record.getId());
            
            // 创建AiResponse对象并关联面试记录
            AiResponse aiResponseEntity = new AiResponse();
            aiResponseEntity.setInterviewRecord(record);
            aiResponseEntity.setAiResponse(aiResponse);
            
            System.out.println("[AiResponseService] 准备保存: " + aiResponseEntity);
            
            AiResponse savedResponse = aiResponseRepository.save(aiResponseEntity);
            System.out.println("[AiResponseService] 测试保存成功: ID=" + savedResponse.getId());
            return savedResponse;
            
        } catch (Exception e) {
            System.err.println("[AiResponseService] 测试保存失败: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 缓存AI回复片段（流式分片合并）
     * 注意：只有正常结束面试时才会保存到数据库，直接退出时缓存会被丢弃
     */
    public void cacheAiResponseFragment(String sessionId, String requestId, String aiResponseFragment, int status) {
        try {
            String fragmentKey = sessionId + "_" + requestId;
            
            // 添加片段到分片缓存
            fragmentCache.computeIfAbsent(fragmentKey, k -> new java.util.ArrayList<>()).add(aiResponseFragment);
            
            // 如果status=2，说明这一轮回复结束，合并所有片段
            if (status == 2) {
                java.util.List<String> fragments = fragmentCache.get(fragmentKey);
                if (fragments != null && !fragments.isEmpty()) {
                    // 合并所有片段为完整回复
                    String completeResponse = String.join("", fragments);
                    
                    // 添加到完整回复缓存
                    responseCache.computeIfAbsent(sessionId, k -> new java.util.ArrayList<>()).add(completeResponse);
                    
                    // 清除分片缓存
                    fragmentCache.remove(fragmentKey);
                }
            }
        } catch (Exception e) {
            System.err.println("[AiResponseService] 缓存AI回复片段失败: " + e.getMessage());
        }
    }
    
    /**
     * 兼容旧接口，直接缓存完整回复（用于非流式场景）
     */
    public void cacheAiResponse(String sessionId, String aiResponseFragment) {
        try {
            // 直接添加到完整回复缓存
            responseCache.computeIfAbsent(sessionId, k -> new java.util.ArrayList<>()).add(aiResponseFragment);
        } catch (Exception e) {
            System.err.println("[AiResponseService] 缓存AI回复失败: " + e.getMessage());
        }
    }
    
    /**
     * 实现AiResponseCallback接口
     */
    @Override
    public void onAiResponse(String sessionId, String aiResponse) {
        // 兼容旧的回调方式，直接缓存完整回复
        cacheAiResponse(sessionId, aiResponse);
    }

    /**
     * 批量保存指定sessionId的所有缓存回复到指定面试记录
     * 注意：只有正常结束面试时才会调用此方法，直接退出时不会保存
     */
    public void batchSaveAiResponses(String sessionId, Long interviewRecordId) {
        try {
            java.util.List<String> completeResponses = responseCache.get(sessionId);
            if (completeResponses == null || completeResponses.isEmpty()) {
                return;
            }
            
            // 先查找面试记录
            Optional<InterviewRecord> recordOpt = interviewRecordRepository.findById(interviewRecordId);
            if (!recordOpt.isPresent()) {
                throw new RuntimeException("面试记录不存在: " + interviewRecordId);
            }
            
            InterviewRecord record = recordOpt.get();
            
            // 创建AiResponse对象，将完整回复数组存储为JSON数组
            AiResponse aiResponseEntity = new AiResponse();
            aiResponseEntity.setInterviewRecord(record);
            
            // 将List转换为JSON数组字符串
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String jsonArray = mapper.writeValueAsString(completeResponses);
                aiResponseEntity.setAiResponse(jsonArray);
            } catch (Exception e) {
                // 如果JSON转换失败，回退到分隔符方式
                aiResponseEntity.setAiResponse(String.join("|", completeResponses));
            }
            
            // 保存到数据库
            aiResponseRepository.save(aiResponseEntity);
            
            // 清除缓存
            responseCache.remove(sessionId);
            
        } catch (Exception e) {
            System.err.println("[AiResponseService] 批量保存失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 获取指定sessionId的缓存片段数量
     */
    public int getCachedResponseCount(String sessionId) {
        java.util.List<String> fragments = responseCache.get(sessionId);
        return fragments != null ? fragments.size() : 0;
    }

    /**
     * 清除指定sessionId的缓存
     */
    public void clearCache(String sessionId) {
        responseCache.remove(sessionId);
        System.out.println("[AiResponseService] 已清除sessionId " + sessionId + " 的缓存");
    }

    /**
     * 获取所有sessionId的缓存状态
     */
    public Map<String, Integer> getAllCacheStatus() {
        Map<String, Integer> status = new java.util.HashMap<>();
        for (Map.Entry<String, java.util.List<String>> entry : responseCache.entrySet()) {
            status.put(entry.getKey(), entry.getValue().size());
        }
        return status;
    }

    /**
     * 清理过期的缓存（防止内存泄漏）
     * 可以定期调用此方法清理长时间未活动的缓存
     */
    public void cleanupExpiredCache() {
        int beforeSize = responseCache.size();
        // 这里可以添加过期时间逻辑，比如清理超过1小时的缓存
        // 目前简单清理所有缓存
        responseCache.clear();
        System.out.println("[AiResponseService] 清理过期缓存，清理前: " + beforeSize + " 个面试记录");
    }
} 