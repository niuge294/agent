package com.yupi.yuaiagent.controller;

import cn.hutool.core.io.FileUtil;
import com.yupi.yuaiagent.constant.FileConstant;
import com.yupi.yuaiagent.context.UserContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/file")
public class FileController {

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    //写的是用户上传的原始文件
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            userId = 0L;
        }
        Map<String, Object> result = new HashMap<>();
        try {
            String dir = FileConstant.FILE_SAVE_DIR + "/" + userId + "/file";
            FileUtil.mkdir(dir);
            String filePath = dir + "/" + file.getOriginalFilename();
            FileUtil.writeBytes(file.getBytes(), filePath);
            result.put("success", true);
            result.put("fileName", file.getOriginalFilename());
            result.put("fileSize", file.getSize());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
}
