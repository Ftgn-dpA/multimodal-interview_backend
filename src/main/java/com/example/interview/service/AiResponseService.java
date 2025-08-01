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
    
    // 新增：面试人原始回答缓存
    private final java.util.Map<String, java.util.List<String>> userAnswerCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, java.util.List<String>> userFragmentCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 保存AI回复记录
     */
    public AiResponse saveAiResponse(Long interviewRecordId, String aiResponse) {
        // 静默保存，不输出详细信息
        
        try {
            Optional<InterviewRecord> recordOpt = interviewRecordRepository.findById(interviewRecordId);
            if (recordOpt.isPresent()) {
                InterviewRecord record = recordOpt.get();
                
                AiResponse aiResponseEntity = new AiResponse(
                    record, 
                    aiResponse
                );
                
                AiResponse savedResponse = aiResponseRepository.save(aiResponseEntity);
                return savedResponse;
            } else {
                throw new RuntimeException("面试记录不存在: " + interviewRecordId);
            }
        } catch (Exception e) {
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
        try {
            // 先查找面试记录
            Optional<InterviewRecord> recordOpt = interviewRecordRepository.findById(interviewRecordId);
            if (!recordOpt.isPresent()) {
                throw new RuntimeException("面试记录不存在: " + interviewRecordId);
            }
            
            InterviewRecord record = recordOpt.get();
            
            // 创建AiResponse对象并关联面试记录
            AiResponse aiResponseEntity = new AiResponse();
            aiResponseEntity.setInterviewRecord(record);
            aiResponseEntity.setAiResponse(aiResponse);
            
            AiResponse savedResponse = aiResponseRepository.save(aiResponseEntity);
            return savedResponse;
            
        } catch (Exception e) {
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
            // 静默处理缓存错误
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
            // 静默处理缓存错误
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
            throw e;
        }
    }

    /**
     * 缓存面试人原始回答片段
     */
    public void cacheUserAnswerFragment(String sessionId, String requestId, String userAnswerFragment, int status) {
        try {
            if (status == 2) {
                userAnswerCache.computeIfAbsent(sessionId, k -> new java.util.ArrayList<>()).add(userAnswerFragment);
            }
        } catch (Exception e) {
            // 静默处理缓存错误
        }
    }

    /**
     * 批量保存所有面试人原始回答到数据库
     */
    public void batchSaveUserAnswers(String sessionId, Long interviewRecordId) {
        try {
            java.util.List<String> completeAnswers = userAnswerCache.get(sessionId);
            if (completeAnswers == null || completeAnswers.isEmpty()) {
                return;
            }
            java.util.Optional<com.example.interview.model.InterviewRecord> recordOpt = interviewRecordRepository.findById(interviewRecordId);
            if (!recordOpt.isPresent()) {
                throw new RuntimeException("面试记录不存在: " + interviewRecordId);
            }
            com.example.interview.model.InterviewRecord record = recordOpt.get();
            // 保存为一条AiResponse，userAnswers字段存JSON数组
            com.example.interview.model.AiResponse userAnswerEntity = new com.example.interview.model.AiResponse();
            userAnswerEntity.setInterviewRecord(record);
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String jsonArray = mapper.writeValueAsString(completeAnswers);
                userAnswerEntity.setUserAnswers(jsonArray);
            } catch (Exception e) {
                userAnswerEntity.setUserAnswers(String.join("|", completeAnswers));
            }
            aiResponseRepository.save(userAnswerEntity);
            userAnswerCache.remove(sessionId);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 聚合所有面试人原始回答文本，支持自定义分隔符
     */
    public String aggregateUserAnswersByRecordId(Long recordId, String delimiter) {
        try {
            java.util.List<com.example.interview.model.AiResponse> responses = aiResponseRepository.findByInterviewRecordId(recordId);
            java.util.List<String> allAnswers = new java.util.ArrayList<>();
            for (com.example.interview.model.AiResponse resp : responses) {
                String json = resp.getUserAnswers();
                if (json != null && !json.isEmpty()) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        java.util.List<String> arr = mapper.readValue(json, java.util.List.class);
                        allAnswers.addAll(arr);
                    } catch (Exception e) {
                        allAnswers.add(json);
                    }
                }
            }
            return String.join(delimiter, allAnswers);
        } catch (Exception e) {
            return "";
        }
    }
    // 兼容老接口，默认分隔符为\n
    public String aggregateUserAnswersByRecordId(Long recordId) {
        return aggregateUserAnswersByRecordId(recordId, "\n");
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
        // 这里可以添加过期时间逻辑，比如清理超过1小时的缓存
        // 目前简单清理所有缓存
        responseCache.clear();
    }

    // 新增：一次性批量保存AI回复和面试人原始回答，保证一场面试只插入一条记录，两个字段一一对应
    public void batchSaveAllResponses(String sessionId, Long interviewRecordId) {
        try {
            java.util.List<String> aiResponses = responseCache.getOrDefault(sessionId, new java.util.ArrayList<>());
            java.util.List<String> userAnswers = userAnswerCache.getOrDefault(sessionId, new java.util.ArrayList<>());
            // 长度对齐，短的补空串
            while (aiResponses.size() < userAnswers.size()) aiResponses.add("");
            while (userAnswers.size() < aiResponses.size()) userAnswers.add("");
            // 查找面试记录
            Optional<InterviewRecord> recordOpt = interviewRecordRepository.findById(interviewRecordId);
            if (!recordOpt.isPresent()) {
                throw new RuntimeException("面试记录不存在: " + interviewRecordId);
            }
            InterviewRecord record = recordOpt.get();
            AiResponse entity = new AiResponse();
            entity.setInterviewRecord(record);
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                entity.setAiResponse(mapper.writeValueAsString(aiResponses));
                entity.setUserAnswers(mapper.writeValueAsString(userAnswers));
            } catch (Exception e) {
                entity.setAiResponse(String.join("|", aiResponses));
                entity.setUserAnswers(String.join("|", userAnswers));
            }
            aiResponseRepository.save(entity);
            responseCache.remove(sessionId);
            userAnswerCache.remove(sessionId);
        } catch (Exception e) {
            throw e;
        }
    }
} 