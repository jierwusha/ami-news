package com.project.aminewsbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.stereotype.Service;
import com.project.aminewsbackend.entity.NewsVector;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.HashMap;

@Service
public class ElasticsearchIndexService {
    
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;
    
    /**
     * 应用启动时自动创建索引
     */
    @PostConstruct
    public void initializeIndices() {
        createNewsVectorIndex();
    }
    
    /**
     * 创建news_vectors索引
     */
    public boolean createNewsVectorIndex() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(NewsVector.class);
            
            // 检查索引是否已存在
            if (indexOps.exists()) {
                System.out.println("✅ news_vectors索引已存在");
                return true;
            }
            
            System.out.println("🔨 开始创建news_vectors索引...");
            
            // 创建索引设置
            Document settings = Document.create();
            settings.put("number_of_shards", 1);
            settings.put("number_of_replicas", 0);
            
            // 创建映射
            Document mapping = createNewsVectorMapping();
            
            // 创建索引
            boolean created = indexOps.create(settings);
            if (created) {
                indexOps.putMapping(mapping);
                System.out.println("✅ news_vectors索引创建成功");
                return true;
            } else {
                System.err.println("❌ news_vectors索引创建失败");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ 创建news_vectors索引时发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 创建NewsVector的映射配置
     */
    private Document createNewsVectorMapping() {
        Document mapping = Document.create();
        Document properties = Document.create();
        
        // itemId字段
        Map<String, Object> itemIdField = new HashMap<>();
        itemIdField.put("type", "integer");
        properties.put("itemId", itemIdField);
        
        // channelId字段
        Map<String, Object> channelIdField = new HashMap<>();
        channelIdField.put("type", "integer");
        properties.put("channelId", channelIdField);
        
        // title字段
        Map<String, Object> titleField = new HashMap<>();
        titleField.put("type", "text");
        titleField.put("analyzer", "standard");
        properties.put("title", titleField);
        
        // content字段
        Map<String, Object> contentField = new HashMap<>();
        contentField.put("type", "text");
        contentField.put("analyzer", "standard");
        properties.put("content", contentField);
        
        // titleVector字段 - 1024维向量
        Map<String, Object> titleVectorField = new HashMap<>();
        titleVectorField.put("type", "dense_vector");
        titleVectorField.put("dims", 1024);
        properties.put("titleVector", titleVectorField);
        
        // contentVector字段 - 1024维向量
        Map<String, Object> contentVectorField = new HashMap<>();
        contentVectorField.put("type", "dense_vector");
        contentVectorField.put("dims", 1024);
        properties.put("contentVector", contentVectorField);
        
        // 修改pubDate字段映射，支持多种日期格式
        Document pubDateMapping = Document.create();
        pubDateMapping.put("type", "date");
        pubDateMapping.put("format", "yyyy-MM-dd'T'HH:mm:ss||yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||strict_date_optional_time||epoch_millis");
        properties.put("pubDate", pubDateMapping);
        
        // 创建映射文档
        mapping.put("properties", properties);
        return mapping;
    }
    
    /**
     * 删除索引（用于重建）
     */
    public boolean deleteNewsVectorIndex() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(NewsVector.class);
            if (indexOps.exists()) {
                boolean deleted = indexOps.delete();
                System.out.println(deleted ? "✅ 索引删除成功" : "❌ 索引删除失败");
                return deleted;
            }
            return true;
        } catch (Exception e) {
            System.err.println("❌ 删除索引时发生错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 重建索引
     */
    public boolean rebuildNewsVectorIndex() {
        System.out.println("🔄 开始重建news_vectors索引...");
        
        // 删除现有索引
        deleteNewsVectorIndex();
        
        // 等待一秒
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 重新创建索引
        return createNewsVectorIndex();
    }
    
    /**
     * 检查索引健康状态
     */
    public boolean checkIndexHealth() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(NewsVector.class);
            return indexOps.exists();
        } catch (Exception e) {
            return false;
        }
    }
}
