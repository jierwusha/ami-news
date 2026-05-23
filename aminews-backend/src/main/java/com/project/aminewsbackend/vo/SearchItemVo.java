package com.project.aminewsbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SearchItemVo {
    private Integer id;
    private String title;
    private String description;
    private String pubDate;
}
