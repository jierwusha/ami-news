package com.project.aminewsbackend.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.annotation.Transient;

@Document(indexName = "news_vectors")
public class NewsVector {
    
    @Id
    private String id;
    
    @Field(type = FieldType.Long)
    private Long itemId;
    
    @Field(type = FieldType.Text)
    private String title;
    
    @Field(type = FieldType.Text)
    private String content;
    
    @Field(type = FieldType.Dense_Vector, dims = 1024)
    private float[] titleVector;
    
    @Field(type = FieldType.Dense_Vector, dims = 1024)
    private float[] contentVector;
    
    @Field(type = FieldType.Long)
    private Long channelId;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime pubDate;
    
    @Transient
    private double score; // ES搜索分数
    
    // 构造函数
    public NewsVector() {}
    
    public NewsVector(Long itemId, String title, String content, 
                     LocalDateTime pubDate, Long channelId) {
        this.itemId = itemId;
        this.title = title;
        this.content = content;
        this.pubDate = pubDate;
        this.channelId = channelId;
    }
    
    // Getter和Setter方法
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public float[] getTitleVector() { return titleVector; }
    public void setTitleVector(float[] titleVector) { this.titleVector = titleVector; }
    
    public float[] getContentVector() { return contentVector; }
    public void setContentVector(float[] contentVector) { this.contentVector = contentVector; }
    
    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }
    
    public LocalDateTime getPubDate() { return pubDate; }
    public void setPubDate(LocalDateTime pubDate) { this.pubDate = pubDate; }
    
    public double getScore() {
        return score;
    }
    
    public void setScore(double score) {
        this.score = score;
    }
}