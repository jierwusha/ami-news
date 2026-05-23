package com.project.aminewsbackend.controller;

import com.project.aminewsbackend.dto.RssDTO;
import com.project.aminewsbackend.service.HotWordService;
import com.project.aminewsbackend.service.RssService;
import com.project.aminewsbackend.utils.Result;
import com.project.aminewsbackend.utils.UserThreadLocal;
import com.rometools.rome.feed.synd.SyndEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rss")
public class RssController {
    @Autowired
    private RssService rssService;
    @Autowired
    private HotWordService hotWordService;

    @PostMapping("/add")
    public Result addRss(@RequestBody RssDTO rssDTO) throws Exception {
        return rssService.addRss(rssDTO.getUrl());
    }

    @PostMapping("/subscribe/url")
    public Result subscribeRss(@RequestBody RssDTO rssDTO) throws Exception {
        Result result =  rssService.subscribeRss(rssDTO.getUrl());
        hotWordService.refreshHotWordsForUser(UserThreadLocal.getUser().getId());
        return result;
    }

    @PostMapping("/subscribe/channel/{channelId}")
    public Result subscribeChannel(@PathVariable("channelId") String channelId) throws Exception {
        Result result = rssService.subscribeChannel(channelId);
        hotWordService.refreshHotWordsForUser(UserThreadLocal.getUser().getId());
        return result;
    }

    @PostMapping("/unsubscribe")
    public Result unsubscribeRss(@RequestBody RssDTO rssDTO) throws Exception {
        Result result =  rssService.unsubscribeRss(rssDTO.getUrl());
        hotWordService.refreshHotWordsForUser(UserThreadLocal.getUser().getId());
        return result;
    }

    @PostMapping("/unsubscribe/channel/{channelId}")
    public Result unsubscribeChannel(@PathVariable("channelId") String channelId) throws Exception {
        Result result = rssService.unsubscribeChannel(channelId);
        hotWordService.refreshHotWordsForUser(UserThreadLocal.getUser().getId());
        return result;
    }
}
