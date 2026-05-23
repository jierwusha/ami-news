package com.project.aminewsbackend.vo;
import lombok.Data;

import java.util.List;

import com.hankcs.hanlp.dependency.nnparser.util.std;

@Data
public class ItemVo {
    private Integer id;
    private String title;
    private String link;
    private String pubDate;
    private String imageUrl;
    private String description;
    private String aiDescription;
    private String channelTitle;
    private String channelIcon;
}
