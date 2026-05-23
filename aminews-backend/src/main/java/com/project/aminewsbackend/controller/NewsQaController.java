package com.project.aminewsbackend.controller;

import com.project.aminewsbackend.service.NewsQaService;
import com.project.aminewsbackend.entity.QaRecord;
import com.project.aminewsbackend.entity.Item;
import com.project.aminewsbackend.dto.NewsQaResponseDTO;
import com.project.aminewsbackend.utils.Result;
import com.project.aminewsbackend.utils.UserThreadLocal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/qa")
@CrossOrigin(originPatterns = "*", allowCredentials = "false")
public class NewsQaController {
    
    @Autowired
    private NewsQaService newsQaService;
    
    /**
     * 问答接口 - 返回详细信息包含相关新闻
     */
    @PostMapping("/ask")
    public Result askQuestion(
            @RequestParam String question,
            @RequestParam(required = false) String sessionId) {
        try {
            if (question == null || question.trim().isEmpty()) {
                return Result.error(400, "问题不能为空");
            }
            
            // 生成会话ID（如果前端没有提供）
            if (sessionId == null || sessionId.trim().isEmpty()) {
                sessionId = "session_" + System.currentTimeMillis() + "_" + 
                           java.util.UUID.randomUUID().toString().substring(0, 8);
            }
            
            // 调用问答服务 - 返回详细响应
            NewsQaResponseDTO qaResponse = newsQaService.answerQuestion(question, sessionId);
            
            // 构建响应对象
            QaResponse response = new QaResponse();
            response.setSessionId(qaResponse.getSessionId());
            response.setQuestion(question);
            response.setAnswer(qaResponse.getAnswer());
            response.setRelevantNews(qaResponse.getRelevantNews());
            response.setAvgRelevanceScore(qaResponse.getAvgRelevanceScore());
            response.setTotalNewsCount(qaResponse.getRelevantNews() != null ? qaResponse.getRelevantNews().size() : 0);
            response.setStatus(qaResponse.getStatus());
            response.setTimestamp(System.currentTimeMillis());
            
            return Result.success(response);
            
        } catch (Exception e) {
            System.err.println("问答API调用失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error(500, "问答服务暂时不可用，请稍后再试");
        }
    }
    
    /**
     * 简单问答接口 - 只返回答案文本（兼容旧版本）
     */
    @PostMapping("/ask-simple")
    public Result askQuestionSimple(
            @RequestParam String question,
            @RequestParam(required = false) String sessionId) {
        try {
            if (question == null || question.trim().isEmpty()) {
                return Result.error(400, "问题不能为空");
            }
            
            // 生成会话ID（如果前端没有提供）
            if (sessionId == null || sessionId.trim().isEmpty()) {
                sessionId = "session_" + System.currentTimeMillis() + "_" + 
                           java.util.UUID.randomUUID().toString().substring(0, 8);
            }
            
            // 调用问答服务获取详细响应
            NewsQaResponseDTO qaResponse = newsQaService.answerQuestion(question, sessionId);
            
            // 构建简单响应对象
            SimpleQaResponse response = new SimpleQaResponse();
            response.setSessionId(qaResponse.getSessionId());
            response.setQuestion(question);
            response.setAnswer(qaResponse.getAnswer());
            response.setTimestamp(System.currentTimeMillis());
            
            return Result.success(response);
            
        } catch (Exception e) {
            System.err.println("简单问答API调用失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error(500, "问答服务暂时不可用，请稍后再试");
        }
    }
    
    /**
     * 获取最近的问答记录
     */
    @GetMapping("/history/{sessionId}")
    public Result getSessionHistory(@PathVariable String sessionId) {
        try {
            List<QaRecord> records = newsQaService.getSessionHistory(sessionId);
            
            // 将QaRecord转换为包含新闻标题的响应DTO
            List<QaRecordWithNews> enrichedRecords = new ArrayList<>();
            
            for (QaRecord record : records) {
                QaRecordWithNews enrichedRecord = new QaRecordWithNews();
                enrichedRecord.setId(record.getId());
                enrichedRecord.setUserId(record.getUserId());
                enrichedRecord.setSessionId(record.getSessionId());
                enrichedRecord.setQuestion(record.getQuestion());
                enrichedRecord.setAnswer(record.getAnswer());
                enrichedRecord.setRetrievalScore(record.getRetrievalScore());
                enrichedRecord.setCreatedTime(record.getCreatedTime());
                enrichedRecord.setUpdatedTime(record.getUpdatedTime());
                
                // 解析context_items并获取新闻标题
                List<NewsItem> newsItems = parseContextItemsAndGetTitles(record.getContextItems());
                enrichedRecord.setNewsItems(newsItems);
                
                enrichedRecords.add(enrichedRecord);
            }
            
            return Result.success(enrichedRecords);
        } catch (Exception e) {
            System.err.println("获取最近记录失败: " + e.getMessage());
            return Result.error(500, "获取记录失败");
        }
    }
    
    /**
     * 解析context_items JSON并获取新闻标题
     */
    private List<NewsItem> parseContextItemsAndGetTitles(String contextItems) {
        List<NewsItem> newsItems = new ArrayList<>();
        
        if (contextItems == null || contextItems.trim().isEmpty()) {
            return newsItems;
        }
        
        try {
            // 解析JSON格式的新闻ID列表
            ObjectMapper objectMapper = new ObjectMapper();
            List<Integer> itemIds = objectMapper.readValue(contextItems, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, Integer.class));
            
            // 批量查询新闻标题
            if (!itemIds.isEmpty()) {
                List<Item> items = newsQaService.getItemsByIds(itemIds);
                
                for (Item item : items) {
                    NewsItem newsItem = new NewsItem();
                    newsItem.setId(item.getId());
                    newsItem.setTitle(item.getTitle());
                    newsItem.setLink(item.getLink());
                    newsItem.setPubDate(item.getPubDate());
                    newsItems.add(newsItem);
                }
            }
            
        } catch (Exception e) {
            System.err.println("解析context_items失败: " + e.getMessage());
            // 如果解析失败，尝试简单的逗号分割方式
            try {
                String[] idStrings = contextItems.split(",");
                List<Integer> itemIds = new ArrayList<>();
                for (String idStr : idStrings) {
                    try {
                        itemIds.add(Integer.parseInt(idStr.trim()));
                    } catch (NumberFormatException ignored) {
                        // 忽略无效的ID
                    }
                }
                
                if (!itemIds.isEmpty()) {
                    List<Item> items = newsQaService.getItemsByIds(itemIds);
                    for (Item item : items) {
                        NewsItem newsItem = new NewsItem();
                        newsItem.setId(item.getId());
                        newsItem.setTitle(item.getTitle());
                        newsItem.setLink(item.getLink());
                        newsItem.setPubDate(item.getPubDate());
                        newsItems.add(newsItem);
                    }
                }
            } catch (Exception ex) {
                System.err.println("备用解析方式也失败: " + ex.getMessage());
            }
        }
        
        return newsItems;
    }
    
    
    
    /**
     * 获取用户所有问答记录
     */
    @GetMapping("/user/records")
    public Result getUserQaRecords(@RequestParam(defaultValue = "50") int limit) {
        try {
            List<QaRecord> records = newsQaService.getUserQaRecords(limit);
            return Result.success(records);
        } catch (Exception e) {
            System.err.println("获取用户问答记录失败: " + e.getMessage());
            return Result.error(500, "获取记录失败");
        }
    }
    
    /**
     * 获取用户会话列表
     */
    @GetMapping("/user/sessions")
    public Result getUserSessions() {
        try {
            // 获取用户最近的问答记录，按会话分组
            List<QaRecord> records = newsQaService.getUserQaRecords(100);
            
            // 按会话ID分组并生成会话信息
            List<SessionInfo> sessions = records.stream()
                    .collect(java.util.stream.Collectors.groupingBy(QaRecord::getSessionId))
                    .entrySet().stream()
                    .map(entry -> {
                        String sessionId = entry.getKey();
                        List<QaRecord> sessionRecords = entry.getValue();
                        
                        SessionInfo sessionInfo = new SessionInfo();
                        sessionInfo.setSessionId(sessionId);
                        sessionInfo.setQuestionCount(sessionRecords.size());
                        
                        // 获取最新的问题作为会话标题
                        sessionRecords.sort((a, b) -> b.getCreatedTime().compareTo(a.getCreatedTime()));
                        QaRecord latestRecord = sessionRecords.get(0);
                        
                        String title = latestRecord.getQuestion();
                        if (title.length() > 30) {
                            title = title.substring(0, 30) + "...";
                        }
                        sessionInfo.setTitle(title);
                        sessionInfo.setLastQuestionTime(latestRecord.getCreatedTime());
                        
                        return sessionInfo;
                    })
                    .sorted((a, b) -> b.getLastQuestionTime().compareTo(a.getLastQuestionTime()))
                    .collect(java.util.stream.Collectors.toList());
            
            return Result.success(sessions);
            
        } catch (Exception e) {
            System.err.println("获取用户会话列表失败: " + e.getMessage());
            return Result.error(500, "获取会话列表失败");
        }
    }

    
    /**
     * 删除会话及其所有问答记录
     */
    @PostMapping("/session/delete/{sessionId}")
    public Result deleteSession(@PathVariable String sessionId) {
        try {
            if (sessionId == null || sessionId.trim().isEmpty()) {
                return Result.error(400, "会话ID不能为空");
            }
            
            // 验证会话是否属于当前用户
            Integer currentUserId = UserThreadLocal.getUser().getId();
            List<QaRecord> sessionRecords = newsQaService.getSessionHistory(sessionId);
            
            if (sessionRecords.isEmpty()) {
                return Result.error(404, "会话不存在");
            }
            
            // 检查会话是否属于当前用户
            boolean belongsToUser = sessionRecords.stream()
                    .allMatch(record -> record.getUserId().equals(currentUserId));
            
            if (!belongsToUser) {
                return Result.error(403, "无权限删除此会话");
            }
            
            // 删除会话下的所有问答记录
            int deletedCount = newsQaService.deleteSession(sessionId, currentUserId);
            
            if (deletedCount > 0) {
                System.out.println("[NewsQaController] 成功删除会话: " + sessionId + 
                                ", 删除记录数: " + deletedCount);
                return Result.success("会话删除成功，共删除 " + deletedCount + " 条记录");
            } else {
                return Result.error(500, "删除会话失败");
            }
            
        } catch (Exception e) {
            System.err.println("删除会话失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error(500, "删除会话失败: " + e.getMessage());
        }
    }


    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Result healthCheck() {
        try {
            HealthInfo healthInfo = new HealthInfo();
            healthInfo.setStatus("healthy");
            healthInfo.setMessage("问答服务运行正常");
            healthInfo.setTimestamp(System.currentTimeMillis());
            healthInfo.setVersion("1.0.0");
            
            return Result.success(healthInfo);
            
        } catch (Exception e) {
            System.err.println("健康检查失败: " + e.getMessage());
            return Result.error(500, "问答服务异常");
        }
    }
    
    /**
     * 问答请求DTO
     */
    public static class QaRequest {
        private String question;
        private String sessionId;
        
        // Getters and Setters
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }
    
    /**
     * 详细问答响应DTO - 包含相关新闻
     */
    public static class QaResponse {
        private String sessionId;
        private String question;
        private String answer;
        private List<Item> relevantNews;
        private double avgRelevanceScore;
        private int totalNewsCount;
        private String status;
        private long timestamp;
        
        // Getters and Setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        
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
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * 简单问答响应DTO - 仅包含答案文本
     */
    public static class SimpleQaResponse {
        private String sessionId;
        private String question;
        private String answer;
        private long timestamp;
        
        // Getters and Setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * 会话信息DTO
     */
    public static class SessionInfo {
        private String sessionId;
        private String title;
        private int questionCount;
        private java.time.LocalDateTime lastQuestionTime;
        
        // Getters and Setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public int getQuestionCount() { return questionCount; }
        public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
        
        public java.time.LocalDateTime getLastQuestionTime() { return lastQuestionTime; }
        public void setLastQuestionTime(java.time.LocalDateTime lastQuestionTime) { this.lastQuestionTime = lastQuestionTime; }
    }
    
    /**
     * 健康信息DTO
     */
    public static class HealthInfo {
        private String status;
        private String message;
        private long timestamp;
        private String version;
        
        // Getters and Setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }

    /**
     * 问答记录响应DTO - 包含相关新闻信息
     */
    public static class QaRecordWithNews {
        private Long id;
        private Integer userId;
        private String sessionId;
        private String question;
        private String answer;
        private Double retrievalScore;
        private java.time.LocalDateTime createdTime;
        private java.time.LocalDateTime updatedTime;
        private List<NewsItem> newsItems;
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        
        public Double getRetrievalScore() { return retrievalScore; }
        public void setRetrievalScore(Double retrievalScore) { this.retrievalScore = retrievalScore; }
        
        public java.time.LocalDateTime getCreatedTime() { return createdTime; }
        public void setCreatedTime(java.time.LocalDateTime createdTime) { this.createdTime = createdTime; }
        
        public java.time.LocalDateTime getUpdatedTime() { return updatedTime; }
        public void setUpdatedTime(java.time.LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
        
        public List<NewsItem> getNewsItems() { return newsItems; }
        public void setNewsItems(List<NewsItem> newsItems) { this.newsItems = newsItems; }
    }
    
    /**
     * 新闻条目信息DTO
     */
    public static class NewsItem {
        private Integer id;
        private String title;
        private String link;
        private String pubDate;
        
        // Getters and Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getLink() { return link; }
        public void setLink(String link) { this.link = link; }
        
        public String getPubDate() { return pubDate; }
        public void setPubDate(String pubDate) { this.pubDate = pubDate; }
    }
}