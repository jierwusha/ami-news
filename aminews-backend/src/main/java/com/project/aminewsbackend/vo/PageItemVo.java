package com.project.aminewsbackend.vo;

import lombok.Data;

import java.util.List;

@Data
public class PageItemVo {
    List<ItemVo> items;
    Integer pageNum;
    Integer pageSize;
    boolean hasNext;
}
