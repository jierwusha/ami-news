package com.project.aminewsbackend.controller;

import com.project.aminewsbackend.utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hello")
public class TestController {

    @GetMapping
    public Result hello() {
        return Result.success("Hello, World!");
    }
}
