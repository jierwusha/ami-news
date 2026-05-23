package com.project.aminewsbackend.controller;

import com.project.aminewsbackend.dto.HotWordDTO;
import com.project.aminewsbackend.vo.ItemVo;
import com.project.aminewsbackend.service.HotWordService;
import com.project.aminewsbackend.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hotword")
public class HotWordController {

    @Autowired
    private HotWordService hotWordService;
    /**
     * 刷新当前用户的热词数据
     * @return 操作结果
     */
    @PostMapping("/refresh")
    public Result refreshCurrentUserHotWords() {
        try {
            hotWordService.refreshHotWordsForCurrentUser();
            return Result.success("热词数据刷新成功");
        } catch (Exception e) {
            return Result.error(500, "刷新热词失败: " + e.getMessage());
        }
    }


    /**
     * 获取当前用户的热词列表（首页展示用）
     * @return 热词列表
     */
    @GetMapping("/list")
    public Result getCurrentUserHotWords() {
        try {
            List<HotWordDTO> hotWords = hotWordService.getCurrentUserHotWords();
            return Result.success(hotWords);
        } catch (Exception e) {
            return Result.error(500, "获取热词列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据热词ID获取对应的新闻详情
     * @param hotWordId 热词ID
     * @return 新闻列表
     */
    @GetMapping("/items/{hotWordId}")
    public Result getItemsByHotWordId(@PathVariable Integer hotWordId) {
        try {
            List<ItemVo> items = hotWordService.getItemsByHotWordId(hotWordId);
            return Result.success(items);
        } catch (Exception e) {
            return Result.error(500, "获取热词对应新闻失败: " + e.getMessage());
        }
    }




}