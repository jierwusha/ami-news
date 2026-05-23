package com.project.aminewsbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.aminewsbackend.entity.Subscribe;
import com.project.aminewsbackend.mapper.SubscribeMapper;
import org.springframework.stereotype.Service;

/**
* @author ARounder
* @description 针对表【subscribe】的数据库操作Service实现
* @createDate 2025-06-24 16:16:01
*/
@Service
public class SubscribeService extends ServiceImpl<SubscribeMapper, Subscribe>
    implements IService<Subscribe> {

}




