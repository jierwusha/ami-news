package com.project.aminewsbackend.mapper;

import com.project.aminewsbackend.entity.Subscribe;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
* @author ARounder
* @description 针对表【subscribe】的数据库操作Mapper
* @createDate 2025-06-24 16:16:01
* @Entity generator.entity.Subscribe
*/
public interface SubscribeMapper extends BaseMapper<Subscribe> {

    @Select("SELECT s.* FROM subscribe s "
    + "JOIN channel c ON s.channel_id = c.id "
            + "WHERE c.url = #{url} AND s.user_id = #{userId}"
    )
    public Subscribe getSubscribeByUrlAndUserId(@Param("url") String url, @Param("userId") Integer userId);

    @Select("SELECT DISTINCT s.channel_id FROM subscribe s "
        + "WHERE s.user_id = #{userId}")
    public List<Integer> selectChannelIdsByUserId(@Param("userId") Integer userId);

    @Delete("DELETE s FROM subscribe s "
            + "LEFT JOIN folder_channel fc ON s.channel_id = fc.channel_id "
            + "WHERE user_id = #{userId} AND fc.channel_id IS NULL"
    )
    void deleteChannelNotInOtherFolders(@Param("userId") Integer id);
}




