package com.project.aminewsbackend.dto;

import com.project.aminewsbackend.entity.Item;
import java.util.List;

public class NewsQaResponseDTO {
    
    /**
     * AI生成的答案
     */
    private String answer;
    
    /**
     * 相关新闻列表
     */
    private List<Item> relevantNews;
    
    /**
     * 平均相关性评分
     */
    private double avgRelevanceScore;
    
    /**
     * 检索到的总新闻数
     */
    private int totalNewsCount;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 问题处理状态
     */
    private String status;
    
    /**
     * 处理时间戳
     */
    private long timestamp;
    
    // 构造函数
    public NewsQaResponseDTO() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public NewsQaResponseDTO(String answer, List<Item> relevantNews) {
        this();
        this.answer = answer;
        this.relevantNews = relevantNews;
        this.totalNewsCount = relevantNews != null ? relevantNews.size() : 0;
    }
    
    // Getter和Setter方法
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    
    public List<Item> getRelevantNews() { return relevantNews; }
    public void setRelevantNews(List<Item> relevantNews) { 
        this.relevantNews = relevantNews; 
        this.totalNewsCount = relevantNews != null ? relevantNews.size() : 0;
    }
    
    public double getAvgRelevanceScore() { return avgRelevanceScore; }
    public void setAvgRelevanceScore(double avgRelevanceScore) { this.avgRelevanceScore = avgRelevanceScore; }
    
    public int getTotalNewsCount() { return totalNewsCount; }
    public void setTotalNewsCount(int totalNewsCount) { this.totalNewsCount = totalNewsCount; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}