package com.project.aminewsbackend.vo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TagVo {
    private Integer id;

    private String name;

    private Integer itemCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
