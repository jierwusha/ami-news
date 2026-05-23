package com.project.aminewsbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.aminewsbackend.entity.Item;
import com.project.aminewsbackend.entity.Subscribe;
import com.project.aminewsbackend.mapper.ChannelMapper;
import com.project.aminewsbackend.entity.Channel;
import com.project.aminewsbackend.mapper.ItemMapper;
import com.project.aminewsbackend.mapper.SubscribeMapper;
import com.project.aminewsbackend.utils.Result;
import com.project.aminewsbackend.utils.UserThreadLocal;
import com.project.aminewsbackend.vo.ItemVo;
import com.project.aminewsbackend.vo.PageItemVo;
import com.project.aminewsbackend.vo.SearchChannelVo;
import com.project.aminewsbackend.vo.SearchItemVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
* @author ARounder
* @description 针对表【channel】的数据库操作Service实现
* @createDate 2025-06-24 16:16:02
*/
@Service
public class ChannelService extends ServiceImpl<ChannelMapper, Channel>
    implements IService<Channel> {

    @Autowired
    private ItemMapper itemMapper;
    @Autowired
    private RssService rssService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private SubscribeMapper subscribeMapper;

    public Result getItemsByChannelId(String channelId, Integer pageNum, Integer pageSize) {
        System.out.println("[ChannelService] 查询频道下Item，channelId=" + channelId + ", pageNum=" + pageNum + ", pageSize=" + pageSize);
        QueryWrapper<Item> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("channel_id", channelId).orderByDesc("pub_date");
        Page<Item> page = new Page<>(pageNum, pageSize);
        List<Item> items = itemMapper.selectPage(page, queryWrapper).getRecords();
        List<ItemVo> itemVos = new ArrayList<>();
        for(Item item : items) {
            ItemVo itemVo = new ItemVo();
            BeanUtils.copyProperties(item, itemVo);
            // 提取第一个图片链接
            String firstImageUrl = itemService.extractFirstImageUrl(item.getDescription());
            itemVo.setImageUrl(firstImageUrl);
            itemVos.add(itemVo);
        }
        if (items.isEmpty()) {
            System.out.println("[ChannelService] 没有找到对应的Item，channelId=" + channelId);
            return Result.error(401,"没有找到对应的Item");
        }
        PageItemVo pageItemVo = new PageItemVo();
        pageItemVo.setItems(itemVos);
        pageItemVo.setPageNum(pageNum);
        pageItemVo.setPageSize(pageSize);
        pageItemVo.setHasNext(page.hasNext());
        System.out.println("[ChannelService] 查询到Item数量: " + itemVos.size() + ", channelId=" + channelId);
        return Result.success(pageItemVo, "查询Item成功");
    }

    public Result getRandomChannel(Integer num) {
        System.out.println("[ChannelService] 随机查询频道，数量: " + num);
        QueryWrapper<Channel> queryWrapper = new QueryWrapper<>();
        queryWrapper.last("ORDER BY RAND() LIMIT " + num);
        List<Channel> channels = this.list(queryWrapper);
        List<SearchChannelVo> searchChannelVoList = convertToSearchChannelVoList(channels);
        if (channels.isEmpty()) {
            System.out.println("[ChannelService] 没有找到对应的Channel");
            return Result.error(401,"没有找到对应的Channel");
        }
        System.out.println("[ChannelService] 查询到频道数量: " + channels.size());
        return Result.success(searchChannelVoList, "查询Channel成功");
    }

    public Result searchChannels(String keyword) throws Exception {
        System.out.println("[ChannelService] 搜索频道，关键字: " + keyword);
        // 如果keyword是链接
        if (keyword.startsWith("http://") || keyword.startsWith("https://")) {
            Result result = rssService.addRss(keyword);
            // TODO 设置VO
            Channel channel = (Channel) result.getData();
            // 将Channel转成 SearchChannelVo
            List<Channel> channelList = new ArrayList<>();
            channelList.add(channel);
            List<SearchChannelVo> searchChannelVoList = convertToSearchChannelVoList(channelList);
            System.out.println("[ChannelService] 通过链接添加Channel成功: " + keyword);
            return Result.success(searchChannelVoList, "通过链接添加Channel成功");
        }
        // 不是链接，则通过title查询
        QueryWrapper<Channel> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("title", keyword)
                    .or()
                    .like("description", keyword)
                    .or()
                    .like("url", keyword)
                    .select("DISTINCT *");
        List<Channel> channels = this.list(queryWrapper);
        if (channels.isEmpty()) {
            System.out.println("[ChannelService] 没有找到对应的Channel，关键字: " + keyword);
            return Result.error(401,"没有找到对应的Channel");
        }
        // TODO 设置VO
        List<SearchChannelVo> searchChannelVoList = convertToSearchChannelVoList(channels);
        System.out.println("[ChannelService] 查询到频道数量: " + channels.size() + "，关键字: " + keyword);
        return Result.success(searchChannelVoList, "查询Channel成功");
    }

    public List<SearchChannelVo> convertToSearchChannelVoList(List<Channel> channelList) {
        System.out.println("[ChannelService] 转换Channel为SearchChannelVo，数量: " + channelList.size());
        List<SearchChannelVo> searchChannelVoList = new ArrayList<>();
        for (Channel channel : channelList) {
            SearchChannelVo searchChannelVo = new SearchChannelVo();
            BeanUtils.copyProperties(channel, searchChannelVo);
            QueryWrapper<Item> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("channel_id", channel.getId());
            queryWrapper.orderByDesc("pub_date").last("LIMIT 3");
            List<Item> items = itemMapper.selectList(queryWrapper);
            List<SearchItemVo> searchItemVoList = new ArrayList<>();
            for (Item item : items) {
                SearchItemVo searchItemVo = new SearchItemVo();
                BeanUtils.copyProperties(item, searchItemVo);
                searchItemVoList.add(searchItemVo);
            }
            searchChannelVo.setItems(searchItemVoList);
            // 设置是否订阅
            Subscribe subscribe = subscribeMapper.selectOne(new QueryWrapper<Subscribe>().eq("channel_id", channel.getId()).eq("user_id", UserThreadLocal.getUser().getId()));
            if(subscribe != null) {
                searchChannelVo.setSubscribed(true);
            } else {
                searchChannelVo.setSubscribed(false);
            }
            searchChannelVoList.add(searchChannelVo);
        }
        System.out.println("[ChannelService] 转换完成，SearchChannelVo数量: " + searchChannelVoList.size());
        return searchChannelVoList;
    }
}
