package com.project.aminewsbackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChannelDTO {
    private Integer id;
    private String name;
    private String description;
    private String icon;
    private String link;
    List<ItemDTO> top5news;
}
