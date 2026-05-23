package com.project.aminewsbackend.controller;

import com.project.aminewsbackend.service.ReadLaterService;
import com.project.aminewsbackend.utils.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/readlater")
public class ReadLaterController {

    private final ReadLaterService readLaterService;

    public ReadLaterController(ReadLaterService readLaterService) {
        this.readLaterService = readLaterService;
    }

    @PostMapping("/add")
    public Result addToReadLater(@RequestParam Integer itemId) {
        return readLaterService.addToReadLater(itemId);
    }

    @PostMapping("/remove")
    public Result removeFromReadLater(@RequestParam Integer itemId) {
        return readLaterService.removeFromReadLater(itemId);
    }

    @GetMapping("/list")
    public Result listReadLaterItems(@RequestParam Integer pageNum,
                                     @RequestParam Integer pageSize) {
        return readLaterService.listReadLaterItems(pageNum, pageSize);
    }
}
