package com.project.aminewsbackend.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName channel
 */
@TableName(value ="channel")
@Data
public class Channel implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 
     */
    private String title;

    private String url;

    /**
     * 
     */
    private String link;

    /**
     * 
     */
    private String atomLink;

    /**
     * 
     */
    private String description;

    /**
     * 
     */
    private String icon;

    /**
     * 
     */
    @TableField(value="create_time",fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     *
     */
    @TableField(value="update_time",fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}