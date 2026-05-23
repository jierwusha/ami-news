package com.project.aminewsbackend.controller;

import com.project.aminewsbackend.dto.UserDTO;
import com.project.aminewsbackend.entity.User;
import com.project.aminewsbackend.service.UserService;
import com.project.aminewsbackend.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/login")
    public Result login(@RequestBody UserDTO userDTO) {
        return userService.login(userDTO);
    }

    @PostMapping("/register")
    public Result register(@RequestBody UserDTO userDTO) {
        return userService.register(userDTO);
    }

    @GetMapping("/code")
    public Result getCode(@RequestParam("email") String email) {
        return userService.getCode(email);
    }

    //退出登陆
    @GetMapping("/logout")
    public Result logout(@RequestHeader("Authorization") String token) {
        return userService.logout(token);
    }

    @PostMapping("/reset-password")
    public Result resetPassword(@RequestBody UserDTO userDTO) {
        return userService.resetPassword(userDTO);
    }
}
