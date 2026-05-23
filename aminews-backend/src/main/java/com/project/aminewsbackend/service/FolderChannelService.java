package com.project.aminewsbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.aminewsbackend.entity.Folder;
import com.project.aminewsbackend.entity.FolderChannel;
import com.project.aminewsbackend.mapper.FolderChannelMapper;
import org.springframework.stereotype.Service;

/**
* @author ARounder
* @description 针对表【category】的数据库操作Service实现
* @createDate 2025-06-24 16:16:02
*/
@Service
public class FolderChannelService extends ServiceImpl<FolderChannelMapper, FolderChannel>
    implements IService<FolderChannel> {

}




