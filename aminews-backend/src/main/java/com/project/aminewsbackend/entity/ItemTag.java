package com.project.aminewsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName item_tag
 */
@TableName(value ="item_tag")
@Data
public class ItemTag {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 
     */
    private Integer itemId;

    /**
     * 
     */
    private Integer tagId;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;
}