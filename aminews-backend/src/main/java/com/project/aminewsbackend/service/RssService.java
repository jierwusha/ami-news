package com.project.aminewsbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.aminewsbackend.configuration.ConfigProperties;
import com.project.aminewsbackend.entity.Subscribe;
import com.project.aminewsbackend.entity.User;
import com.project.aminewsbackend.mapper.ChannelMapper;
import com.project.aminewsbackend.mapper.FolderChannelMapper;
import com.project.aminewsbackend.mapper.ItemMapper;
import com.project.aminewsbackend.mapper.SubscribeMapper;
import com.project.aminewsbackend.utils.Result;
import com.project.aminewsbackend.utils.UserThreadLocal;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.project.aminewsbackend.entity.Channel;
import com.project.aminewsbackend.entity.Item;

import java.io.File;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.time.ZoneId;

import java.net.URL;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class RssService {

    @Autowired
    private ChannelMapper channelMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private SubscribeMapper subscribeMapper;

    @Autowired
    private FolderService folderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConfigProperties configProperties;
    @Autowired
    private FolderChannelMapper folderChannelMapper;
    @Autowired
    private HotWordService hotWordService;

    @Autowired
    private NewsVectorService newsVectorService;
    @Autowired
    private VectorQueueManager vectorQueueManager;

    @Async
    public SyndFeed fetchRss(String feedUrl) throws Exception {
        // 设置超时时间为10秒，超时抛出ConnectException
        // 给feedUrl中的特殊字符进行编码
        if (feedUrl == null || feedUrl.isEmpty()) {
            throw new IllegalArgumentException("RSS feed URL cannot be null or empty");
        }
        // 只对中文和特殊字符进行编码
        feedUrl = java.net.URLEncoder.encode(feedUrl, "UTF-8")
                .replaceAll("\\+", "%20") // 将空格替换为 %20
                .replaceAll("%3A", ":") // 恢复冒号
                .replaceAll("%2F", "/"); // 恢复斜杠
        URL url = new URL(feedUrl);
        java.net.URLConnection connection = url.openConnection();
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(30_000);
        try (XmlReader reader = new XmlReader(connection)) {
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed =  input.build(reader);
            if (feed.getImage() == null && feed.getLink() != null) {
                URL linkUrl = new URL(feed.getLink());
                String faviconUrl = linkUrl.getProtocol() + "://" + linkUrl.getHost() + "/favicon.ico";
                HttpURLConnection conn = (HttpURLConnection) new URL(faviconUrl).openConnection();
                conn.setRequestMethod("HEAD");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                if (conn.getResponseCode() == 200) {
                    com.rometools.rome.feed.synd.SyndImage image = new com.rometools.rome.feed.synd.SyndImageImpl();
                    image.setUrl(faviconUrl);
                    image.setTitle(feed.getTitle());
                    // 直接调用 setImage
                    feed.setImage(image);
                }
            }
            return feed;
        } catch (java.net.SocketTimeoutException e) {
            System.out.println("[RssService] 连接超时: " + e.getMessage());
            throw new java.net.ConnectException("连接超时: " + e.getMessage());
        }
    }

    public Channel convertFeedToChannel(SyndFeed feed) {
        Channel channel = new Channel();
        channel.setTitle(feed.getTitle());
        channel.setLink(feed.getLink());
        channel.setDescription(feed.getDescription());
        channel.setAtomLink(feed.getUri());
        // 这里icon可根据feed.getImage()获取
        if (feed.getImage() != null) {
            channel.setIcon(feed.getImage().getUrl());
        }
        return channel;
    }

    public List<Item> convertFeedToItems(SyndFeed feed, Integer channelId) {
        List<Item> items = new ArrayList<>();
        if (feed.getEntries() != null) {
            for (SyndEntry entry : feed.getEntries()) {
                Item item = new Item();
                item.setChannelId(channelId);
                item.setTitle(entry.getTitle());
                item.setDescription(entry.getDescription() != null ? entry.getDescription().getValue() : null);
                item.setLink(entry.getLink());
                item.setPubDate(entry.getPublishedDate() != null ?
                        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(entry.getPublishedDate()) : String.valueOf(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));
                items.add(item);
            }
        }
        return items;
    }

    public Result addRss(String url) throws Exception {
        System.out.println("[RssService] 开始添加RSS源: " + url);
        // 1. 查看是否已存在相同的RSS源
        QueryWrapper<Channel> qw = new QueryWrapper<>();
        qw.eq("url", url);
        Channel existingChannel = channelMapper.selectOne(qw);
        if(existingChannel != null) {
            System.out.println("[RssService] RSS源已存在: " + url);
            return Result.success(existingChannel, "RSS源已存在，无需重复添加");
        }
        // 2. 如果不存在，解析RSS源,存入数据库
        SyndFeed feed = this.fetchRss(url);
        Channel channel = this.convertFeedToChannel(feed);
        channel.setUrl(url);
        channelMapper.insert(channel);
        System.out.println("[RssService] 新RSS源已插入数据库: " + channel.getTitle());
        Integer channelId = channel.getId();
        List<Item> items = this.convertFeedToItems(feed, channelId);
        itemMapper.insert(items);
        System.out.println("[RssService] 插入RSS源item数量: " + items.size());
        return Result.success(channel, "RSS源添加成功");
    }

    public Result subscribeRss(String url) throws Exception {
        System.out.println("[RssService] 用户订阅RSS源: " + url);
        // 0. 查看是否已经有订阅
        Subscribe existingSubscribe = subscribeMapper.getSubscribeByUrlAndUserId(url, UserThreadLocal.getUser().getId());
        if(existingSubscribe != null) {
            System.out.println("[RssService] 已订阅该RSS源: " + url);
            return Result.error(407, "已订阅该RSS源");
        }
        // 1. 查看是否已存在相同的RSS源
        QueryWrapper<Channel> qw = new QueryWrapper<>();
        qw.eq("url", url);
        Channel existingChannel = channelMapper.selectOne(qw);
        // 不存在，则添加rss源
        if(existingChannel == null) {
            System.out.println("[RssService] 该RSS源不存在，自动添加: " + url);
            SyndFeed feed = this.fetchRss(url);
            Channel channel = this.convertFeedToChannel(feed);
            channel.setUrl(url);
            channelMapper.insert(channel);
            folderService.addToRootFolder(channel.getId(), UserThreadLocal.getUser().getId());
            Integer channelId = channel.getId();
            List<Item> items = this.convertFeedToItems(feed, channelId);
            itemMapper.insert(items);
            // 将所有新item添加到向量化队列，而不是直接处理
            CompletableFuture.runAsync(() -> {
                try {
                    System.out.println("[RssService] 将 " + items.size() + " 条新item添加到向量化队列");
                    vectorQueueManager.addToQueue(items); // 添加所有items到队列
                } catch (Exception e) {
                    System.err.println("添加到向量化队列失败: " + e.getMessage());
                }
            });
            System.out.println("[RssService] 新增RSS源及item完成: " + channel.getTitle());
            existingChannel = channel;
        }
        // 修改subscribe
        Subscribe subscribe = new Subscribe();
        subscribe.setChannelId(existingChannel.getId());
        subscribe.setUserId(UserThreadLocal.getUser().getId());
        subscribeMapper.insert(subscribe);
        folderService.addToRootFolder(existingChannel.getId(), UserThreadLocal.getUser().getId());
        System.out.println("[RssService] 订阅成功: " + url);
        return Result.success("订阅成功");
    }

    public Result subscribeChannel(String channelId) throws Exception {
        System.out.println("[RssService] 用户订阅频道: " + channelId);
        // 0. 查看是否已经有订阅
        Subscribe existingSubscribe = subscribeMapper.selectOne(new QueryWrapper<Subscribe>()
                .eq("channel_id", channelId)
                .eq("user_id", UserThreadLocal.getUser().getId()));
        if(existingSubscribe != null) {
            System.out.println("[RssService] 已订阅该频道: " + channelId);
            return Result.error(407, "已订阅该频道");
        }
        // 1. 查看频道是否存在
        QueryWrapper<Channel> qw = new QueryWrapper<>();
        qw.eq("id", channelId);
        Channel existingChannel = channelMapper.selectOne(qw);
        if(existingChannel == null) {
            System.out.println("[RssService] 频道不存在: " + channelId);
            return Result.error(404, "频道不存在");
        }
        // 修改subscribe
        Subscribe subscribe = new Subscribe();
        subscribe.setChannelId(existingChannel.getId());
        subscribe.setUserId(UserThreadLocal.getUser().getId());
        subscribeMapper.insert(subscribe);
        folderService.addToRootFolder(existingChannel.getId(), UserThreadLocal.getUser().getId());
        System.out.println("[RssService] 订阅频道成功: " + channelId);
        return Result.success("订阅成功");
    }

    public Result unsubscribeRss(String url) {
        System.out.println("[RssService] 用户取消订阅RSS源: " + url);
        Subscribe subscribe = subscribeMapper.getSubscribeByUrlAndUserId(url, UserThreadLocal.getUser().getId());
        if (subscribe == null) {
            System.out.println("[RssService] 未订阅该RSS源: " + url);
            return Result.error(406, "未订阅该RSS源");
        }
        // 删除订阅记录
        subscribeMapper.deleteById(subscribe.getId());
        folderChannelMapper.removeFromRootFolder(subscribe.getChannelId(), UserThreadLocal.getUser().getId());
        folderChannelMapper.removeFromAllFolder(UserThreadLocal.getUser().getId(), subscribe.getChannelId());
        System.out.println("[RssService] 取消订阅成功: " + url);
        return Result.success("取消订阅成功");
    }

    @Async
    @Scheduled(cron = "0 0/10 * * * ?")
    //@Scheduled(cron = "0 * * * * ?")   // 每分钟执行一次 调试用
    public void updateSubscribeItems() {
        System.out.println("[RssService] --------------开始更新订阅的RSS源内容---------------");
        // 1. 获取所有订阅记录
        // 2. 遍历每个订阅记录，获取对应的RSS源
        // 3. 根据RSS源的item信息，更新数据库内容
        List<Channel> channels = channelMapper.getAllSubscribedChannels();
        for (Channel channel : channels) {
            try {
                SyndFeed feed;
                feed = this.fetchRss(channel.getUrl());
                System.out.println("[RssService] 正在更新订阅的RSS源: " + channel.getTitle());
                List<Item> items = this.convertFeedToItems(feed, channel.getId());

                // 4. 遍历items，插入或更新到数据库
                for (Item item : items) {
                    QueryWrapper<Item> qw = new QueryWrapper<>();
                    qw.eq("link", item.getLink());
                    Item existingItem = itemMapper.selectOne(qw);
                    if (existingItem == null) {
                        System.out.println("[RssService] 插入item: " + item.getTitle());
                        // 插入新item
                        itemMapper.insert(item);
                         // 异步处理新item的向量化
                        vectorQueueManager.addToQueue(item);
                    } else {
                        // 更新已存在的item
                        item.setId(existingItem.getId());
                        itemMapper.updateById(item);
                    }
                }

            } catch (Exception e) {
                System.err.println("[RssService] 更新订阅的RSS源内容失败: " + e.getMessage());
            }
        }
        System.out.println("[RssService] --------------更新订阅的RSS源内容完成---------------");
    }

    @Async
    public void addDefaultSubscribes(User user) {
        try {
            UserThreadLocal.set(user);
            File file = new File(configProperties.getDefaultSubscribeListPath());
            System.out.println("[RssService] 正在添加默认订阅列表: " + file.getAbsolutePath());
            JsonNode root = objectMapper.readTree(file);
            for (JsonNode folderNode : root) {
                String folderName = folderNode.get("folder").asText();
                Result folderResult = folderService.createFolder(folderName);
                if (folderResult.getCode() != 200) continue;
                Integer folderId = ((com.project.aminewsbackend.entity.Folder)folderResult.getData()).getId();
                java.util.stream.StreamSupport.stream(folderNode.get("urls").spliterator(), true)
                        .forEach(urlNode -> {
                            String url = urlNode.asText();
                            try {
                                Result subResult = this.subscribeRss(url);
                                if (subResult.getCode() == 200) {
                                    Channel channel = channelMapper.selectOne(
                                            new QueryWrapper<Channel>().eq("url", url)
                                    );
                                    if (channel != null) {
                                        folderService.addChannelToFolder(folderId, channel.getId());
                                    }
                                }
                            } catch (Exception ignore) {}
                        });
            }
        } catch (Exception e) {
            System.err.println("[RssService] 添加默认订阅失败: " + e.getMessage());
        } finally {
            User currentUser = UserThreadLocal.getUser(); // 获取当前用户
            if (currentUser != null) {
                hotWordService.refreshHotWordsForUser(currentUser.getId());// 传递用户参数
            }
            UserThreadLocal.remove();
        }
    }

    public Result unsubscribeChannel(String channelId) {
        Subscribe subscribe = subscribeMapper.selectOne(new QueryWrapper<Subscribe>()
                .eq("channel_id", channelId)
                .eq("user_id", UserThreadLocal.getUser().getId()));
        if (subscribe == null) {
            return Result.error(406, "未订阅该RSS源");
        }
        // 删除订阅记录
        subscribeMapper.deleteById(subscribe.getId());
        folderChannelMapper.removeFromRootFolder(subscribe.getChannelId(), UserThreadLocal.getUser().getId());
        folderChannelMapper.removeFromAllFolder(UserThreadLocal.getUser().getId(), subscribe.getChannelId());

        return Result.success("取消订阅成功");
    }
}
