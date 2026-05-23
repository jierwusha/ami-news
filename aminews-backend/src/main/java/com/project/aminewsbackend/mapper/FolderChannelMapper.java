package com.project.aminewsbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.aminewsbackend.entity.FolderChannel;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author ARounder
* @description 针对表【category】的数据库操作Mapper
* @createDate 2025-06-24 16:16:02
* @Entity generator.entity.Category
*/
public interface FolderChannelMapper extends BaseMapper<FolderChannel> {

    @Delete("DELETE FROM folder_channel WHERE channel_id = #{channelId} AND folder_id IN (SELECT f.id FROM folder f WHERE f.user_id = #{userId} AND f.name = 'ROOTFOLDER')")
    void removeFromRootFolder(@Param("channelId")Integer channelId, @Param("userId")Integer userId);


    @Select("SELECT * FROM folder_channel WHERE channel_id = #{channelId} AND folder_id IN (SELECT id FROM folder WHERE user_id = #{userId} AND name = 'ROOTFOLDER')")
    List<FolderChannel> getElseFolderChannel(@Param("channelId")Integer channelId, @Param("userId")Integer userId);

   @Delete("DELETE FROM folder_channel WHERE folder_id IN (SELECT id FROM folder WHERE user_id = #{userId}) AND channel_id = #{channelId}")
   void removeFromAllFolder(@Param("userId") Integer userId, @Param("channelId") Integer channelId);
}




