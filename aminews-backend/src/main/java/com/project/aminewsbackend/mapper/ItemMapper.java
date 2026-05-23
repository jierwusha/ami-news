package com.project.aminewsbackend.mapper;

import com.project.aminewsbackend.dto.ItemChannelInfo;
import com.project.aminewsbackend.entity.Item;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.aminewsbackend.vo.ItemVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author ARounder
* @description 针对表【item】的数据库操作Mapper
* @createDate 2025-06-24 16:16:01
* @Entity generator.entity.Item
*/
public interface ItemMapper extends BaseMapper<Item> {

    @Select("SELECT DISTINCT i.*, c.title AS channelTitle, c.icon AS channelIcon FROM item i " +
            "JOIN folder_channel fc ON i.channel_id = fc.channel_id " +
            "JOIN channel c ON i.channel_id = c.id " +
            "WHERE fc.folder_id = #{folderId} " +
            "ORDER BY i.pub_date DESC " +
            "LIMIT #{pageSize} OFFSET #{offset}"
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
    List<ItemVo> getItemsByFolderId(@Param("folderId")Integer folderId, @Param("offset")Integer offset, @Param("pageSize") Integer pageSize);

    @Select("SELECT i.id as item_id, c.title as channel_title, c.icon as channel_icon " +
        "FROM item i LEFT JOIN channel c ON i.channel_id = c.id " +
        "WHERE i.id IN (${itemIds})")
    List<ItemChannelInfo> getItemChannelInfoByIds(@Param("itemIds") String itemIds);




    /**
     * 获取最近N天的新闻
     */
    @Select("SELECT * FROM item WHERE pub_date >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "ORDER BY pub_date DESC LIMIT #{limit}")
    List<Item> selectRecentItems(@Param("days") int days, @Param("limit") int limit);
    

}




