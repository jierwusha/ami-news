package com.project.aminewsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName folder
 */
@TableName(value ="folder")
@Data
public class Folder {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 
     */
    private Integer userId;

    /**
     * 
     */
    private String name;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;
}