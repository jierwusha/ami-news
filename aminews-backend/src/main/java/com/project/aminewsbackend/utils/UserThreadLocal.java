package com.project.aminewsbackend.utils;

import com.project.aminewsbackend.entity.User;

public class UserThreadLocal {
    private static ThreadLocal<User> userThread = new ThreadLocal<>();

    public static void set(User user) {
        userThread.set(user);
    }

    public static User getUser(){
        return userThread.get();
    }

    public static void remove() {
        userThread.remove();
    }
}