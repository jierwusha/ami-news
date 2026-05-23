package com.project.aminewsbackend.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

@SpringBootTest
class HotWordServiceTest {
    @Autowired
    private HotWordService hotWordService;

    @Test
    void testGetTopHotwordsWithItemsByUser_emptySubscribe_real() {
        Integer userId = 1;
        Map<String, List<Long>> result = hotWordService.getTopHotwordsWithItemsByUser(userId);
        System.out.println("emptySubscribe result (real): " + result);
    }

    @Test
    void testRefreshHotWordsForUser_real() {
        Integer userId = 1;
        hotWordService.refreshHotWordsForUser(userId);
        System.out.println("refreshHotWordsForUser executed for userId=" + userId);
    }
}
