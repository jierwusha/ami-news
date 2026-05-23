package com.project.aminewsbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.aminewsbackend.entity.Item;
import com.project.aminewsbackend.mapper.ItemMapper;
import com.project.aminewsbackend.utils.Result;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/**
* @author ARounder
* @description 针对表【item】的数据库操作Service实现
* @createDate 2025-06-24 16:16:01
*/
@Service
public class ItemService extends ServiceImpl<ItemMapper, Item>
    implements IService<Item> {

    private final ItemMapper itemMapper;

    public ItemService(ItemMapper itemMapper) {
        this.itemMapper = itemMapper;
    }

    public Result getItemById(Integer itemId) {
        System.out.println("[ItemService] 查询Item，itemId=" + itemId);
        Item item = itemMapper.selectById(itemId);
        if(item == null) {
            System.out.println("[ItemService] 未找到Item，itemId=" + itemId);
            return Result.error(401, "找不到Item");
        }
        System.out.println("[ItemService] 查询到Item，itemId=" + itemId);
        return Result.success(item);
    }
    
    /**
     * 从HTML描述中提取第一个图片链接
     * @param description HTML描述内容
     * @return 第一个图片链接，如果没有找到则返回空字符串
     */
    public String extractFirstImageUrl(String description) {
        if (description == null || description.trim().isEmpty()) {
            System.out.println("[ItemService] 描述为空，无法提取图片链接");
            return "";
        }
        
        // 正则表达式匹配img标签的src属性
        Pattern pattern = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(description);
        
        if (matcher.find()) {
            System.out.println("[ItemService] 成功提取图片链接: " + matcher.group(1));
            return matcher.group(1);
        }
        
        System.out.println("[ItemService] 未提取到图片链接");
        return "";
    }

}
