package com.project.aminewsbackend.dto;

import com.project.aminewsbackend.entity.HotWord;
import lombok.Data;

import java.util.List;

@Data
public class HotWordDTO {
    private int id;
    private String word;
    private Integer count;
}