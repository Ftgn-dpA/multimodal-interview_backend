package com.example.interview.repository;

import com.example.interview.model.InterviewRecord;
import com.example.interview.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InterviewRecordRepository extends JpaRepository<InterviewRecord, Long> {
    List<InterviewRecord> findByUser(User user);
    List<InterviewRecord> findByUserOrderByCreatedAtDesc(User user);
} 