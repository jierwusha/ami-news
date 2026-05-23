package com.project.aminewsbackend.controller;

import com.project.aminewsbackend.service.AiSummaryService;
import com.project.aminewsbackend.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(originPatterns = "*", allowCredentials = "false")
public class AiSummaryController {

    @Autowired
    private AiSummaryService aiSummaryService;

    /**
     * 流式生成AI总结
     * @param itemId Item ID
     * @return SSE流
     */
    @GetMapping(value = "/summary/{itemId}",produces = "text/event-stream;charset=UTF-8")
    public SseEmitter streamAiSummary(@PathVariable Integer itemId) {
        return aiSummaryService.streamAiSummary(itemId);
    }
}