package com.project.aminewsbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.aminewsbackend.entity.Item;
import com.project.aminewsbackend.entity.ReadLater;
import com.project.aminewsbackend.mapper.ReadLaterMapper;
import com.project.aminewsbackend.utils.Result;
import com.project.aminewsbackend.utils.UserThreadLocal;
import com.project.aminewsbackend.vo.ItemVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
* @author ARounder
* @description 针对表【read_later】的数据库操作Service实现
* @createDate 2025-06-24 16:16:01
*/
@Service
public class ReadLaterService extends ServiceImpl<ReadLaterMapper, ReadLater>
    implements IService<ReadLater> {

    private final ReadLaterMapper readLaterMapper;
    private final ItemService itemService;

    public ReadLaterService(ReadLaterMapper readLaterMapper, ItemService itemService) {
        this.readLaterMapper = readLaterMapper;
        this.itemService = itemService;
    }

    public Result addToReadLater(Integer itemId) {
        System.out.println("[ReadLaterService] addToReadLater called, itemId=" + itemId + ", userId=" + UserThreadLocal.getUser().getId());
        // 检查是否已存在
        ReadLater existingReadLater = this.baseMapper.selectOne(
                new LambdaQueryWrapper<ReadLater>()
                        .eq(ReadLater::getItemId, itemId)
                        .eq(ReadLater::getUserId, UserThreadLocal.getUser().getId())
        );
        if (existingReadLater != null) {
            System.out.println("[ReadLaterService] item already exists in read later list, itemId=" + itemId);
            return Result.error(400, "该文章已在稍后阅读列表中");
        }
        ReadLater readLater = new ReadLater();
        readLater.setItemId(itemId);
        readLater.setUserId(UserThreadLocal.getUser().getId());
        this.save(readLater);
        System.out.println("[ReadLaterService] item added to read later, itemId=" + itemId);
        return Result.success("已添加到稍后阅读");
    }

    public Result removeFromReadLater(Integer itemId) {
        System.out.println("[ReadLaterService] removeFromReadLater called, itemId=" + itemId + ", userId=" + UserThreadLocal.getUser().getId());
        readLaterMapper.delete(new QueryWrapper<ReadLater>().eq("item_id", itemId)
                .eq("user_id", UserThreadLocal.getUser().getId()));
        System.out.println("[ReadLaterService] item removed from read later, itemId=" + itemId);
        return Result.success("已从稍后阅读中移除");
    }

    public Result listReadLaterItems(Integer pageNum, Integer pageSize) {
        System.out.println("[ReadLaterService] listReadLaterItems called, pageNum=" + pageNum + ", pageSize=" + pageSize + ", userId=" + UserThreadLocal.getUser().getId());
        Integer offset = (pageNum - 1) * pageSize;
        List<ItemVo> itemVos = readLaterMapper.listItemsByUserId(UserThreadLocal.getUser().getId(), offset, pageSize);
        for(ItemVo itemVo : itemVos) {
            String firstImageUrl = itemService.extractFirstImageUrl(itemVo.getDescription());
            itemVo.setImageUrl(firstImageUrl);
        }
        return Result.success(itemVos, "成功获取稍后阅读列表");
    }
}
