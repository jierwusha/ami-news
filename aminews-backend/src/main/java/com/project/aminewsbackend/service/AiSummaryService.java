package com.project.aminewsbackend.service;

import com.project.aminewsbackend.entity.Item;
import com.project.aminewsbackend.configuration.OpenAiProperties;
import com.project.aminewsbackend.utils.Result;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.project.aminewsbackend.mapper.ItemMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Service
public class AiSummaryService {

    private final OpenAiProperties properties;
    private final String promptPrefix;
    private final ItemMapper itemMapper;
    private final ObjectMapper objectMapper;
    
    public AiSummaryService(OpenAiProperties properties, ItemMapper itemMapper) {
        this.properties = properties;
        this.promptPrefix = properties.getPromptPrefix();
        this.itemMapper = itemMapper;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 流式生成AI总结
     * @param itemId Item ID
     * @return SSE发射器
     */
    public SseEmitter streamAiSummary(Integer itemId) {
        SseEmitter emitter = new SseEmitter(60000L);
        
        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("start")
                    .data("开始生成AI总结...")
                    .build());
                
                if (itemId == null || itemId <= 0) {
                    emitter.send(SseEmitter.event().name("error").data("无效的Item ID"));
                    emitter.complete();
                    return;
                }

                Item item = itemMapper.selectById(itemId);
                if (item == null) {
                    emitter.send(SseEmitter.event().name("error").data("找不到Item"));
                    emitter.complete();
                    return;
                }

                String description = item.getDescription();
                if (description == null || description.trim().isEmpty()) {
                    emitter.send(SseEmitter.event().name("error").data("Item描述为空，无法生成总结"));
                    emitter.complete();
                    return;
                }

                // 如果已经有AI总结，等待1-2秒后流式传输显示
                if (item.getAiDescription() != null && !item.getAiDescription().trim().isEmpty()) {
                    try {
                        Thread.sleep(1000 + (int)(Math.random() * 1000)); 
                    } catch (InterruptedException ignored) {}
                    String existingSummary = item.getAiDescription();
                    streamExistingSummary(emitter, existingSummary);
                    return;
                }

                // 调用API生成新的总结
                streamNewSummary(emitter, item, description);

            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("系统错误: " + e.getMessage()));
                } catch (IOException ex) {
                    // ignore
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void streamExistingSummary(SseEmitter emitter, String existingSummary) {
        CompletableFuture.runAsync(() -> {
            try {
                for (int i = 0; i < existingSummary.length(); i++) {
                    String character = String.valueOf(existingSummary.charAt(i));
                    emitter.send(SseEmitter.event().name("token").data(character));
                    Thread.sleep(50);
                }
                emitter.send(SseEmitter.event().name("complete").data(existingSummary));
                emitter.complete();
            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e);
            }
        });
    }

    private void streamNewSummary(SseEmitter emitter, Item item, String description) {
        try {
            String prompt = promptPrefix + description + "\n\n请用简体中文回答，输出格式为纯文本。";
            
            // 构建请求体 - 移除多余的逗号
            String requestBody = String.format("""
                {
                    "model": "%s",
                    "messages": [
                        {
                            "role": "user",
                            "content": "%s"
                        }
                    ],
                    "stream": true,
                    "temperature": 0.3
                }
                """, properties.getModelName(), escapeJson(prompt));

            // 添加调试日志
            System.out.println("Request Body: " + requestBody);
            System.out.println("Model Name: " + properties.getModelName());
            System.out.println("Base URL: " + properties.getBaseUrl());

            // 创建HTTP连接
            URL url = new URL(properties.getBaseUrl() + "/chat/completions");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Authorization", "Bearer " + properties.getApiKey());
            connection.setRequestProperty("Accept", "text/event-stream; charset=UTF-8");
            connection.setDoOutput(true);

            // 发送请求
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    connection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(requestBody);
                writer.flush();
            }

            // 检查响应码
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            if (responseCode != 200) {
                // 读取错误响应
                String errorResponse = "";
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorBuilder = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorBuilder.append(line);
                    }
                    errorResponse = errorBuilder.toString();
                    System.err.println("API Error Response: " + errorResponse);
                }
                
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("API调用失败 (HTTP " + responseCode + "): " + errorResponse)
                    .build());
                emitter.complete();
                return;
            }

            // 读取流式响应
            StringBuilder fullSummary = new StringBuilder();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        
                        try {
                            JsonNode jsonNode = objectMapper.readTree(data);
                            JsonNode choices = jsonNode.get("choices");
                            if (choices != null && choices.isArray() && choices.size() > 0) {
                                JsonNode delta = choices.get(0).get("delta");
                                if (delta != null && delta.has("content")) {
                                    String content = delta.get("content").asText();

                                    if (content != null && 
                                        !"null".equals(content) && 
                                        !content.trim().isEmpty()) {
                                        
                                        fullSummary.append(content);
                                        
                                        emitter.send(SseEmitter.event()
                                            .name("token")
                                            .data(content)
                                            .build());
                                    } 
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Error parsing JSON: " + e.getMessage());
                            System.err.println("JSON data: " + data);
                        }
                    }
                }
            }

            // 保存到数据库并发送完成信号
            String summary = fullSummary.toString().trim();
            if (!summary.isEmpty()) {
                item.setAiDescription(summary);
                itemMapper.updateById(item);
            }

            emitter.send(SseEmitter.event()
                .name("complete")
                .data(summary)
                .build());
            emitter.complete();

        } catch (Exception e) {
            System.err.println("Exception in streamNewSummary: " + e.getMessage());
            e.printStackTrace();
            
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("AI总结生成失败: " + e.getMessage())
                    .build());
            } catch (IOException ex) {
                System.err.println("Failed to send error event: " + ex.getMessage());
            }
            emitter.completeWithError(e);
        }
    }

    private String escapeJson(String text) {
        return text.replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * 保持原有的非流式方法
     */
    public CompletableFuture<Result> getAiSummaryByItemId(Integer itemId) {
        if (itemId == null || itemId <= 0) {
            return CompletableFuture.completedFuture(Result.error(400, "无效的Item ID"));
        }

        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            return CompletableFuture.completedFuture(Result.error(404, "找不到Item"));
        }

        String description = item.getDescription();
        if (description == null || description.trim().isEmpty()) {
            return CompletableFuture.completedFuture(Result.error(400, "Item描述为空，无法生成总结"));
        }

        if (item.getAiDescription() != null && !item.getAiDescription().trim().isEmpty()) {
            return CompletableFuture.completedFuture(Result.success(item.getAiDescription()));
        }

        return CompletableFuture.completedFuture(Result.success("请使用流式接口"));
    }

    /**
     * 生成文本回答（用于问答系统）
     */
    public CompletableFuture<String> generateTextAnswer(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 构建请求体
                String requestBody = String.format("""
                    {
                        "model": "%s",
                        "messages": [
                            {
                                "role": "user",
                                "content": "%s"
                            }
                        ],
                        "stream": false,
                        "temperature": 0.3,
                        "max_tokens": 2000
                    }
                    """, properties.getModelName(), escapeJson(prompt));

                // 创建HTTP连接
                URL url = new URL(properties.getBaseUrl() + "/chat/completions");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Authorization", "Bearer " + properties.getApiKey());
                connection.setDoOutput(true);

                // 发送请求
                try (OutputStreamWriter writer = new OutputStreamWriter(
                        connection.getOutputStream(), StandardCharsets.UTF_8)) {
                    writer.write(requestBody);
                    writer.flush();
                }

                // 读取响应
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                // 解析响应
                JsonNode jsonNode = objectMapper.readTree(response.toString());
                JsonNode choices = jsonNode.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode message = choices.get(0).get("message");
                    if (message != null && message.has("content")) {
                        return message.get("content").asText().trim();
                    }
                }

                return "抱歉，无法生成回答。";

            } catch (Exception e) {
                System.err.println("生成文本回答失败: " + e.getMessage());
                e.printStackTrace();
                return "抱歉，生成回答时出现技术问题。";
            }
        });
    }
}