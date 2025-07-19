package com.example.interview.repository;

import com.example.interview.model.AiResponse;
import com.example.interview.model.InterviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiResponseRepository extends JpaRepository<AiResponse, Long> {
    
    // 根据面试记录ID查询所有AI回复
    List<AiResponse> findByInterviewRecord(InterviewRecord interviewRecord);
    
    // 根据面试记录ID查询所有AI回复
    @Query("SELECT ar FROM AiResponse ar WHERE ar.interviewRecord.id = :recordId")
    List<AiResponse> findByInterviewRecordId(@Param("recordId") Long recordId);
    
    // 根据用户ID查询所有AI回复
    @Query("SELECT ar FROM AiResponse ar WHERE ar.interviewRecord.user.id = :userId")
    List<AiResponse> findByUserId(@Param("userId") Long userId);
    
    // 统计某个面试记录的AI回复数量
    @Query("SELECT COUNT(ar) FROM AiResponse ar WHERE ar.interviewRecord.id = :recordId")
    Long countByInterviewRecordId(@Param("recordId") Long recordId);
} 