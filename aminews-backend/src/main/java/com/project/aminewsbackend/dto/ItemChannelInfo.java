package com.project.aminewsbackend.dto;

public class ItemChannelInfo {
    private Integer itemId;
    private String channelTitle;
    private String channelIcon;
    
    // getters and setters
    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }
    
    public String getChannelTitle() { return channelTitle; }
    public void setChannelTitle(String channelTitle) { this.channelTitle = channelTitle; }
    
    public String getChannelIcon() { return channelIcon; }
    public void setChannelIcon(String channelIcon) { this.channelIcon = channelIcon; }
}