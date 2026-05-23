package com.project.aminewsbackend.controller;

import com.project.aminewsbackend.entity.Folder;
import com.project.aminewsbackend.entity.FolderChannel;
import com.project.aminewsbackend.service.FolderService;
import com.project.aminewsbackend.utils.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/folder")
public class FolderController {

    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    @PostMapping("/create")
    public Result createFolder(@RequestParam String name) {
        return folderService.createFolder(name);
    }

    @PostMapping("/add")
    public Result addChannelToFolder(@RequestParam Integer folderId, @RequestParam Integer channelId) {
        return folderService.addChannelToFolder(folderId, channelId);
    }

    @PostMapping("/remove")
    public Result removeChannelFromFolder(@RequestParam Integer folderId, @RequestParam Integer channelId) {
        return folderService.removeChannelFromFolder(folderId, channelId);
    }

    @PostMapping("/delete")
    public Result deleteFolder(@RequestParam Integer folderId) {
        return folderService.deleteFolder(folderId);
    }

    @GetMapping("/new")
    public Result getNewFolders() {
        return folderService.getNewFolders();
    }

    @GetMapping("/items")
    public Result getItemsByFolderId(@RequestParam Integer folderId, @RequestParam Integer pageNum, @RequestParam Integer pageSize) {
        return folderService.getItemsByFolderId(folderId, pageNum, pageSize);
    }
}
