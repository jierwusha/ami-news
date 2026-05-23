package com.project.aminewsbackend.vo;

import lombok.Data;

import java.util.List;

@Data
public class SearchChannelVo {
    private Integer id;
    private String title;
    private String description;
    private String icon;
    private String url;
    private List<SearchItemVo> items;
    private boolean isSubscribed;
}
