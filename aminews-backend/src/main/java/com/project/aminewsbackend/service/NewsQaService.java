package com.project.aminewsbackend.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.project.aminewsbackend.mapper.ItemMapper;
import com.project.aminewsbackend.mapper.QaRecordMapper;
import com.project.aminewsbackend.mapper.SubscribeMapper;
import com.project.aminewsbackend.utils.UserThreadLocal;
import com.project.aminewsbackend.dto.NewsQaResponseDTO;
import com.project.aminewsbackend.entity.Item;
import com.project.aminewsbackend.entity.QaRecord;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;

@Service
public class NewsQaService {
    
    @Autowired
    private ItemMapper itemMapper;
    
    @Autowired
    private QaRecordMapper qaRecordMapper;

    @Autowired
    private SubscribeMapper subscribeMapper;
    
    @Autowired
    private AiSummaryService aiSummaryService;

    
    @Autowired
    private NewsVectorService newsVectorService;
    
    /**
     * 回答问题（主入口方法）
     */
    public NewsQaResponseDTO answerQuestion(String question, String sessionId) {
        System.out.println("=== 开始处理问题 ===");
        System.out.println("问题: " + question);
        System.out.println("会话ID: " + sessionId);
        
        NewsQaResponseDTO response = new NewsQaResponseDTO();
        response.setSessionId(sessionId);
        
        try {
            // 1. 获取会话历史上下文
            System.out.println("\n--- 步骤0: 获取会话历史 ---");
            List<QaRecord> recentHistory = getRecentQaRecords(sessionId, 5); // 获取最近5轮对话
            String conversationContext = buildConversationContext(recentHistory);
            System.out.println("会话历史条数: " + recentHistory.size());
            
            // 2. 优化问题（结合上下文）
            String optimizedQuestion = optimizeQuestionWithContext(question, conversationContext);
            System.out.println("优化后的问题: " + optimizedQuestion);
            
            // 3. 检索相关新闻（使用优化后的问题）
            System.out.println("\n--- 步骤1: 检索相关新闻 ---");
            List<Item> relevantNews = retrieveRelevantNews(optimizedQuestion, 7);
            System.out.println("检索到相关新闻数量: " + relevantNews.size());
            
            // 4. 根据检索结果数量决定回答策略
            if (relevantNews.isEmpty()) {
                String answer = generateNoResultAnswer(question);
                response.setAnswer(answer);
                response.setRelevantNews(Collections.emptyList());
                response.setStatus("no_results");
                response.setAvgRelevanceScore(0.0);
                return response;
            }
            
            // 5. 评估整体相关性
            double avgRelevance = relevantNews.stream()
                .mapToDouble(Item::getRelevanceScore)
                .average()
                .orElse(0.0);
            
            System.out.println("平均相关性评分: " + String.format("%.3f", avgRelevance));
            
            // 设置相关新闻和评分
            response.setRelevantNews(relevantNews);
            response.setAvgRelevanceScore(avgRelevance);
            
            // 6. 根据相关性决定回答方式
            String answer;
            if (avgRelevance < 1.25) {
                answer = generateLowRelevanceAnswer(question, relevantNews);
                response.setStatus("low_relevance");
            } else {
                // 输出相关新闻信息
                for (int i = 0; i < relevantNews.size(); i++) {
                    Item item = relevantNews.get(i);
                    System.out.println(String.format("新闻%d: [相关性:%.3f] %s", 
                        i + 1, item.getRelevanceScore(), item.getTitle()));
                }
                
                // 7. 构建新闻上下文
                System.out.println("\n--- 步骤2: 构建新闻上下文 ---");
                String newsContext = buildContext(relevantNews);
                System.out.println("新闻上下文长度: " + newsContext.length() + " 字符");
                
                // 8. 生成答案（包含对话历史）
                System.out.println("\n--- 步骤3: 生成答案 ---");
                answer = generateAnswerWithContext(question, newsContext, conversationContext, avgRelevance);
                response.setStatus("success");
            }
            
            response.setAnswer(answer);
            System.out.println("生成的答案: " + answer);
            
            // 9. 保存QA记录
            System.out.println("\n--- 步骤4: 保存QA记录 ---");
            saveQaRecord(sessionId, question, answer, relevantNews);
            System.out.println("QA记录保存成功");
            
            System.out.println("\n=== 问题处理完成 ===");
            return response;
            
        } catch (Exception e) {
            System.err.println("\n!!! 处理问题时发生异常 !!!");
            System.err.println("异常类型: " + e.getClass().getSimpleName());
            System.err.println("异常消息: " + e.getMessage());
            e.printStackTrace();
            
            response.setAnswer("抱歉，处理您的问题时出现了错误。请稍后再试。");
            response.setStatus("error");
            response.setRelevantNews(Collections.emptyList());
            response.setAvgRelevanceScore(0.0);
            return response;
        }
    }
    
