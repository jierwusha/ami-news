package com.project.aminewsbackend;

import com.project.aminewsbackend.service.RssService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
class AminewsBackendApplicationTests {

    @Autowired
    private RssService rssService;

    @Test
    void addAllRss() throws Exception {
        List<String> urls = new ArrayList<>();
        Pattern pattern = Pattern.compile("http[s]?://\\S+");
        try (BufferedReader reader = new BufferedReader(new FileReader("默认订阅.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    urls.add(matcher.group());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(int i = 0;i < urls.size(); i++) {
            String url = urls.get(i);
            System.out.println("正在添加第" + (i + 1) + "个RSS：" + url);
            try {
                rssService.addRss(url);
            } catch (Exception e) {
                System.err.println("添加RSS失败: " + url + "，错误信息：" + e.getMessage());
            }
        }
    }
}
