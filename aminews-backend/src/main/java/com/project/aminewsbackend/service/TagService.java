package com.project.aminewsbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.aminewsbackend.entity.Item;
import com.project.aminewsbackend.entity.ItemTag;
import com.project.aminewsbackend.entity.Tag;
import com.project.aminewsbackend.mapper.ItemMapper;
import com.project.aminewsbackend.mapper.TagMapper;
import com.project.aminewsbackend.utils.Result;
import com.project.aminewsbackend.utils.UserThreadLocal;
import com.project.aminewsbackend.mapper.ItemTagMapper;
import com.project.aminewsbackend.vo.ItemVo;
import com.project.aminewsbackend.vo.TagVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/**
* @author ARounder
* @description 针对表【tag】的数据库操作Service实现
* @createDate 2025-06-24 16:16:02
*/
@Service
public class TagService extends ServiceImpl<TagMapper, Tag>
    implements IService<Tag> {

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ItemTagMapper itemTagMapper;
    @Autowired
    private TagMapper tagMapper;
    @Autowired
    private ItemService itemService;

    public Result createTag(String name) {
        // 检查标签名是否已存在
        Tag existingTag = this.getOne(new QueryWrapper<Tag>().eq("name", name).eq("user_id", UserThreadLocal.getUser().getId()));
        if (existingTag != null) {
            System.out.println("[TagService] 创建标签失败：标签名已存在 -> " + name);
            return Result.error(400, "标签名已存在");
        }
        Tag tag = new Tag();
        tag.setName(name);
        tag.setUserId(UserThreadLocal.getUser().getId());
        boolean save = this.save(tag);
        System.out.println("[TagService] 新建标签保存结果: " + save + "，标签名: " + name);
        if (save) {
            System.out.println("[TagService] 标签创建成功: " + name);
            return Result.success(tag);
        } else {
            System.out.println("[TagService] 标签创建失败: " + name);
            return Result.error(402, "创建标签失败");
        }
    }


    public Result addTagToItem(Integer itemId, Integer tagId) {
        // 检查标签是否存在
        Tag tag = this.getById(tagId);
        if (tag == null) {
            System.out.println("[TagService] 添加标签到项目失败：标签不存在，tagId=" + tagId);
            return Result.error(404, "标签不存在");
        }
        // 检查项目是否存在
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            System.out.println("[TagService] 添加标签到项目失败：项目不存在，itemId=" + itemId);
            return Result.error(404, "项目不存在");
        }
        // 添加标签到项目
        ItemTag itemTag = itemTagMapper.selectOne(new QueryWrapper<ItemTag>().eq("item_id", itemId).eq("tag_id", tagId));
        if (itemTag != null) {
            System.out.println("[TagService] 项目已包含此标签，itemId=" + itemId + ", tagId=" + tagId);
            return Result.error(400, "项目已包含此标签");
        }
        itemTag = new ItemTag();
        itemTag.setItemId(itemId);
        itemTag.setTagId(tagId);
        itemTagMapper.insert(itemTag);
        System.out.println("[TagService] 标签添加到项目成功，itemId=" + itemId + ", tagId=" + tagId);
        return Result.success("标签添加成功");
    }

    public Result removeTagFromItem(Integer itemId, Integer tagId) {
        // 移除标签从项目
        int delCount = itemTagMapper.delete(new QueryWrapper<ItemTag>().eq("item_id", itemId).eq("tag_id", tagId));
        System.out.println("[TagService] 标签移除结果，itemId=" + itemId + ", tagId=" + tagId + ", 删除数: " + delCount);
        return Result.success("标签移除成功");
    }

    public Result deleteTag(Integer tagId) {
        int delItemTag = itemTagMapper.delete(new QueryWrapper<ItemTag>().eq("tag_id", tagId));
        int delTag = tagMapper.deleteById(tagId);
        System.out.println("[TagService] 标签删除，tagId=" + tagId + ", 删除ItemTag数: " + delItemTag + ", 删除Tag数: " + delTag);
        return Result.success("标签删除成功");
    }

    public Result listTags() {
        QueryWrapper<Tag> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", UserThreadLocal.getUser().getId());
        queryWrapper.orderByAsc("create_time");
        List<Tag> tags = this.list(queryWrapper);
        if (tags.isEmpty()) {
            System.out.println("[TagService] 查询标签为空");
            return Result.error(404, "没有找到标签");
        }
        List<TagVo> tagVos = new ArrayList<>();
        for(Tag tag : tags) {
            TagVo tagVo = new TagVo();
            BeanUtils.copyProperties(tag, tagVo);
            // 查询标签下的项目数量
            Integer itemCount = Math.toIntExact(itemTagMapper.selectCount(new QueryWrapper<ItemTag>().eq("tag_id", tag.getId())));
            tagVo.setItemCount(itemCount);
            tagVos.add(tagVo);
        }
        System.out.println("[TagService] 查询标签成功，数量: " + tags.size());
        return Result.success(tagVos, "查询标签成功");
    }

    public Result getItemsByTagId(@RequestParam Integer tagId) {
        List<ItemVo> items = tagMapper.getItemVosByTagId(tagId);
        for(ItemVo itemVo : items) {
            itemVo.setImageUrl(itemService.extractFirstImageUrl(itemVo.getDescription()));
        }
        System.out.println("[TagService] 查询标签对应的项目成功，tagId=" + tagId + "，项目数: " + items.size());
        return Result.success(items, "查询标签对应的项目成功");
    }
}
