package com.project.aminewsbackend.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 
 * @TableName hot_word
 */
@TableName(value ="hot_word")
@Data
public class HotWord {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 
     */
    private String word;

    /**
     * 
     */
    private Integer userId;

    /**
     * 热词出现次数
     */
    private Integer count;

    @TableField(value="create_time",fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     *
     */
    @TableField(value="update_time",fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}