package com.project.aminewsbackend.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.project.aminewsbackend.entity.Channel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author ARounder
* @description 针对表【channel】的数据库操作Mapper
* @createDate 2025-06-24 16:16:02
* @Entity generator.entity.Channel
*/
public interface ChannelMapper extends BaseMapper<Channel> {

    @Select("SELECT DISTINCT c.* FROM channel c " +
    " JOIN subscribe s ON s.channel_id = c.id "
    )
    List<Channel> getAllSubscribedChannels();

    @Select("SELECT DISTINCT c.* FROM channel c " +
            "JOIN folder_channel fc ON c.id = fc.channel_id " +
            "WHERE fc.folder_id = #{folderId}")
    List<Channel> selectChannelListByFolderId(@Param("folderId") Integer folderId);

    
}




