package com.project.aminewsbackend.controller;

import com.project.aminewsbackend.service.ChannelService;
import com.project.aminewsbackend.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/channel")
public class ChannelController {

    @Autowired
    private ChannelService channelService;

    @GetMapping("/items")
    public Result getItemsByChannelId(@RequestParam("channelId") String channelId, @RequestParam("pageNum") Integer pageNum, @RequestParam("pageSize") Integer pageSize) {
        System.out.println("请求的channelId为：" + channelId + ", pageNum: " + pageNum + ", pageSize: " + pageSize);
        return channelService.getItemsByChannelId(channelId, pageNum, pageSize);
    }

    @GetMapping("/random")
    public Result getRandomChannel(@RequestParam("num") Integer num) {
        return channelService.getRandomChannel(num);
    }

    @GetMapping("/search")
    public Result searchChannels(@RequestParam("keyword") String keyword) throws Exception {
        return channelService.searchChannels(keyword);
    }
}
