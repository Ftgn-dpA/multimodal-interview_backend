package com.example.interview.service;

import com.example.interview.model.ScoreFeedback;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Random;

@Service
public class LargeModelService {
    
    // 根据面试类型生成相应的题目
    public String generateQuestion(String type) {
        // TODO: 调用大模型API，根据类型生成不同风格的题目
        switch (type) {
            case "video":
                return getVideoInterviewQuestion();
            default:
                return "";
        }
    }

    // 视频面试题目
    private String getVideoInterviewQuestion() {
        String[] questions = {
            "请简要介绍一下你自己。",
            "请谈谈你的技术栈和项目经验。",
            "你如何处理团队中的冲突？",
            "请描述一个你解决过的技术难题。",
            "你对未来的职业规划是什么？",
            "请谈谈你对敏捷开发的理解。",
            "你如何保持技术更新？",
            "请描述一个你参与过的成功项目。"
        };
        return questions[new Random().nextInt(questions.length)];
    }

    // 处理音频文件，转换为文本
    public String processAudio(MultipartFile audioFile) throws IOException {
        // TODO: 调用语音识别API将音频转换为文本
        // 这里暂时返回模拟的文本
        return "这是从音频转换的文本内容。";
    }

    // 生成AI语音回复
    public String generateAudioResponse(String question, String userAnswer) {
        // TODO: 调用大模型API生成语音回复
        return "感谢您的回答。您的表现很好，请继续下一个问题。";
    }

    // 对回答进行评分和反馈
    public ScoreFeedback scoreAnswer(String question, String answer) {
        // TODO: 调用大模型API进行智能评分
        // 这里使用简单的模拟评分逻辑
        double score = calculateScore(answer);
        String feedback = generateFeedback(score, answer);
        return new ScoreFeedback(score, feedback);
    }

    // 简单的评分算法
    private double calculateScore(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return 0.0;
        }
        
        // 基于回答长度的简单评分
        int length = answer.trim().length();
        if (length < 10) return 3.0;
        if (length < 30) return 5.0;
        if (length < 100) return 7.0;
        if (length < 200) return 8.5;
        return 9.0;
    }

    // 生成反馈
    private String generateFeedback(double score, String answer) {
        if (score < 5.0) {
            return "回答过于简短，建议提供更多详细信息。";
        } else if (score < 7.0) {
            return "回答基本完整，但可以更加详细和具体。";
        } else if (score < 8.5) {
            return "回答很好，表达清晰，内容完整。";
        } else {
            return "回答非常出色，表达清晰，逻辑性强，内容充实。";
        }
    }
} 