package com.project.aminewsbackend.controller;

import com.project.aminewsbackend.service.ItemService;
import com.project.aminewsbackend.utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/item")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping("/{itemId}")
    public Result getItemById(@PathVariable Integer itemId) {
        return itemService.getItemById(itemId);
    }
}
