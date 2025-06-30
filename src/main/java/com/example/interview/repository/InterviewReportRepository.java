package com.example.interview.repository;

import com.example.interview.model.InterviewReport;
import com.example.interview.model.InterviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InterviewReportRepository extends JpaRepository<InterviewReport, Long> {
    Optional<InterviewReport> findByInterviewRecord(InterviewRecord interviewRecord);
    void deleteByInterviewRecord(InterviewRecord interviewRecord);
} 