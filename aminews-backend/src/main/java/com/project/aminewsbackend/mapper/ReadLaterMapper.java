package com.project.aminewsbackend.mapper;

import com.project.aminewsbackend.entity.Item;
import com.project.aminewsbackend.entity.ReadLater;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.aminewsbackend.vo.ItemVo;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author ARounder
* @description 针对表【read_later】的数据库操作Mapper
* @createDate 2025-06-24 16:16:01
* @Entity generator.entity.ReadLater
*/
public interface ReadLaterMapper extends BaseMapper<ReadLater> {

    @Select("SELECT i.*, c.title AS channelTitle, c.icon AS channelIcon FROM item i " +
            "JOIN read_later rl ON i.id = rl.item_id " +
            "JOIN channel c ON i.channel_id = c.id " +
            "WHERE rl.user_id = #{id} " +
            "ORDER BY rl.create_time DESC " +
            "LIMIT #{offset}, #{pageSize}")
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
    List<ItemVo> listItemsByUserId(Integer id, Integer offset, Integer pageSize);
}




