package com.project.aminewsbackend.mapper;

import com.project.aminewsbackend.dto.FolderDTO;
import com.project.aminewsbackend.entity.Folder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author ARounder
* @description 针对表【folder】的数据库操作Mapper
* @createDate 2025-06-27 09:19:55
* @Entity com.project.aminewsbackend.entity.Folder
*/
public interface FolderMapper extends BaseMapper<Folder> {

}




