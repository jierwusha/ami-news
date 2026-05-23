package com.project.aminewsbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.aminewsbackend.entity.HotWordItem;
import com.project.aminewsbackend.mapper.HotWordItemMapper;
import org.springframework.stereotype.Service;

/**
* @author ARounder
* @description 针对表【hot_word_item】的数据库操作Service实现
* @createDate 2025-06-27 09:26:42
*/
@Service
public class HotWordItemService extends ServiceImpl<HotWordItemMapper, HotWordItem>
    implements IService<HotWordItem> {

}