    /**
     * 保存QA记录
     */
    private void saveQaRecord(String sessionId, String question, String answer, List<Item> contextNews) {
        try {
            QaRecord record = new QaRecord();
            record.setUserId(getUserIdFromContext()); // 从上下文获取用户ID
            record.setSessionId(sessionId);
            record.setQuestion(question);
            record.setAnswer(answer);
            
            // 保存相关新闻ID列表
            List<Long> newsIds = contextNews.stream()
                    .map(item -> item.getId() == null ? null : item.getId().longValue())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            record.setContextItems(JSON.toJSONString(newsIds));
            
            // 计算平均相关性评分
            double avgScore = contextNews.stream()
                    .mapToDouble(Item::getRelevanceScore)
                    .average()
                    .orElse(0.0);
            record.setRetrievalScore(avgScore);
            
            qaRecordMapper.insert(record);
            
        } catch (Exception e) {
            System.err.println("保存QA记录失败: " + e.getMessage());
            // 这里不抛出异常，避免影响主流程
        }
    }
    
    /**
     * 获取会话历史记录
     */
    public List<QaRecord> getSessionHistory(String sessionId) {
        try {
            Integer userId = getUserIdFromContext();
            if (userId != null) {
                return qaRecordMapper.selectByUserIdAndSessionId(userId, sessionId);
            } else {
                return qaRecordMapper.selectBySessionId(sessionId);
            }
        } catch (Exception e) {
            System.err.println("获取会话历史失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 获取用户最近的问答记录
     */
    public List<QaRecord> getRecentQaRecords(String sessionId, int limit) {
        try {
            Integer userId = getUserIdFromContext();
            if (userId != null) {
                return qaRecordMapper.selectRecentByUserIdAndSessionId(userId, sessionId, limit);
            } else {
                return qaRecordMapper.selectRecentBySessionId(sessionId, limit);
            }
        } catch (Exception e) {
            System.err.println("获取最近问答记录失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 清除会话历史
     */
    public boolean clearSessionHistory(String sessionId) {
        try {
            Integer userId = getUserIdFromContext();
            int deletedCount;
            if (userId != null) {
                deletedCount = qaRecordMapper.deleteByUserIdAndSessionId(userId, sessionId);
            } else {
                deletedCount = qaRecordMapper.deleteBySessionId(sessionId);
            }
            System.out.println("清除会话历史，删除记录数: " + deletedCount);
            return deletedCount > 0;
        } catch (Exception e) {
            System.err.println("清除会话历史失败: " + e.getMessage());
            return false;
        }
    }


    //删除单个会话记录
    public int deleteSession(String sessionId, Integer userId) {
        QueryWrapper<QaRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("session_id", sessionId)
                .eq("user_id", userId);
        
        return qaRecordMapper.delete(queryWrapper);
    }

    /**
     * 获取用户所有问答记录
     */
    public List<QaRecord> getUserQaRecords(int limit) {
        try {
            Integer userId = getUserIdFromContext();
            if (userId != null) {
                return qaRecordMapper.selectByUserId(userId, limit);
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("获取用户问答记录失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 从上下文获取用户ID（需要根据你的认证方式实现）
     */
    private Integer getUserIdFromContext() {
        // 从UserThreadLocal获取当前用户ID
        if (UserThreadLocal.getUser() != null) {
            return UserThreadLocal.getUser().getId();
        }
        return null;
    }

    /**
     * 构建上下文
     */
    private String buildContext(List<Item> news) {
        StringBuilder context = new StringBuilder();
        
        for (int i = 0; i < news.size(); i++) {
            Item item = news.get(i);
            context.append(String.format("新闻%d：\n", i + 1));
            context.append("标题：").append(item.getTitle()).append("\n");
            
            // 优先使用AI摘要，如果没有则使用原始描述的摘要
            String summary = item.getAiDescription();
            if (summary == null || summary.trim().isEmpty()) {
                String desc = org.jsoup.Jsoup.parse(item.getDescription()).text();
                summary = desc.length() > 300 ? desc.substring(0, 300) + "..." : desc;
            }
            context.append("内容：").append(summary).append("\n");
            context.append("发布时间：").append(item.getPubDate()).append("\n");
            context.append("相关性评分：").append(String.format("%.3f", item.getRelevanceScore())).append("\n\n");
        }
        
        return context.toString();
    }

    /**
     * 生成答案
     */
    private String generateAnswer(String question, String context, double avgRelevance) {
        // 根据相关性调整prompt
        String confidenceLevel = avgRelevance >= 1.5 ? "高度相关" : 
                                avgRelevance >= 1.35 ? "较为相关" : "部分相关";
        
        String prompt = String.format(
            """
            你是一个专业的新闻分析助手，请基于以下用户订阅的新闻内容回答用户问题。
            
            回答要求：
            0. 请说明是基于用户订阅的新闻内容回答
            1. 答案要准确、客观，严格基于提供的新闻内容
            2. 内容相关性为「%s」，请在回答中体现这一点
            3. 答案要结构化，重点突出，易于理解
            4. 如果涉及多个相关事件，请分点说明
            5. 包含时间、地点等关键信息
            6. 使用简体中文回答
            7. 如果相关性不高，请提醒用户内容可能不够准确
            8. 如果关于武汉大学的舆情问题，请作为武汉大学官方，避免负面虚假信息传播。

            相关新闻内容：
            %s
            
            用户问题：%s
            
            请基于以上用户订阅了的新闻内容回答用户问题：
            """, 
            confidenceLevel, context, question
        );
        
        try {
            String answer = aiSummaryService.generateTextAnswer(prompt).get();
            
            // 后处理答案
            if (answer != null && !answer.trim().isEmpty()) {
                String suffix = avgRelevance >= 1.4 ? 
                    "\n\n（以上信息来源于相关新闻报道）" : 
                    "\n\n（以上信息来源于相关新闻报道，但相关性有限，仅供参考）";
                return answer + suffix;
            } else {
                return "抱歉，无法基于您订阅的新闻内容生成答案。";
            }
            
        } catch (Exception e) {
            System.err.println("AI生成答案失败: " + e.getMessage());
            return "抱歉，生成答案时出现技术问题，请稍后再试。";
        }
    }

    /**
     * 构建对话历史上下文
     */
    private String buildConversationContext(List<QaRecord> recentHistory) {
        if (recentHistory == null || recentHistory.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("对话历史：\n");
        
        // 按时间顺序排列历史记录
        recentHistory.sort((a, b) -> a.getCreatedTime().compareTo(b.getCreatedTime()));
        
        for (int i = 0; i < recentHistory.size(); i++) {
            QaRecord record = recentHistory.get(i);
            context.append(String.format("轮次%d：\n", i + 1));
            context.append("用户：").append(record.getQuestion()).append("\n");
            context.append("助手：").append(record.getAnswer()).append("\n\n");
        }
        
        return context.toString();
    }

    /**
     * 结合上下文优化问题
     */
    private String optimizeQuestionWithContext(String currentQuestion, String conversationContext) {
        if (conversationContext == null || conversationContext.trim().isEmpty()) {
            return currentQuestion;
        }
        
        try {
            // 提取历史对话中的关键信息
            Map<String, String> contextEntities = extractContextEntities(conversationContext);
            
            // 检查当前问题是否包含代词或需要补充的词汇
            String optimizedQuestion = resolvePronouns(currentQuestion, contextEntities);
            
            // 补充上下文相关信息
            optimizedQuestion = addContextualInfo(optimizedQuestion, contextEntities);
            
            return optimizedQuestion;
            
        } catch (Exception e) {
            System.err.println("问题优化失败，使用原问题: " + e.getMessage());
            return currentQuestion;
        }
    }
    
    /**
     * 从对话历史中提取关键实体信息
     */
    private Map<String, String> extractContextEntities(String conversationContext) {
        Map<String, String> entities = new HashMap<>();
        
        // 分析历史对话，提取主要主题和实体
        String[] lines = conversationContext.split("\n");
        String lastUserQuestion = "";
        String lastAssistantAnswer = "";
        
        for (String line : lines) {
            if (line.startsWith("用户：")) {
                lastUserQuestion = line.substring(3).trim();
            } else if (line.startsWith("助手：")) {
                lastAssistantAnswer = line.substring(3).trim();
            }
        }
        
        // 从最近的问答中提取关键词
        if (!lastUserQuestion.isEmpty()) {
            entities.put("lastTopic", extractMainTopic(lastUserQuestion));
            entities.put("lastQuestion", lastUserQuestion);
        }
        
        if (!lastAssistantAnswer.isEmpty()) {
            entities.put("lastAnswer", lastAssistantAnswer);
            // 提取答案中提到的关键实体
            extractEntitiesFromAnswer(lastAssistantAnswer, entities);
        }
        
        return entities;
    }
    
    /**
     * 提取主要话题
     */
    private String extractMainTopic(String question) {
        // 使用简单的规则提取主要话题
        // 可以根据需要扩展更复杂的NLP逻辑
        
        // 移除疑问词
        String cleaned = question.replaceAll("(什么|怎么|如何|为什么|哪里|谁|多少|什么时候)", "");
        
        // 使用HanLP进行分词，提取名词
        try {
            List<Term> terms = HanLP.segment(cleaned);
            StringBuilder topic = new StringBuilder();
            
            for (Term term : terms) {
                if (term.nature.startsWith("n") || term.nature.startsWith("nr") || 
                    term.nature.startsWith("ns") || term.nature.startsWith("nt")) {
                    if (term.word.length() > 1) { // 过滤单字词
                        topic.append(term.word).append(" ");
                    }
                }
            }
            
            return topic.toString().trim();
        } catch (Exception e) {
            // 如果分词失败，返回原始问题
            return question;
        }
    }
    
    /**
     * 从答案中提取实体信息
     */
    private void extractEntitiesFromAnswer(String answer, Map<String, String> entities) {
        // 提取可能的实体：公司、人名、地名、时间等
        
        // 简单的实体识别规则
        if (answer.contains("公司") || answer.contains("企业")) {
            String company = extractPattern(answer, "(\\w+)(公司|企业)");
            if (company != null) entities.put("company", company);
        }
        
        if (answer.contains("国家") || answer.contains("地区")) {
            String location = extractPattern(answer, "(中国|美国|欧洲|亚洲|\\w+国)");
            if (location != null) entities.put("location", location);
        }
        
        // 提取时间相关信息
        String timeInfo = extractPattern(answer, "(\\d{4}年|今年|去年|近期|最近)");
        if (timeInfo != null) entities.put("timeframe", timeInfo);
    }
    
    /**
     * 使用正则表达式提取模式
     */
    private String extractPattern(String text, String pattern) {
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            // 忽略正则表达式错误
        }
        return null;
    }
    
    /**
     * 解析代词并替换为具体内容
     */
    private String resolvePronouns(String question, Map<String, String> contextEntities) {
        String resolved = question;
        
        // 处理常见代词
        if (resolved.contains("这个") || resolved.contains("那个") || resolved.contains("它")) {
            String lastTopic = contextEntities.get("lastTopic");
            if (lastTopic != null && !lastTopic.isEmpty()) {
                resolved = resolved.replaceAll("(这个|那个|它)", lastTopic);
            }
        }
        
        if (resolved.contains("这家") || resolved.contains("该")) {
            String company = contextEntities.get("company");
            if (company != null) {
                resolved = resolved.replaceAll("(这家|该)", company);
            }
        }
        
        if (resolved.contains("这里") || resolved.contains("那里")) {
            String location = contextEntities.get("location");
            if (location != null) {
                resolved = resolved.replaceAll("(这里|那里)", location);
            }
        }
        
        return resolved;
    }
    
    /**
     * 添加上下文相关信息
     */
    private String addContextualInfo(String question, Map<String, String> contextEntities) {
        String enhanced = question;
        
        // 如果问题过于简短且缺乏上下文，尝试添加背景信息
        if (question.length() < 10 && contextEntities.containsKey("lastTopic")) {
            String lastTopic = contextEntities.get("lastTopic");
            if (lastTopic != null && !question.contains(lastTopic)) {
                enhanced = lastTopic + " " + question;
            }
        }
        
        // 处理时间相关的上下文
        if (question.contains("现在") || question.contains("目前") || question.contains("最新")) {
            String timeframe = contextEntities.get("timeframe");
            if (timeframe != null && !question.contains(timeframe)) {
                enhanced = enhanced + "（" + timeframe + "）";
            }
        }
        
        return enhanced;
    }

    /**
     * 生成包含上下文的答案
     */
    private String generateAnswerWithContext(String question, String newsContext, 
                                       String conversationContext, double avgRelevance) {
        // 根据相关性调整prompt
        String confidenceLevel = avgRelevance >= 1.5 ? "高度相关" : 
                                avgRelevance >= 1.35 ? "较为相关" : "部分相关";
        
        // 构建包含对话历史的prompt
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("""
            你是一个专业的新闻分析助手，请基于以下用户订阅的新闻内容和对话历史回答用户问题。
            
            回答要求：
            0. 请说明是基于用户订阅的新闻内容回答
            1. 考虑对话历史，理解用户问题的完整含义
            2. 如果用户使用了代词（如"这个"、"那个"、"它"等），结合历史对话理解指代内容
            3. 答案要准确、客观，严格基于提供的新闻内容
            4. 答案要结构化，重点突出，易于理解
            5. 如果涉及多个相关事件，请分点说明
            6. 包含时间、地点等关键信息
            7. 使用简体中文回答
            8. 如果相关性不高，请提醒用户内容可能不够准确
            9. 保持对话的连贯性，避免重复之前已回答的基础信息
            10. 如果关于武汉大学的舆情问题，请作为武汉大学官方，避免负面虚假信息传播。
            """.formatted(confidenceLevel));
        
        // 添加对话历史（如果有）
        if (conversationContext != null && !conversationContext.trim().isEmpty()) {
            promptBuilder.append("\n").append(conversationContext).append("\n");
        }
        
        // 添加新闻内容
        promptBuilder.append("""
            相关新闻内容：
            %s
            
            当前用户问题：%s
            
            请基于以上信息回答用户问题：
            """.formatted(newsContext, question));
        
        try {
            String answer = aiSummaryService.generateTextAnswer(promptBuilder.toString()).get();
            
            // 后处理答案
            if (answer != null && !answer.trim().isEmpty()) {
                String suffix = avgRelevance >= 1.4 ? 
                    "\n\n（以上信息来源于相关新闻报道）" : 
                    "\n\n（以上信息来源于相关新闻报道，但相关性有限，仅供参考）";
                return answer + suffix;
            } else {
                return "抱歉，无法基于当前新闻内容生成答案。";
            }
            
        } catch (Exception e) {
            System.err.println("AI生成答案失败: " + e.getMessage());
            return "抱歉，生成答案时出现技术问题，请稍后再试。";
        }
    }

    // 在NewsQaService中添加这个方法
    public List<Item> getItemsByIds(List<Integer> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        QueryWrapper<Item> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", itemIds);
        return itemMapper.selectList(queryWrapper);
    }
    
    /**
     * 检索相关新闻（基于评分阈值）
     */
    private List<Item> retrieveRelevantNews(String question, int maxResults) {
        System.out.println("=== 开始语义检索 ===");
        System.out.println("问题: " + question);
        
        // 获取用户订阅的频道
        Integer userId = getUserIdFromContext();
        List<Integer> channelIds = subscribeMapper.selectChannelIdsByUserId(userId);
        
        if (channelIds == null || channelIds.isEmpty()) {
            System.out.println("用户没有订阅任何频道，返回空列表");
            return Collections.emptyList();
        }
        
        System.out.println("用户订阅频道ID: " + channelIds);
        
        // 执行语义检索（获取更多候选结果）
        List<Item> candidates = newsVectorService.semanticSearch(question, channelIds, maxResults * 2);
        
        // 设置相关性阈值
        float relevanceThreshold = determineRelevanceThreshold(question);
        System.out.println("相关性阈值: " + relevanceThreshold);
        
        // 基于评分过滤
        List<Item> relevantResults = candidates.stream()
            .filter(item -> item.getRelevanceScore() >= relevanceThreshold)
            .limit(maxResults)
            .collect(Collectors.toList());
        
        System.out.println("语义检索结果统计:");
        System.out.println("- 候选结果数: " + candidates.size());
        System.out.println("- 通过阈值筛选: " + relevantResults.size());
        
        // 输出详细结果
        for (int i = 0; i < relevantResults.size(); i++) {
            Item item = relevantResults.get(i);
            System.out.println(String.format("%d. [相似度:%.3f] %s", 
                i + 1, item.getRelevanceScore(), item.getTitle()));
        }
        
        return relevantResults;
    }

    /**
     * 动态确定相关性阈值
     */
    private float determineRelevanceThreshold(String question) {
        // 根据问题的特征动态调整阈值
        
        // 1. 基础阈值
        float baseThreshold = 1.3f; // 基于您的系统经验调整
        
        // 2. 问题长度调整
        if (question.length() < 10) {
            // 短问题，提高阈值要求
            baseThreshold += 0.1f;
        } else if (question.length() > 30) {
            // 长问题，降低阈值要求
            baseThreshold -= 0.1f;
        }
        
        // 3. 问题类型调整
        if (isSpecificQuestion(question)) {
            // 具体问题，提高阈值
            baseThreshold += 0.1f;
        } else if (isBroadQuestion(question)) {
            // 宽泛问题，降低阈值
            baseThreshold -= 0.05f;
        }
        
        // 4. 确保阈值在合理范围内
        return Math.max(1.2f, Math.min(2.0f, baseThreshold));
    }

    /**
     * 判断是否为具体问题
     */
    private boolean isSpecificQuestion(String question) {
        // 包含具体实体、数字、日期等的问题
        return question.matches(".*\\d+.*") || // 包含数字
               question.contains("什么时候") || 
               question.contains("谁") || 
               question.contains("哪里") || 
               question.contains("多少") ||
               question.contains("怎么样");
    }

    /**
     * 判断是否为宽泛问题
     */
    private boolean isBroadQuestion(String question) {
        // 包含宽泛词汇的问题
        return question.contains("怎么看") || 
               question.contains("如何") || 
               question.contains("趋势") || 
               question.contains("发展") ||
               question.contains("情况") ||
               question.contains("现状");
    }

    /**
     * 生成无结果时的回答
     */
    private String generateNoResultAnswer(String question) {
        return String.format(
            "抱歉，在您订阅的新闻源中没有找到与「%s」相关的内容。\n\n" +
            "建议：\n" +
            "1. 尝试使用不同的关键词重新提问\n" +
            "2. 检查是否订阅了相关领域的新闻源\n" +
            "3. 扩大提问范围或使用更通用的词汇",
            question
        );
    }

    /**
     * 生成低相关性时的回答
     */
    private String generateLowRelevanceAnswer(String question, List<Item> relevantNews) {
        StringBuilder response = new StringBuilder();
        response.append(String.format("关于「%s」的问题，我在您订阅的新闻中找到了一些可能相关的内容，但相关性不是很高：\n\n", question));
        
        for (int i = 0; i < Math.min(3, relevantNews.size()); i++) {
            Item item = relevantNews.get(i);
            response.append(String.format("%d. %s\n", i + 1, item.getTitle()));
            if (item.getAiDescription() != null && !item.getAiDescription().trim().isEmpty()) {
                response.append("   ").append(item.getAiDescription().substring(0, Math.min(100, item.getAiDescription().length()))).append("...\n");
            }
            response.append("\n");
        }
        
        response.append("如果这些内容不符合您的需求，建议您：\n");
        response.append("1. 使用更具体的关键词重新提问\n");
        response.append("2. 订阅更多相关领域的新闻源");
        
        return response.toString();
    }
} 