package com.project.aminewsbackend.service;

import com.project.aminewsbackend.entity.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

@Service
public class VectorQueueManager {
    
    @Autowired
    private NewsVectorService newsVectorService;
    
    private final BlockingQueue<Item> vectorQueue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    @PostConstruct
    public void init() {
        // 启动定时处理队列 - 每35秒处理一次
        scheduler.scheduleWithFixedDelay(this::processQueue, 10, 35, TimeUnit.SECONDS);
        System.out.println(" [VectorQueueManager]向量化队列管理器已启动");
    }
    
    /**
     * 添加单个item到向量化队列
     */
    public void addToQueue(Item item) {
        try {
            vectorQueue.offer(item);;
        } catch (Exception e) {
            System.err.println("[VectorQueueManager]添加到向量化队列失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量添加items到向量化队列
     */
    public void addToQueue(List<Item> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        
        for (Item item : items) {
            vectorQueue.offer(item);
        }
    }
    
    /**
     * 处理队列中的items
     */
    private void processQueue() {
        if (vectorQueue.isEmpty()) {
            return;
        }
        
        List<Item> batchItems = new ArrayList<>();
        int batchSize = 500; // 每批处理500个，可以根据API限制调整

        // 取出一批items
        for (int i = 0; i < batchSize && !vectorQueue.isEmpty(); i++) {
            Item item = vectorQueue.poll();
            if (item != null) {
                batchItems.add(item);
            }
        }
        
        if (!batchItems.isEmpty()) {
            System.out.println("[VectorQueueManager]处理向量化队列: " + batchItems.size() + " 条，剩余队列: " + vectorQueue.size() + " 条");
            
            // 同步处理，确保限流控制
            try {
                newsVectorService.batchProcessNewsVectors(batchItems);
            } catch (Exception e) {
                System.err.println("[VectorQueueManager]队列向量化处理失败: " + e.getMessage());
                // 可以选择将失败的items重新放回队列
                // reQueueFailedItems(batchItems);
            }
        }
    }
    
    /**
     * 重新排队失败的items
     */
    private void reQueueFailedItems(List<Item> failedItems) {
        for (Item item : failedItems) {
            vectorQueue.offer(item);
        }
        System.out.println("[VectorQueueManager] 重新排队失败的items: " + failedItems.size() + " 条");
    }
    
    /**
     * 获取队列大小
     */
    public int getQueueSize() {
        return vectorQueue.size();
    }
    
    /**
     * 清空队列
     */
    public void clearQueue() {
        vectorQueue.clear();
        System.out.println("[VectorQueueManager]队列已清空");
    }
    
    /**
     * 获取队列状态
     */
    public String getQueueStatus() {
        return String.format("队列大小: %d, 待处理items", vectorQueue.size());
    }
}