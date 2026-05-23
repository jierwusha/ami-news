package com.project.aminewsbackend.service;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.*;
import com.project.aminewsbackend.entity.Item;
import com.project.aminewsbackend.entity.NewsVector;
import com.project.aminewsbackend.mapper.ItemMapper;

@Service
public class NewsVectorService {
    
    @Autowired
    private TextEmbeddingService embeddingService;
    
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;
    
    @Autowired
    private ItemMapper itemMapper;
    
    @Autowired
    private ElasticsearchIndexService elasticsearchIndexService;
    
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"), // 带毫秒
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"), // 不带秒
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"), // 空格分隔，不带秒
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };
    
    /**
     * 统一初始化服务
     */
    @PostConstruct
    public void init() {
        try {
            System.out.println("[NewsVectorService]=== 向量服务统一初始化 ===");
            
            // 1. 检查并创建ES索引
            if (!checkAndCreateIndex()) {
                System.err.println("[NewsVectorService] ❌索引初始化失败");
                return;
            }
            
            // 2. 检查ES连接健康状态
            if (!checkElasticsearchHealth()) {
                System.err.println("[NewsVectorService]❌ Elasticsearch连接检查失败");
                return;
            }
            
            // 3. 清理无效向量数据
            cleanupInvalidVectors();

            // 4. 清理零向量数据
            cleanupZeroVectors();
            
            // 5. 检查现有向量数据
            long vectorCount = checkExistingVectors();
            
            // 6. 如果数据不足，触发初始化
            if (vectorCount < 10) {
                System.out.println("[NewsVectorService] ⚠️  向量数据不足，触发初始向量化");
                triggerInitialVectorization();
            } else {
                System.out.println("[NewsVectorService] ✅ 向量数据充足，服务就绪");
            }
            
        } catch (Exception e) {
            System.err.println("[NewsVectorService]❌ 向量服务初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 检查并创建索引
     */
    private boolean checkAndCreateIndex() {
        try {
            // 优先使用专门的索引服务
            if (elasticsearchIndexService != null) {
                if (!elasticsearchIndexService.checkIndexHealth()) {
                    return elasticsearchIndexService.createNewsVectorIndex();
                }
                return true;
            }
            
            // 降级到基础实现
            IndexOperations indexOps = elasticsearchOperations.indexOps(NewsVector.class);
            
            if (!indexOps.exists()) {
                System.out.println("[NewsVectorService]🔧 索引不存在，创建 news_vectors 索引...");
                indexOps.create();
                Document mapping = indexOps.createMapping();
                indexOps.putMapping(mapping);
                System.out.println("[NewsVectorService]✅ news_vectors 索引创建成功");
            } else {
                System.out.println("[NewsVectorService]✅ news_vectors 索引已存在");
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("[NewsVectorService]❌ 索引检查/创建失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查Elasticsearch健康状态
     */
    private boolean checkElasticsearchHealth() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(NewsVector.class);
            
            if (!indexOps.exists()) {
                System.err.println("[NewsVectorService]❌ news_vectors索引不存在");
                return false;
            }
            
            // 执行简单查询测试连接
            elasticsearchOperations.count(Query.findAll(), NewsVector.class);
            System.out.println("[NewsVectorService]✅ Elasticsearch连接和索引正常");
            return true;
            
        } catch (Exception e) {
            System.err.println("[NewsVectorService]❌ Elasticsearch健康检查失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查现有向量数据
     */
    public long checkExistingVectors() {
        try {
            long totalCount = elasticsearchOperations.count(Query.findAll(), NewsVector.class);
            System.out.println("[NewsVectorService]📊 Elasticsearch中向量数据总数: " + totalCount);

            return totalCount;
            
        } catch (Exception e) {
            System.err.println("[NewsVectorService]❌ 查询向量数据失败: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 从Elasticsearch获取已向量化的itemId集合
     */
    private Set<Long> getVectorizedItemIds() {
        try {
            // 查询所有已向量化的itemId
            Query query = Query.findAll();
            SearchHits<NewsVector> searchHits = elasticsearchOperations.search(query, NewsVector.class);
            
            return searchHits.getSearchHits().stream()
                .map(hit -> hit.getContent().getItemId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
                
        } catch (Exception e) {
            System.err.println("[NewsVectorService]❌ 获取已向量化itemId失败: " + e.getMessage());
            return new HashSet<>();
        }
    }

    /**
     * 获取未向量化的新闻
     */
    public List<Item> getUnvectorizedItems(int limit, int maxDays) {
        try {
            // 1. 从ES获取已向量化的itemId
            Set<Long> vectorizedItemIds = getVectorizedItemIds();
            System.out.println("[NewsVectorService]已向量化的新闻数量: " + vectorizedItemIds.size());

            // 2. 从数据库获取最近的新闻
            List<Item> recentItems = itemMapper.selectRecentItems(maxDays, limit); // 多取一些，过滤后可能不够
            
            // 3. 过滤出未向量化的新闻
            List<Item> unvectorizedItems = recentItems.stream()
                .filter(item -> item.getId() != null && !vectorizedItemIds.contains(item.getId().longValue()))
                .limit(limit)
                .collect(Collectors.toList());

            System.out.println("[NewsVectorService]找到未向量化的新闻: " + unvectorizedItems.size() + " 条");
            return unvectorizedItems;
            
        } catch (Exception e) {
            System.err.println("[NewsVectorService]获取未向量化新闻失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 检查单个新闻是否已向量化
     */
    public boolean isItemVectorized(Long itemId) {
        try {
            Criteria criteria = new Criteria("itemId").is(itemId);
            Query query = new CriteriaQuery(criteria);
            
            long count = elasticsearchOperations.count(query, NewsVector.class);
            return count > 0;
            
        } catch (Exception e) {
            System.err.println("[NewsVectorService]检查新闻向量化状态失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 触发初始向量化任务（异步）
     */
    private void triggerInitialVectorization() {
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("[NewsVectorService]🔄 开始异步向量化任务...");
                
                // 获取未向量化的新闻数据
                List<Item> unvectorizedItems = getUnvectorizedItems(3, 7);
                System.out.println("[NewsVectorService]📰 找到未向量化新闻: " + unvectorizedItems.size() + " 条");
                
                if (!unvectorizedItems.isEmpty()) {
                    batchProcessNewsVectors(unvectorizedItems);
                    System.out.println("[NewsVectorService]✅ 初始向量化任务完成");
                } else {
                    System.out.println("[NewsVectorService]⚠️  没有找到需要处理的新闻数据");
                }
                
            } catch (Exception e) {
                System.err.println("[NewsVectorService]🔥 异步向量化任务失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    // ========== 向量化处理方法 ==========
    
    /**
     * 手动触发向量化处理
     */
    // public void manualProcessVectors(int limit) {
    //     try {
    //         List<Item> items = itemMapper.selectUnvectorizedItems(limit);
            
    //         if (items.isEmpty()) {
    //             System.out.println("没有找到需要处理的新闻");
    //             return;
    //         }
            
    //         System.out.println("🚀 开始处理 " + items.size() + " 条新闻的向量化");
    //         batchProcessNewsVectors(items);
            
    //         // 处理完成后再次检查
    //         checkExistingVectors();
            
    //     } catch (Exception e) {
    //         System.err.println("手动向量化处理失败: " + e.getMessage());
    //         e.printStackTrace();
    //     }
    // }
    
    /**
     * 为新闻生成并存储向量（带去重检查）
     */
    public void processNewsVector(Item item) {
        try {
            // 先检查是否已经向量化
            if (item.getId() != null && isItemVectorized(item.getId().longValue())) {
                System.out.println("[NewsVectorService]⏭️  新闻已向量化，跳过: " + item.getTitle());
                return;
            }
            
            // // 检测 API 连接
            // if (!embeddingService.testConnection()) {
            //     System.err.println("❌ TextEmbeddingService API 连接失败，跳过此新闻");
            //     return;
            // }
            
            // 验证标题有效性
            if (item.getTitle() == null || item.getTitle().trim().isEmpty()) {
                System.err.println("[NewsVectorService]❌ 跳过处理：标题为空，itemId: " + item.getId());
                return;
            }
            
            // 生成标题向量
            float[] titleVector = embeddingService.getEmbedding(item.getTitle());
            
            // 处理内容，如果内容为空使用标题
            String content = extractContent(item.getDescription());
            if (content == null || content.trim().isEmpty()) {
                content = item.getTitle(); // 使用标题作为内容
            }
            
            // 生成内容向量
            float[] contentVector = embeddingService.getEmbedding(content);

            // 验证向量有效性
            if (!isValidVector(titleVector) || !isValidVector(contentVector)) {
                System.err.println("[NewsVectorService]❌ 跳过写入：向量生成失败或无效，itemId: " + item.getId());
                return;
            }

            NewsVector newsVector = new NewsVector();
            newsVector.setItemId(item.getId() != null ? item.getId().longValue() : null);
            newsVector.setTitle(item.getTitle());
            newsVector.setContent(content);
            newsVector.setTitleVector(titleVector);
            newsVector.setContentVector(contentVector);
            newsVector.setChannelId(item.getChannelId() != null ? item.getChannelId().longValue() : null);

            // 修复日期解析逻辑
            LocalDateTime pubDate = parsePubDate(item.getPubDate());
            newsVector.setPubDate(pubDate);
            
            // 存储到Elasticsearch
            NewsVector savedVector = elasticsearchOperations.save(newsVector);

            System.out.println("[NewsVectorService]✅ 新闻向量存储成功: " + item.getTitle());

        } catch (Exception e) {
            System.err.println("[NewsVectorService]❌ 处理新闻向量失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 提取和清理内容
     */
    private String extractContent(String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 使用Jsoup清理HTML
            String content = Jsoup.parse(description).text();
            
            // 进一步清理
            content = content.trim()
                    .replaceAll("\\s+", " ")
                    .replaceAll("[\\r\\n]+", " ");
            
            // 如果内容太短，认为无效
            if (content.length() < 10) {
                return null;
            }
            
            // 限制内容长度避免超时
            if (content.length() > 1000) {
                content = content.substring(0, 1000);
            }
            
            return content;
            
        } catch (Exception e) {
            System.err.println("[NewsVectorService]内容提取失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 检测和清理零向量
     */
    public void cleanupZeroVectors() {
        try {
            System.out.println("[NewsVectorService]🧹 开始清理零向量数据...");
            
            Query query = Query.findAll();
            SearchHits<NewsVector> searchHits = elasticsearchOperations.search(query, NewsVector.class);
            
            int deletedCount = 0;
            for (var hit : searchHits.getSearchHits()) {
                NewsVector newsVector = hit.getContent();
                boolean shouldDelete = false;
                
                // 检查是否为零向量
                if (isZeroVector(newsVector.getTitleVector()) || isZeroVector(newsVector.getContentVector())) {
                    shouldDelete = true;
                }
                
                if (shouldDelete) {
                    try {
                        elasticsearchOperations.delete(newsVector);
                        deletedCount++;
                        System.out.println("[NewsVectorService]🗑️  删除零向量数据: itemId=" + newsVector.getItemId());
                    } catch (Exception e) {
                        System.err.println("[NewsVectorService]删除失败: " + e.getMessage());
                    }
                }
            }

            System.out.println("[NewsVectorService]✅ 零向量清理完成，删除了 " + deletedCount + " 条记录");

        } catch (Exception e) {
            System.err.println("[NewsVectorService]❌ 清理零向量失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 检测是否为零向量
     */
    private boolean isZeroVector(float[] vector) {
        if (vector == null || vector.length == 0) {
            return true;
        }
        
        double sum = 0;
        for (float value : vector) {
            sum += Math.abs(value);
        }
        
        return sum < 0.001; // 如果所有值的绝对值之和小于0.001，认为是零向量
    }

    /**
     * 更严格的向量有效性检查
     */
    private boolean isValidVector(float[] vector) {
        if (vector == null || vector.length != 1024) {
            return false;
        }
        
        double sum = 0;
        for (float value : vector) {
            if (Float.isNaN(value) || Float.isInfinite(value)) {
                return false;
            }
            sum += Math.abs(value);
        }
        
        // 向量不能是零向量
        return sum >= 0.001;
    }
    
    /**
     * 解析发布日期
     */
    private LocalDateTime parsePubDate(String pubDateStr) {
        if (pubDateStr == null || pubDateStr.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        
        // 清理字符串，去除可能的时区信息
        String cleanDateStr = pubDateStr.trim().split("\\+")[0].split("Z")[0];
        
        // 尝试不同的日期格式
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                // 直接尝试解析，不要用复杂的条件判断
                return LocalDateTime.parse(cleanDateStr, formatter);
            } catch (DateTimeParseException e) {
                // 如果是只有日期格式，尝试转换为LocalDateTime
                try {
                    if (formatter.toString().contains("yyyy-MM-dd") && !formatter.toString().contains("HH:mm")) {
                        LocalDate date = LocalDate.parse(cleanDateStr, formatter);
                        return date.atStartOfDay();
                    }
                } catch (DateTimeParseException ex) {
                    // 继续尝试下一个格式
                }
            }
        }
        
        // 如果所有格式都失败，返回当前时间
        System.err.println("[NewsVectorService]⚠️  无法解析日期格式: " + pubDateStr + "，使用当前时间");
        return LocalDateTime.now();
    }
    
    /**
     * 批量处理新闻向量
     */
    public void batchProcessNewsVectors(List<Item> items) {
        int processed = 0;
        int failed = 0;
        
        for (Item item : items) {
            try {
                processNewsVector(item);
                processed++;
            } catch (Exception e) {
                failed++;
                System.err.println("[NewsVectorService]处理失败: " + item.getTitle() + " - " + e.getMessage());
            }
            
            // 避免API限流
            try {
                Thread.sleep(50); //约1200 RPM
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("[NewsVectorService]📊 批量处理完成 - 成功: " + processed + ", 失败: " + failed);
    }

    /**
     * 定时处理未生成向量的新闻
     */
    //@Scheduled(fixedDelay = 300000) // 5分钟执行一次
    // public void processUnvectorizedNews() {
    //     try {
    //         // 获取未向量化的新闻
    //         List<Item> unprocessedItems = getUnvectorizedItems(20, 30);
            
    //         if (!unprocessedItems.isEmpty()) {
    //             System.out.println("🔄 定时处理未向量化的新闻: " + unprocessedItems.size() + " 条");
    //             batchProcessNewsVectors(unprocessedItems);
    //         }
            
    //     } catch (Exception e) {
    //         System.err.println("❌ 定时向量化任务失败: " + e.getMessage());
    //     }
    // }
    
    // ========== 语义检索方法 ==========
    
    /**
     * 语义检索新闻 - 使用向量搜索
     */
    public List<Item> semanticSearch(String question, List<Integer> channelIds, int topK) {
        try {
            System.out.println("=== 开始语义检索 ===");
            System.out.println("问题: " + question);
            
            // 1. 获取问题的向量表示
            System.out.println("--- 生成问题向量 ---");
            float[] questionVector = embeddingService.getEmbedding(question);
            System.out.println("问题向量维度: " + questionVector);
            
            // 2. 使用Elasticsearch的向量搜索功能
            System.out.println("--- 执行向量搜索 ---");
            List<NewsVector> candidates = vectorSearch(questionVector, channelIds, topK * 5);
            System.out.println("向量搜索结果数量: " + candidates.size());
            
            // 3. 转换为Item并设置相似度分数
            List<Item> results = candidates.stream()
                .map(newsVector -> {
                    Item item = itemMapper.selectById(newsVector.getItemId());
                    if (item != null) {
                        // 使用ES返回的相似度分数
                        item.setRelevanceScore(newsVector.getScore());
                    }
                    return item;
                })
                .filter(Objects::nonNull)
                .limit(topK)
                .collect(Collectors.toList());
            
            System.out.println("语义检索结果:");
            for (int i = 0; i < results.size(); i++) {
                Item item = results.get(i);
                System.out.println(String.format("%d. [相似度:%.3f] %s", 
                    i + 1, item.getRelevanceScore(), item.getTitle()));
            }
            
            return results;
            
        } catch (Exception e) {
            System.err.println("语义检索失败: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * 执行向量搜索
     */
    private List<NewsVector> vectorSearch(float[] queryVector, List<Integer> channelIds, int limit) {
        try {
            // 并行向量搜索
            return parallelVectorSearch(queryVector, channelIds, limit);
            
        } catch (Exception e) {
            System.err.println("向量搜索失败: " + e.getMessage());
            e.printStackTrace();
            
            // 如果向量搜索失败，降级到基于关键词的搜索
            return fallbackKeywordSearch(channelIds, limit);
        }
    }

    


    /**
     * 降级搜索方法 - 当向量搜索失败时使用
     */
    private List<NewsVector> fallbackKeywordSearch(List<Integer> channelIds, int limit) {
        try {
            System.out.println("🔄 向量搜索失败，使用降级搜索...");
            
            // 构建简单的过滤查询
            Criteria criteria = new Criteria("channelId").in(channelIds);
            Query query = new CriteriaQuery(criteria)
                .setPageable(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "pubDate")));
            
            SearchHits<NewsVector> searchHits = elasticsearchOperations.search(query, NewsVector.class);
            
            return searchHits.getSearchHits().stream()
                .map(hit -> {
                    NewsVector newsVector = hit.getContent();
                    newsVector.setScore(1.0f); // 设置默认分数
                    return newsVector;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            System.err.println("降级搜索也失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 手动触发历史数据向量化
     */
    public void processHistoricalNews(int batchSize, int maxDays) {
        try {
            System.out.println("🔄 开始处理历史新闻向量化...");
            
            List<Item> unvectorizedItems = getUnvectorizedItems(batchSize, maxDays);
            
            if (unvectorizedItems.isEmpty()) {
                System.out.println("没有找到需要处理的历史新闻");
                return;
            }
            
            System.out.println("找到 " + unvectorizedItems.size() + " 条历史新闻需要处理");
            batchProcessNewsVectors(unvectorizedItems);
            
        } catch (Exception e) {
            System.err.println("处理历史新闻向量化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 检查索引映射和数据状态
     */
    public void debugIndexStatus() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(NewsVector.class);
            
            // 检查索引是否存在
            if (!indexOps.exists()) {
                System.err.println("❌ 索引不存在");
                return;
            }
            
            // 获取映射信息
            Map<String, Object> mapping = indexOps.getMapping();
            System.out.println("📋 索引映射: " + mapping);
            
            // 检查有向量字段的文档数量
            Criteria titleVectorExists = new Criteria("titleVector").exists();
            Query titleQuery = new CriteriaQuery(titleVectorExists);
            long titleVectorCount = elasticsearchOperations.count(titleQuery, NewsVector.class);
            
            Criteria contentVectorExists = new Criteria("contentVector").exists();
            Query contentQuery = new CriteriaQuery(contentVectorExists);
            long contentVectorCount = elasticsearchOperations.count(contentQuery, NewsVector.class);
            
            System.out.println("📊 有titleVector的文档数: " + titleVectorCount);
            System.out.println("📊 有contentVector的文档数: " + contentVectorCount);
            
            // 检查指定频道的文档数量
            Criteria channelCriteria = new Criteria("channelId").in(Arrays.asList(1, 2, 3));
            Query channelQuery = new CriteriaQuery(channelCriteria);
            long channelDocCount = elasticsearchOperations.count(channelQuery, NewsVector.class);
            System.out.println("📊 指定频道的文档数: " + channelDocCount);
            
        } catch (Exception e) {
            System.err.println("❌ 调试索引状态失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 并行向量搜索 - 分别搜索标题和内容然后合并
     */
    private List<NewsVector> parallelVectorSearch(float[] queryVector, List<Integer> channelIds, int limit) {
        try {
            CompletableFuture<List<NewsVector>> titleSearchFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return titleOnlyVectorSearch(queryVector, channelIds, limit);
                } catch (Exception e) {
                    System.err.println("标题搜索失败: " + e.getMessage());
                    return new ArrayList<>();
                }
            });

            CompletableFuture<List<NewsVector>> contentSearchFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return contentOnlyVectorSearch(queryVector, channelIds, limit);
                } catch (Exception e) {
                    System.err.println("内容搜索失败: " + e.getMessage());
                    return new ArrayList<>();
                }
            });

            // 等待两个搜索完成
            List<NewsVector> titleResults;
            List<NewsVector> contentResults;
            try {
                titleResults = titleSearchFuture.get();
                contentResults = contentSearchFuture.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("并行向量搜索被中断: " + e.getMessage());
                return new ArrayList<>();
            } catch (java.util.concurrent.ExecutionException e) {
                System.err.println("并行向量搜索执行异常: " + e.getMessage());
                return new ArrayList<>();
            }

            // 打印两个结果的评分
            System.out.println("=== 标题向量搜索评分 ===");
            for (int i = 0; i < Math.min(5, titleResults.size()); i++) {
                NewsVector nv = titleResults.get(i);
                System.out.println(String.format("标题结果 %d: itemId=%s, score=%.4f", i + 1, nv.getItemId(), nv.getScore()));
            }
            System.out.println("=== 内容向量搜索评分 ===");
            for (int i = 0; i < Math.min(5, contentResults.size()); i++) {
                NewsVector nv = contentResults.get(i);
                System.out.println(String.format("内容结果 %d: itemId=%s, score=%.4f", i + 1, nv.getItemId(), nv.getScore()));
            }

            // 合并并去重
            Map<Long, NewsVector> mergedResults = new LinkedHashMap<>();

            // 添加标题搜索结果（权重更高）
            for (NewsVector result : titleResults) {
                if (result.getItemId() != null) {
                    result.setScore(result.getScore() * 1.2f); // 提高标题搜索的权重
                    mergedResults.put(result.getItemId(), result);
                }
            }

            // 添加内容搜索结果
            for (NewsVector result : contentResults) {
                if (result.getItemId() != null) {
                    NewsVector existing = mergedResults.get(result.getItemId());
                    if (existing == null) {
                        mergedResults.put(result.getItemId(), result);
                    } else {
                        // 如果已存在，取更高的分数
                        if (result.getScore() > existing.getScore()) {
                            mergedResults.put(result.getItemId(), result);
                        }
                    }
                }
            }

            return mergedResults.values().stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(limit)
                .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("并行向量搜索失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<NewsVector> titleOnlyVectorSearch(float[] queryVector, List<Integer> channelIds, int limit) {
        // 这里是之前成功的简单搜索方法
        return simpleVectorSearch(queryVector, channelIds, limit);
    }

    /**
     * 简化的向量搜索 - 只使用标题向量
     */
    private List<NewsVector> simpleVectorSearch(float[] queryVector, List<Integer> channelIds, int limit) {
        try {
            String vectorJson = IntStream.range(0, queryVector.length)
                .mapToObj(i -> Float.toString(queryVector[i]))
                .collect(Collectors.joining(",", "[", "]"));

            // 使用更简单的脚本
            String queryJson = String.format("""
                {
                  "script_score": {
                    "query": {
                      "bool": {
                        "filter": [
                          {
                            "terms": {
                              "channelId": %s
                            }
                          },
                          {
                            "exists": {
                              "field": "titleVector"
                            }
                          }
                        ]
                      }
                    },
                    "script": {
                      "source": "cosineSimilarity(params.query_vector, 'titleVector') + 1.0",
                      "params": {
                        "query_vector": %s
                      }
                    }
                  }
                }
                """,
                channelIds.toString(),
                vectorJson
            );

            System.out.println("执行标题向量查询...");
            
            StringQuery query = new StringQuery(queryJson);
            query.setPageable(PageRequest.of(0, limit));

            SearchHits<NewsVector> searchHits = elasticsearchOperations.search(query, NewsVector.class);

            List<NewsVector> results = searchHits.getSearchHits().stream()
                .map(hit -> {
                    NewsVector newsVector = hit.getContent();
                    newsVector.setScore(hit.getScore());
                    return newsVector;
                })
                .collect(Collectors.toList());
                
            System.out.println("✅ 标题向量搜索成功，返回 " + results.size() + " 条结果");
            return results;

        } catch (Exception e) {
            System.err.println("标题向量搜索失败: " + e.getMessage());
            throw e; // 重新抛出异常，让上层处理
        }
    }


   private List<NewsVector> contentOnlyVectorSearch(float[] queryVector, List<Integer> channelIds, int limit) {
        try {
            System.out.println("=== 执行安全的内容向量搜索 ===");
            
            String vectorJson = IntStream.range(0, queryVector.length)
                .mapToObj(i -> Float.toString(queryVector[i]))
                .collect(Collectors.joining(",", "[", "]"));

            // 使用与安全测试相同的脚本 - 带错误处理
            String queryJson = String.format("""
                {
                "script_score": {
                    "query": {
                    "bool": {
                        "filter": [
                        {
                            "terms": {
                            "channelId": %s
                            }
                        },
                        {
                            "exists": {
                            "field": "contentVector"
                            }
                        }
                        ]
                    }
                    },
                    "script": {
                    "source": "try { double sim = cosineSimilarity(params.query_vector, 'contentVector'); return Double.isNaN(sim) || Double.isInfinite(sim) ? 0.0 : sim + 1.0; } catch (Exception e) { return 0.0; }",
                    "params": {
                        "query_vector": %s
                    }
                    }
                }
                }
                """,
                channelIds.toString(),
                vectorJson
            );

            System.out.println("执行安全的内容向量查询...");
            
            StringQuery query = new StringQuery(queryJson);
            query.setPageable(PageRequest.of(0, limit));

            SearchHits<NewsVector> searchHits = elasticsearchOperations.search(query, NewsVector.class);

            List<NewsVector> results = searchHits.getSearchHits().stream()
                .map(hit -> {
                    NewsVector newsVector = hit.getContent();
                    newsVector.setScore(hit.getScore());
                    return newsVector;
                })
                .filter(nv -> nv.getScore() > 0) // 过滤掉分数为0的结果（错误处理返回的）
                .collect(Collectors.toList());
                
            System.out.println("✅ 安全内容向量搜索成功，返回 " + results.size() + " 条结果");
            
            // 打印前几个结果的分数
            for (int i = 0; i < Math.min(3, results.size()); i++) {
                NewsVector nv = results.get(i);
                System.out.println(String.format("内容结果 %d: itemId=%s, score=%.4f", i + 1, nv.getItemId(), nv.getScore()));
            }
            
            return results;

        } catch (Exception e) {
            System.err.println("❌ 安全内容向量搜索失败: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 诊断标题向量问题
     */
    public void diagnoseTitleVectors() {
        try {
            System.out.println("=== 标题向量诊断 ===");

            // 1. 查询前几个有titleVector的文档
            Criteria titleVectorExists = new Criteria("titleVector").exists();
            Query query = new CriteriaQuery(titleVectorExists).setPageable(PageRequest.of(0, 5));
            SearchHits<NewsVector> searchHits = elasticsearchOperations.search(query, NewsVector.class);

            System.out.println("找到有titleVector的文档: " + searchHits.getTotalHits());

            for (int i = 0; i < searchHits.getSearchHits().size(); i++) {
                NewsVector newsVector = searchHits.getSearchHits().get(i).getContent();
                System.out.println("文档 " + (i + 1) + ":");
                System.out.println("  - itemId: " + newsVector.getItemId());
                System.out.println("  - title: " + newsVector.getTitle());

                if (newsVector.getTitleVector() != null) {
                    float[] titleVector = newsVector.getTitleVector();
                    System.out.println("  - titleVector length: " + titleVector.length);

                    // 检查是否有异常值
                    boolean hasNaN = false;
                    boolean hasInfinity = false;
                    for (float value : titleVector) {
                        if (Float.isNaN(value)) hasNaN = true;
                        if (Float.isInfinite(value)) hasInfinity = true;
                    }
                    System.out.println("  - 包含NaN: " + hasNaN);
                    System.out.println("  - 包含无穷大: " + hasInfinity);
                    System.out.println("  - 前5个值: " + Arrays.toString(Arrays.copyOf(titleVector, Math.min(5, titleVector.length))));
                } else {
                    System.out.println("  - titleVector: null");
                }
            }

        } catch (Exception e) {
            System.err.println("❌ 标题向量诊断失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 清理无效的向量数据
     */
    public void cleanupInvalidVectors() {
        try {
            System.out.println("[NewsVectorService]🧹 开始清理无效向量数据...");

            // 查找没有有效向量的文档
            Query query = Query.findAll();
            SearchHits<NewsVector> searchHits = elasticsearchOperations.search(query, NewsVector.class);
            
            int deletedCount = 0;
            for (var hit : searchHits.getSearchHits()) {
                NewsVector newsVector = hit.getContent();
                boolean shouldDelete = false;
                
                // 检查标题向量
                if (newsVector.getTitleVector() == null || !isValidVector(newsVector.getTitleVector())) {
                    shouldDelete = true;
                }
                
                // 检查内容向量
                if (newsVector.getContentVector() == null || !isValidVector(newsVector.getContentVector())) {
                    shouldDelete = true;
                }
                
                // 检查标题和内容是否有效
                if (newsVector.getTitle() == null || newsVector.getTitle().trim().isEmpty()) {
                    shouldDelete = true;
                }
                
                if (shouldDelete) {
                    try {
                        elasticsearchOperations.delete(newsVector);
                        deletedCount++;
                        System.out.println("[NewsVectorService]🗑️  删除无效向量数据: itemId=" + newsVector.getItemId());
                    } catch (Exception e) {
                        System.err.println("[NewsVectorService]删除失败: " + e.getMessage());
                    }
                }
            }

            System.out.println("[NewsVectorService]✅ 清理完成，删除了 " + deletedCount + " 条无效记录");

        } catch (Exception e) {
            System.err.println("[NewsVectorService]❌ 清理无效向量数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

   
}