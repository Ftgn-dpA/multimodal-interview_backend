package com.example.interview.model;

public class InterviewAnalysis {
    private String resumetext;
    private String modelQuestion;
    private String speechText;
    private String visionAnalysis;
    private Integer kg;   // 专业知识
    private Integer sl;   // 技能匹配
    private Integer ep;   // 语言表达
    private Integer lo;   // 逻辑思维
    private Integer in;   // 创新能力
    private Integer st;   // 应变抗压
    private String modelAdvice;
    private String question;
    private String modelPath; // 学习资源

    // Getter & Setter
    public String getResumetext() { return resumetext; }
    public void setResumetext(String resumetext) { this.resumetext = resumetext; }
    public String getSpeechText() { return speechText; }
    public void setSpeechText(String speechText) { this.speechText = speechText; }
    public String getVisionAnalysis() { return visionAnalysis; }
    public void setVisionAnalysis(String visionAnalysis) { this.visionAnalysis = visionAnalysis; }
    public Integer getKg() { return kg; }
    public void setKg(Integer kg) { this.kg = kg; }
    public Integer getSl() { return sl; }
    public void setSl(Integer sl) { this.sl = sl; }
    public Integer getEp() { return ep; }
    public void setEp(Integer ep) { this.ep = ep; }
    public Integer getLo() { return lo; }
    public void setLo(Integer lo) { this.lo = lo; }
    public Integer getIn() { return in; }
    public void setIn(Integer in) { this.in = in; }
    public Integer getSt() { return st; }
    public void setSt(Integer st) { this.st = st; }
    public String getModelAdvice() { return modelAdvice; }
    public void setModelAdvice(String modelAdvice) { this.modelAdvice = modelAdvice; }
    public String getModelQuestion() { return modelQuestion; }
    public void setModelQuestion(String modelQuestion) { this.modelQuestion = modelQuestion; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getModelPath() { return modelPath; }
    public void setModelPath(String modelPath) { this.modelPath = modelPath; }
}
