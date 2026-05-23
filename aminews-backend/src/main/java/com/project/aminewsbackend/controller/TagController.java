package com.project.aminewsbackend.controller;

import com.project.aminewsbackend.service.TagService;
import com.project.aminewsbackend.utils.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tag")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @PostMapping("/create")
    public Result createTag(@RequestParam String name) {
        return tagService.createTag(name);
    }

    @PostMapping("/add")
    public Result addTagToItem(@RequestParam Integer itemId, @RequestParam Integer tagId) {
        return tagService.addTagToItem(itemId, tagId);
    }

    @PostMapping("/remove")
    public Result removeTagFromItem(@RequestParam Integer itemId, @RequestParam Integer tagId) {
        return tagService.removeTagFromItem(itemId, tagId);
    }

    @PostMapping("/delete")
    public Result deleteTag(@RequestParam Integer tagId) {
        return tagService.deleteTag(tagId);
    }

    @GetMapping("/list")
    public Result listTags() {
        // TODO 要返回标签下的文章数量
        return tagService.listTags();
    }

    @GetMapping("/items")
    public Result getItemsByTagId(@RequestParam Integer tagId){
        return tagService.getItemsByTagId(tagId);
    }
}
