package com.project.aminewsbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.project.aminewsbackend.entity.Item;
import com.project.aminewsbackend.entity.User;
import com.project.aminewsbackend.dto.HotWordDTO;
import com.project.aminewsbackend.dto.ItemChannelInfo;
import com.project.aminewsbackend.vo.ItemVo;
import com.project.aminewsbackend.entity.Channel;
import com.project.aminewsbackend.entity.HotWord;
import com.project.aminewsbackend.entity.HotWordItem;
import com.project.aminewsbackend.mapper.ItemMapper;
import com.project.aminewsbackend.mapper.SubscribeMapper;
import com.project.aminewsbackend.mapper.UserMapper;
import com.project.aminewsbackend.utils.UserThreadLocal;
import com.project.aminewsbackend.mapper.HotWordMapper;
import com.project.aminewsbackend.mapper.HotWordItemMapper;
import com.project.aminewsbackend.mapper.ChannelMapper;
import com.hankcs.hanlp.HanLP;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HotWordService {

    @Autowired
    private ItemMapper itemMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SubscribeMapper subscribeMapper;
    @Autowired
    private HotWordMapper hotWordMapper;
    @Autowired
    private HotWordItemMapper hotWordItemMapper;
    @Autowired
    private ItemService itemService;
    @Autowired
    private ChannelMapper channelMapper;

    /** * 每个用户的热词数量限制
     * 这里设置为66，可以根据实际需求调整
     */
    private int hotwordsPerUser=66;

    /**
     * 需要过滤的无效热词
     */
    private static final Set<String> FILTERED_WORDS = Set.of(
        // 技术垃圾词
        "htmlVideoCode","htmlVideoCode--",
        
        // 时间相关虚词
        "现在", "今年", "去年", "明年", "昨天", "今天", "明天", "最近", "刚刚", "目前", "当前", "此时", "这时",
        
        // 人物泛指词
        "男子", "女子", "男人", "女人", "人们", "大家", "某人", "这人", "那人", "别人", "他人", "个人",
        
        // 关系连接词
        "相关", "有关", "关于", "针对", "对于", "由于", "因为", "所以", "然后", "接着", "同时", "另外",
        
        // 感受表达词
        "感觉", "觉得", "认为", "以为", "似乎", "好像", "可能", "或许", "大概", "估计", "应该", "必须",
        
        // 程度副词
        "真的", "确实", "非常", "特别", "十分", "很是", "相当", "比较", "更加", "最为", "极其", "异常",
        
        // 指示代词
        "这个", "那个", "这些", "那些", "此类", "该类", "这样", "那样", "如此", "这种", "那种",
        
        // 数量词汇
        "一些", "很多", "不少", "若干", "大量", "少量", "多数", "少数", "全部", "部分", "整个",
        
        // 语气词
        "当然", "显然", "明显", "无疑", "肯定", "一定", "必然", "自然", "果然", "竟然", "居然",
        
        // 位置方位词
        "这里", "那里", "此处", "彼处", "上面", "下面", "前面", "后面", "左边", "右边", "中间",
        
        // 动作行为虚词
        "进行", "实施", "开展", "推进", "落实", "执行", "完成", "实现", "达到", "获得", "取得",
        
        // 状态描述词
        "情况", "状态", "现象", "问题", "事情", "东西", "方面", "角度", "层面", "范围", "领域",
        
        // 新闻常见套话
        "记者", "报道", "消息", "据悉", "了解", "表示", "介绍", "透露", "显示", "反映", "说明",
        
        // 无意义连词
        "以及", "还有", "而且", "并且", "同样", "另一", "其他", "包括", "除了", "不仅", "不但"
    );

    /**
     * 统计指定用户订阅的频道下所有新闻的关键词及其出现频率，并返回前topN高频词及对应新闻ID列表
     */
    public Map<String, List<Long>> getTopHotwordsWithItemsByUser(Integer userId) {
        System.out.println("[HotWordService] 获取用户订阅频道下所有新闻关键词，userId=" + userId);
        // 获取用户订阅的channelId列表
        List<Integer> channelIds = subscribeMapper.selectChannelIdsByUserId(userId);
        if (channelIds == null || channelIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<Item> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("channel_id", channelIds);
        queryWrapper.ge("pub_date", java.time.LocalDate.now().minusDays(3));
        List<Item> items = itemMapper.selectList(queryWrapper);
        Map<String, List<Long>> hotwordMap = new HashMap<>();
        for (Item item : items) {
            String title = item.getTitle() == null ? "" : Jsoup.parse(item.getTitle()).text();
            String description = item.getDescription() == null ? "" : Jsoup.parse(item.getDescription()).text();
            String aiDescription = item.getAiDescription() == null ? "" : item.getAiDescription();
            String content = title + description + aiDescription;
            List<String> keywords = HanLP.extractKeyword(content, 5);
            for (String kw : keywords) {
                if (kw.length() <= 1) continue; // 跳过单字
                if (FILTERED_WORDS.contains(kw)) continue; // 过滤无效词汇
                hotwordMap.computeIfAbsent(kw, k -> new ArrayList<>()).add(item.getId() == null ? null : item.getId().longValue());
            }
        }
        // 统计词频并排序，返回前topN
        System.out.println("[HotWordService] 统计词频并排序，userId=" + userId + "，热词数: " + hotwordMap.size());
        Map<String, List<Long>> result = new LinkedHashMap<>();
        hotwordMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().size() - a.getValue().size())
                .limit(hotwordsPerUser)
                .forEachOrdered(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    /**
     * 统计指定用户订阅的频道下所有新闻的关键词及其出现频率，并返回前topN高频词及对应新闻ID列表
     */
    public Map<String, List<Long>> getTopHotwordsWithItemsByCurrentUser() {
        Integer userId = com.project.aminewsbackend.utils.UserThreadLocal.getUser().getId();
        System.out.println("[HotWordService] 获取当前用户订阅频道下所有新闻关键词，userId=" + userId);
        // 获取用户订阅的channelId列表
        List<Integer> channelIds = subscribeMapper.selectChannelIdsByUserId(userId);
        if (channelIds == null || channelIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<Item> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("channel_id", channelIds);
        queryWrapper.ge("pub_date", java.time.LocalDate.now().minusDays(3));
        List<Item> items = itemMapper.selectList(queryWrapper);
        Map<String, List<Long>> hotwordMap = new HashMap<>();
        for (Item item : items) {
            String title = item.getTitle() == null ? "" : item.getTitle();
            String description = item.getDescription() == null ? "" : Jsoup.parse(item.getDescription()).text();
            String aiDescription = item.getAiDescription() == null ? "" : item.getAiDescription();
            String content = title + description + aiDescription;
            List<String> keywords = HanLP.extractKeyword(content, 5);
            for (String kw : keywords) {
                if (kw.length() <= 1) continue; // 跳过单字
                if (FILTERED_WORDS.contains(kw)) continue; // 过滤无效词汇
                hotwordMap.computeIfAbsent(kw, k -> new ArrayList<>()).add(item.getId() == null ? null : item.getId().longValue());
            }
        }
        // 统计词频并排序，返回前topN
        System.out.println("[HotWordService] 统计词频并排序，userId=" + userId + "，热词数: " + hotwordMap.size());
        Map<String, List<Long>> result = new LinkedHashMap<>();
        hotwordMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().size() - a.getValue().size())
                .limit(hotwordsPerUser)
                .forEachOrdered(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    /**
     * 全量重算并存储用户热词及其关联item
     */
    @Async
    @Transactional
    public void refreshHotWordsForUser(Integer userId) {
        Map<String, List<Long>> hotwords = getTopHotwordsWithItemsByUser(userId);
        System.out.println("[HotWordService] 开始更新热词，userId=" + userId + "，热词数: " + hotwords.size());
        // 先查出该用户所有 hot_word 的 id
        List<HotWord> oldHotWords = hotWordMapper.selectList(new QueryWrapper<HotWord>().eq("user_id", userId));
        if (!oldHotWords.isEmpty()) {
            System.out.println("[HotWordService] 删除旧热词及关联项，userId=" + userId + "，旧热词数: " + oldHotWords.size());
            List<Integer> hotWordIds = oldHotWords.stream().map(HotWord::getId).toList();
            hotWordItemMapper.delete(new QueryWrapper<HotWordItem>().in("hot_word_id", hotWordIds));
        }
        // 再删 hot_word
        hotWordMapper.delete(new QueryWrapper<HotWord>().eq("user_id", userId));
        // 插入新数据
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, List<Long>> entry : hotwords.entrySet()) {
            HotWord hw = new HotWord();
            hw.setWord(entry.getKey());
            hw.setUserId(userId);
            hw.setCount(entry.getValue().size());
            hw.setCreateTime(now);
            hw.setUpdateTime(now);
            hotWordMapper.insert(hw);
            for (Long itemId : entry.getValue()) {
                HotWordItem hwi = new HotWordItem();
                hwi.setHotWordId(hw.getId());
                hwi.setItemId(itemId.intValue());
                hwi.setCreateTime(now);
                hwi.setUpdateTime(now);
                hotWordItemMapper.insert(hwi);
            }
        }
        System.out.println("[HotWordService] 插入新热词数据，userId=" + userId + "，新热词数: " + hotwords.size());
    }

    /**
     * 全量重算并存储当前登录用户热词及其关联item。
     */
    @Transactional
    public void refreshHotWordsForCurrentUser() {
        Integer userId = com.project.aminewsbackend.utils.UserThreadLocal.getUser().getId();
        Map<String, List<Long>> hotwords = getTopHotwordsWithItemsByCurrentUser();
        System.out.println("[HotWordService] 开始更新当前用户热词，userId=" + userId + "，热词数: " + hotwords.size());
        // 先查出该用户所有 hot_word 的 id
        List<HotWord> oldHotWords = hotWordMapper.selectList(new QueryWrapper<HotWord>().eq("user_id", userId));
        if (!oldHotWords.isEmpty()) {
            System.out.println("[HotWordService] 删除旧热词及关联项，userId=" + userId + "，旧热词数: " + oldHotWords.size());
            List<Integer> hotWordIds = oldHotWords.stream().map(HotWord::getId).toList();
            hotWordItemMapper.delete(new QueryWrapper<HotWordItem>().in("hot_word_id", hotWordIds));
        }
        // 再删 hot_word
        hotWordMapper.delete(new QueryWrapper<HotWord>().eq("user_id", userId));
        // 插入新数据
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, List<Long>> entry : hotwords.entrySet()) {
            HotWord hw = new HotWord();
            hw.setWord(entry.getKey());
            hw.setUserId(userId);
            hw.setCount(entry.getValue().size()); 
            hw.setCreateTime(now);
            hw.setUpdateTime(now);
            hotWordMapper.insert(hw);
            for (Long itemId : entry.getValue()) {
                HotWordItem hwi = new HotWordItem();
                hwi.setHotWordId(hw.getId());
                hwi.setItemId(itemId.intValue());
                hwi.setCreateTime(now);
                hwi.setUpdateTime(now);
                hotWordItemMapper.insert(hwi);
            }
        }
        System.out.println("[HotWordService] 插入新热词数据，userId=" + userId + "，新热词数: " + hotwords.size());
    }


    /**
     * 全量重算并存储所有用户的热词及其关联item
     */
    @Async
    @Transactional
    public void refreshHotWordsForAllUsers() {
        List<User> users = userMapper.selectList(null);
        List<Integer> userIds = users.stream()
                .map(User::getId)
                .toList();
        System.out.println("[HotWordService] 批量更新所有用户热词，用户数: " + userIds.size());
        if (userIds.isEmpty()) {
            System.out.println("[HotWordService] 没有用户需要更新热词");
            return;
        }
        for (Integer userId : userIds) {
            refreshHotWordsForUser(userId);
        }
        System.out.println("[HotWordService] 所有用户热词更新完成");
    }


    /**
     * 获取当前用户的热词列表（首页展示用）
     */
    public List<HotWordDTO> getCurrentUserHotWords() {
        Integer userId = UserThreadLocal.getUser().getId();
        System.out.println("[HotWordService] 查询当前用户热词，userId=" + userId);
        // 查询当前用户的热词，按count降序排列
        QueryWrapper<HotWord> hotWordQuery = new QueryWrapper<HotWord>()
                .eq("user_id", userId)
                .orderByDesc("count");
        List<HotWord> hotWords = hotWordMapper.selectList(hotWordQuery);

        List<HotWordDTO> result = new ArrayList<>();
        for (HotWord hotWord : hotWords) {
            HotWordDTO hotWordDTO = new HotWordDTO();
            hotWordDTO.setId(hotWord.getId());
            hotWordDTO.setWord(hotWord.getWord());
            hotWordDTO.setCount(hotWord.getCount());
            // 首页不需要加载具体的新闻详情，只显示热词和数量
            result.add(hotWordDTO);
        }
        System.out.println("[HotWordService] 查询到热词数量: " + result.size());
        return result;
    }

    /**
     * 根据热词ID获取对应的新闻详情
     */
    public List<ItemVo> getItemsByHotWordId(Integer hotWordId) {
        Integer userId = UserThreadLocal.getUser().getId();
        System.out.println("[HotWordService] 查询热词对应新闻，hotWordId=" + hotWordId + ", userId=" + userId);
        // 验证热词是否属于当前用户
        HotWord hotWord = hotWordMapper.selectById(hotWordId);
        if (hotWord == null || !hotWord.getUserId().equals(userId)) {
            System.out.println("[HotWordService] 热词不存在或不属于当前用户，hotWordId=" + hotWordId);
            return new ArrayList<>();
        }

        // 查询该热词关联的itemId
        QueryWrapper<HotWordItem> hwiQuery = new QueryWrapper<HotWordItem>()
                .eq("hot_word_id", hotWordId);
        List<HotWordItem> hotWordItems = hotWordItemMapper.selectList(hwiQuery);

        // 收集所有itemId
        List<Integer> itemIds = hotWordItems.stream()
                .map(HotWordItem::getItemId)
                .collect(Collectors.toList());
        
        // 使用优化后的连表查询同时获取title和icon
        Map<String, Map<Integer, String>> channelInfo = getChannelTitleAndIconsByItemIds(itemIds);
        Map<Integer, String> channelTitleMap = channelInfo.get("titles");
        Map<Integer, String> channelIconMap = channelInfo.get("icons");

        List<ItemVo> itemVos = new ArrayList<>();
        for (HotWordItem hwi : hotWordItems) {
            Item item = itemMapper.selectById(hwi.getItemId());
            if (item != null) {
                ItemVo itemVo = new ItemVo();
                itemVo.setId(item.getId());
                itemVo.setTitle(item.getTitle());
                itemVo.setLink(item.getLink());
                itemVo.setDescription(item.getDescription());
                itemVo.setAiDescription(item.getAiDescription());
                itemVo.setPubDate(item.getPubDate());
                itemVo.setImageUrl(itemService.extractFirstImageUrl(item.getDescription()));
                itemVo.setChannelTitle(channelTitleMap.get(item.getId()));
                itemVo.setChannelIcon(channelIconMap.get(item.getId()));
                itemVos.add(itemVo);
            }
        }
        // 按 pubDate 降序排序（最新的在前面）
        itemVos.sort((a, b) -> {
            if (a.getPubDate() == null && b.getPubDate() == null) return 0;
            if (a.getPubDate() == null) return 1;
            if (b.getPubDate() == null) return -1;
            return b.getPubDate().compareTo(a.getPubDate());
        });
        System.out.println("[HotWordService] 查询到新闻数量: " + itemVos.size());
        return itemVos;
    }
    
    /**
     * 同时获取多个item的channel title和icon - 优化版本使用连表查询
     */
    public Map<String, Map<Integer, String>> getChannelTitleAndIconsByItemIds(List<Integer> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // 将 itemIds 转换为逗号分隔的字符串
        String itemIdsStr = itemIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        
        // 使用连表查询一次性获取所有数据
        List<ItemChannelInfo> itemChannelInfos = itemMapper.getItemChannelInfoByIds(itemIdsStr);
        
        Map<Integer, String> titleMap = new HashMap<>();
        Map<Integer, String> iconMap = new HashMap<>();
        
        for (ItemChannelInfo info : itemChannelInfos) {
            titleMap.put(info.getItemId(), info.getChannelTitle());
            iconMap.put(info.getItemId(), info.getChannelIcon());
        }
        
        Map<String, Map<Integer, String>> result = new HashMap<>();
        result.put("titles", titleMap);
        result.put("icons", iconMap);
        
        return result;
    }
    
}
