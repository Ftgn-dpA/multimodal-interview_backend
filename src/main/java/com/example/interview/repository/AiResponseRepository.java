package com.example.interview.repository;

import com.example.interview.model.AiResponse;
import com.example.interview.model.InterviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface AiResponseRepository extends JpaRepository<AiResponse, Long> {
    
    // JPA Repository 方法 - 与 AiResponseService 兼容
    List<AiResponse> findByInterviewRecord(InterviewRecord interviewRecord);
    
    @Query("SELECT ar FROM AiResponse ar WHERE ar.interviewRecord.id = :recordId")
    List<AiResponse> findByInterviewRecordId(@Param("recordId") Long recordId);
    
    @Query("SELECT ar FROM AiResponse ar WHERE ar.interviewRecord.user.id = :userId")
    List<AiResponse> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(ar) FROM AiResponse ar WHERE ar.interviewRecord.id = :recordId")
    Long countByInterviewRecordId(@Param("recordId") Long recordId);
    
    // 原生SQL查询方法 - 用于 InterviewAnalysisController
    @Query(value = "SELECT * FROM ai_responses WHERE interview_record_id = :recordId", nativeQuery = true)
    List<Map<String, Object>> findRawByInterviewRecordId(@Param("recordId") Long recordId);
    
    @Query(value = "SELECT ai_responses FROM interview_records WHERE id = :recordId", nativeQuery = true)
    String findAiResponsesByRecordId(@Param("recordId") Long recordId);
} 