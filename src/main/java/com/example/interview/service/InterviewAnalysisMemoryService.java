package com.example.interview.service;

import com.example.interview.model.InterviewAnalysis;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InterviewAnalysisMemoryService {
    private final ConcurrentHashMap<String, InterviewAnalysis> analysisMap = new ConcurrentHashMap<>();

    public void save(String username, InterviewAnalysis analysis) {
        analysisMap.put(username, analysis);
    }

    public InterviewAnalysis get(String username) {
        return analysisMap.get(username);
    }
}