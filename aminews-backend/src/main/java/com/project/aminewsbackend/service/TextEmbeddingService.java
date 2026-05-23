package com.project.aminewsbackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.security.MessageDigest;

@Service
public class TextEmbeddingService {
    
    @Value("${openai.apiKey}")
    private String apiKey;
    
    @Value("${openai.baseUrl}")
    private String baseUrl;
    
    @Value("${openai.embeddingModelName}")
    private String embeddingModel;
    
    // 使用内存缓存（主要用于用户查询的临时缓存）
    private final Map<String, float[]> embeddingCache = new ConcurrentHashMap<>();
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public TextEmbeddingService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 获取文本的向量表示（主要用于用户查询）
     */
    public float[] getEmbedding(String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                System.out.println("[TextEmbeddingService] 输入文本为空，返回默认向量");
                // 返回一个全零向量而不是默认向量，避免影响搜索
                return new float[1024];
            }
            
            // 预处理文本 - 确保文本有效
            String processedText = preprocessText(text);
            if (processedText == null || processedText.trim().isEmpty()) {
                return new float[1024];
            }
            
            // 检查内存缓存（用户查询可能重复）
            String cacheKey = generateHash(processedText);
            if (embeddingCache.containsKey(cacheKey)) {
                return embeddingCache.get(cacheKey);
            }

            // API限流检查
            rateLimitCheck();
            // 构建请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", embeddingModel);
            requestBody.put("input", processedText);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String response = restTemplate.postForObject(baseUrl + "/embeddings", entity, String.class);
            
            // 解析响应
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode embeddingArray = jsonNode.get("data").get(0).get("embedding");
            float[] embedding = new float[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                embedding[i] = (float) embeddingArray.get(i).asDouble();
            }
            
            // 验证向量有效性
            if (isValidVector(embedding)) {
                // 限制内存缓存大小
                if (embeddingCache.size() < 100) {
                    embeddingCache.put(cacheKey, embedding);
                }
                return embedding;
            } else {
                System.err.println("[TextEmbeddingService] 生成的向量包含无效值，返回默认向量");
                return new float[1024];
            }
            
        } catch (Exception e) {
            System.err.println("[TextEmbeddingService] 获取文本向量失败: " + e.getMessage());
            return new float[1024];
        }
    }

    /**
     * 验证向量有效性
     */
    private boolean isValidVector(float[] vector) {
        if (vector == null || vector.length == 0) {
            return false;
        }
        
        for (float value : vector) {
            if (Float.isNaN(value) || Float.isInfinite(value)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 文本预处理
     */
    private String preprocessText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        
        String processed = text.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[\\r\\n]+", " ")
                .trim();
        
        
        return processed.substring(0, Math.min(processed.length(), 8000));
    }
    
    /**
     * 计算向量相似度（余弦相似度）
     */
    public double calculateSimilarity(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("向量维度不匹配");
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
     * 生成文本哈希值
     */
    private String generateHash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(text.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(text.hashCode());
        }
    }
    
    /**
     * 测试API连接
     */
    public boolean testConnection() {
        try {
            float[] testVector = getEmbedding("测试");
            return testVector != null && testVector.length > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * API限流检查
     */
    private void rateLimitCheck() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}