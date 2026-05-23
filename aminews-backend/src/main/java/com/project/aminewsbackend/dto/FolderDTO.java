package com.project.aminewsbackend.dto;

import com.project.aminewsbackend.entity.Channel;
import lombok.Data;

import java.util.List;

@Data
public class FolderDTO {
    private Integer id;
    private String name;
    private List<ChannelDTO> channels;
}
