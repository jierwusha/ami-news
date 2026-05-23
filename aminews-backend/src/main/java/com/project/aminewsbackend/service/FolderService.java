package com.project.aminewsbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.aminewsbackend.dto.ChannelDTO;
import com.project.aminewsbackend.dto.FolderDTO;
import com.project.aminewsbackend.dto.ItemDTO;
import com.project.aminewsbackend.entity.Channel;
import com.project.aminewsbackend.entity.Folder;
import com.project.aminewsbackend.entity.FolderChannel;
import com.project.aminewsbackend.entity.Item;
import com.project.aminewsbackend.mapper.*;
import com.project.aminewsbackend.utils.Result;
import com.project.aminewsbackend.utils.UserThreadLocal;
import com.project.aminewsbackend.vo.ItemVo;
import com.project.aminewsbackend.vo.PageItemVo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
* @author ARounder
* @description 针对表【folder】的数据库操作Service实现
* @createDate 2025-06-27 09:19:55
*/
@Service
public class FolderService extends ServiceImpl<FolderMapper, Folder>
    implements IService<Folder> {


    @Autowired
    private FolderChannelMapper folderChannelMapper;

    @Autowired
    private ChannelMapper channelMapper;
    @Autowired
    private ItemMapper itemMapper;
    @Autowired
    private FolderMapper folderMapper;
    @Autowired
    private ItemService itemService;
    @Autowired
    private SubscribeMapper subscribeMapper;

    public Result createFolder(String name) {
        System.out.println("[FolderService] 创建文件夹: " + name);
        if(name == "ROOTFOLDER") {
            System.out.println("[FolderService] 文件夹名不能为ROOTFOLDER");
            return Result.error(400, "文件夹名不能为ROOTFOLDER");
        }
        Folder folder = new Folder();
        folder.setName(name);
        folder.setUserId(UserThreadLocal.getUser().getId());
        // 检查文件夹名是否已存在
        Folder existingFolder = this.getOne(new QueryWrapper<Folder>().eq("name", name).eq("user_id", UserThreadLocal.getUser().getId()));
        if (existingFolder != null) {
            System.out.println("[FolderService] 文件夹名已存在: " + name);
            return Result.error(400, "文件夹名已存在");
        }
        boolean save = this.save(folder);
        System.out.println("[FolderService] 文件夹保存结果: " + save + "，文件夹名: " + name);
        if (save) {
            System.out.println("[FolderService] 文件夹创建成功: " + name);
            return Result.success(folder);
        } else {
            System.out.println("[FolderService] 创建文件夹失败: " + name);
            return Result.error(402,"创建文件夹失败");
        }
    }

    public String createRootFolder(Integer userId) {
        System.out.println("[FolderService] 创建根文件夹，userId=" + userId);
        Folder folder = new Folder();
        folder.setName("ROOTFOLDER");
        folder.setUserId(userId);
        boolean save = this.save(folder);
        System.out.println("[FolderService] 根文件夹保存结果: " + save);
        return "创建根文件夹成功";
    }

    public Result addChannelToFolder(Integer folderId, Integer channelId) {
        System.out.println("[FolderService] 添加频道到文件夹，folderId=" + folderId + ", channelId=" + channelId);
        // 检查文件夹是否存在
        Folder folder = this.getOne(new QueryWrapper<Folder>().eq("id", folderId).eq("user_id", UserThreadLocal.getUser().getId()));
        if (folder == null) {
            System.out.println("[FolderService] 文件夹不存在，folderId=" + folderId);
            return Result.error(404, "文件夹不存在");
        }
        // 检查频道是否存在
        Channel channel = channelMapper.selectById(channelId);
        if (channel == null) {
            System.out.println("[FolderService] 频道不存在，channelId=" + channelId);
            return Result.error(404, "频道不存在");
        }
        // 检查频道是否已在文件夹中
        FolderChannel existingFolderChannel = folderChannelMapper.selectOne(new QueryWrapper<FolderChannel>()
                .eq("folder_id", folderId)
                .eq("channel_id", channelId));
        if (existingFolderChannel != null) {
            System.out.println("[FolderService] 频道已在文件夹中，folderId=" + folderId + ", channelId=" + channelId);
            return Result.error(400, "频道已在文件夹中");
        }
        // 从根文件夹中移除频道
        folderChannelMapper.removeFromRootFolder(channelId, UserThreadLocal.getUser().getId());
        // 添加频道到文件夹
        FolderChannel folderChannel = new FolderChannel();
        folderChannel.setFolderId(folderId);
        folderChannel.setChannelId(channelId);
        folderChannelMapper.insert(folderChannel);
        System.out.println("[FolderService] 频道已成功添加到文件夹，folderId=" + folderId + ", channelId=" + channelId);
        return Result.success("频道已成功添加到文件夹");
    }

    public Result removeChannelFromFolder(Integer folderId, Integer channelId) {
        System.out.println("[FolderService] 从文件夹移除频道，folderId=" + folderId + ", channelId=" + channelId);
        QueryWrapper<FolderChannel> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("folder_id", folderId)
                    .eq("channel_id", channelId);
        folderChannelMapper.delete(queryWrapper);
        this.addToRootFolder(channelId, UserThreadLocal.getUser().getId());
        System.out.println("[FolderService] 频道已成功从文件夹中移除，folderId=" + folderId + ", channelId=" + channelId);
        return Result.success("频道已成功从文件夹中移除");
    }

    public Result deleteFolder(Integer folderId) {
        System.out.println("[FolderService] 删除文件夹，folderId=" + folderId);
        // 删除所有频道与文件夹的关联
        // TODO DEBUG 1.删除文件夹时不取消订阅 2.频道处于多个文件夹中时，删除文件夹后，会加入根
        QueryWrapper<FolderChannel> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("folder_id", folderId);
        folderChannelMapper.delete(queryWrapper);
        // 取消订阅未被其他文件夹持有的频道
        subscribeMapper.deleteChannelNotInOtherFolders(UserThreadLocal.getUser().getId());
        // 删除文件夹
        Folder folder = folderMapper.selectOne(new QueryWrapper<Folder>().eq("id", folderId).eq("user_id", UserThreadLocal.getUser().getId()));
        if(folder.getName() == "ROOTFOLDER") {
            System.out.println("[FolderService] 不能删除根文件夹，folderId=" + folderId);
            return Result.error(400, "不能删除根文件夹");
        }
        boolean remove = this.removeById(folderId);
        System.out.println("[FolderService] 文件夹删除结果: " + remove + ", folderId=" + folderId);
        if (remove) {
            System.out.println("[FolderService] 文件夹已成功删除，folderId=" + folderId);
            return Result.success("文件夹已成功删除");
        } else {
            System.out.println("[FolderService] 文件夹不存在或删除失败，folderId=" + folderId);
            return Result.error(404, "文件夹不存在或删除失败");
        }
    }

    public Result getNewFolders() {
        System.out.println("[FolderService] 查询用户所有文件夹及其频道和最新新闻");
        // 查出用户的所有文件夹
        // 查出用户所有文件夹下的所有channel
        // 查出用户所有channel下的最新的五个item
        Integer userId = UserThreadLocal.getUser().getId();
        QueryWrapper<Folder> folderQueryWrapper = new QueryWrapper<>();
        folderQueryWrapper.eq("user_id", userId);
        folderQueryWrapper.orderByDesc("create_time");
        List<Folder> folders = folderMapper.selectList(folderQueryWrapper);

        List<FolderDTO> folderDTOS = new ArrayList<>();
        for(Folder folder : folders) {
            // 复制属性
            FolderDTO folderDTO = new FolderDTO();
            folderDTO.setId(folder.getId());
            folderDTO.setName(folder.getName());
            List<Channel> channels = channelMapper.selectChannelListByFolderId(folder.getId());
            List<ChannelDTO> channelDTOS = new ArrayList<>();
            for (Channel channel : channels) {
                ChannelDTO channelDTO = new ChannelDTO();
                channelDTO.setId(channel.getId());
                channelDTO.setName(channel.getTitle());
                channelDTO.setDescription(channel.getDescription());
                channelDTO.setIcon(channel.getIcon());
                channelDTO.setLink(channel.getLink());
                // 这里可以添加更多的属性复制
                List<Item> items = itemMapper.selectList(
                    new QueryWrapper<Item>().eq("channel_id", channel.getId())
                            .orderByDesc("pub_date")
                            .orderByDesc("create_time")
                            .last("LIMIT 5")
                );
                List<ItemDTO> itemDTOS = new ArrayList<>();
                for( Item item : items) {
                    ItemDTO itemDTO = new ItemDTO();
                    itemDTO.setId(item.getId());
                    itemDTO.setTitle(item.getTitle());
                    itemDTO.setPubDate(item.getPubDate());
                    itemDTOS.add(itemDTO);
                }
                channelDTO.setTop5news(itemDTOS);
                channelDTOS.add(channelDTO);
            }
            folderDTO.setChannels(channelDTOS);
            folderDTOS.add(folderDTO);
        }
        System.out.println("[FolderService] 查询到文件夹数量: " + folderDTOS.size());
        return Result.success(folderDTOS);
    }

    public void addToRootFolder(Integer channelId, Integer userId) {
        System.out.println("[FolderService] 尝试将频道添加到根文件夹，channelId=" + channelId + ", userId=" + userId);
        // 查询ROOTFOLDER文件夹
        Folder rootFolder = folderMapper.selectOne(
                new QueryWrapper<Folder>()
                        .eq("user_id", userId)
                        .eq("name", "ROOTFOLDER")
        );
        System.out.println("[FolderService] 查询到的ROOTFOLDER: " + rootFolder);
        if (rootFolder != null) {
            // 检查频道是否存在于其他文件夹中
            List<FolderChannel> existingFolderChannel = folderChannelMapper.getElseFolderChannel(channelId, userId);
            System.out.println("[FolderService] 查询到的其他文件夹中的频道: " + existingFolderChannel);
            if(!existingFolderChannel.isEmpty()) {
                // 如果频道已存在于其他文件夹中，则不添加到根文件夹
                System.out.println("[FolderService] 频道已存在于其他文件夹中，不添加到根文件夹");
                return;
            }
            FolderChannel folderChannel = new FolderChannel();
            folderChannel.setFolderId(rootFolder.getId());
            folderChannel.setChannelId(channelId);
            folderChannelMapper.insert(folderChannel);
            System.out.println("[FolderService] 频道已添加到根文件夹，channelId=" + channelId);
        }
    }

    public Result getItemsByFolderId(Integer folderId, Integer pageNum, Integer pageSize) {
        System.out.println("[FolderService] 查询文件夹下Item，folderId=" + folderId + ", pageNum=" + pageNum + ", pageSize=" + pageSize);
        Integer offset = (pageNum - 1) * pageSize;
        List<ItemVo> itemVos = itemMapper.getItemsByFolderId(folderId, offset, pageSize + 1);
        for(ItemVo itemVo : itemVos) {
            String firstImageUrl = itemService.extractFirstImageUrl(itemVo.getDescription());
            itemVo.setImageUrl(firstImageUrl);
        }
        boolean hasNext = itemVos.size() > pageSize;
        if (hasNext) {
            // 移除最后一个多余的元素
            itemVos.remove(itemVos.size() - 1);
        }
        System.out.println("[FolderService] 查询到Item数量: " + itemVos.size() + ", hasNext=" + hasNext);
        PageItemVo pageItemVo = new PageItemVo();
        pageItemVo.setItems(itemVos);
        pageItemVo.setPageNum(pageNum);
        pageItemVo.setPageSize(pageSize);
        pageItemVo.setHasNext(hasNext);
        return Result.success(pageItemVo, "查询Item成功");
    }
}
