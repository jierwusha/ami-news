package com.project.aminewsbackend.mapper;

import com.project.aminewsbackend.entity.Item;
import com.project.aminewsbackend.entity.Tag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.aminewsbackend.vo.ItemVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author ARounder
* @description 针对表【tag】的数据库操作Mapper
* @createDate 2025-06-24 16:16:02
* @Entity generator.entity.Tag
*/
public interface TagMapper extends BaseMapper<Tag> {

    @Select("SELECT i.* FROM item i " +
            "JOIN item_tag it ON i.id = it.item_id " +
            "WHERE it.tag_id = #{tagId}")
    List<Item> getItemsByTagId(@Param("tagId") Integer tagId);

    @Select("SELECT DISTINCT i.*, c.title AS channelTitle, c.icon AS channelIcon FROM item i " +
            "JOIN item_tag it ON i.id = it.item_id " +
            "JOIN channel c ON i.channel_id = c.id " +
            "WHERE it.tag_id = #{tagId} " +
            "ORDER BY i.pub_date DESC "
    )
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "title", column = "title"),
            @Result(property = "description", column = "description"),
            @Result(property = "pubDate", column = "pub_date"),
            @Result(property = "link", column = "link"),
            @Result(property = "imageUrl", column = "image_url"),
            @Result(property = "channelTitle", column = "channelTitle"),
            @Result(property = "channelIcon", column = "channelIcon"),
            @Result(property = "aiDescription", column = "ai_description"),
    })
    List<ItemVo> getItemVosByTagId(@Param("tagId") Integer tagId);
}




