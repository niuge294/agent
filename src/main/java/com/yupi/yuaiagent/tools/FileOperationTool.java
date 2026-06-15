package com.yupi.yuaiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.yupi.yuaiagent.constant.FileConstant;
import com.yupi.yuaiagent.context.UserContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 文件操作工具类（按用户隔离存储）
 */
public class FileOperationTool {

    /**
     * 获取当前用户的文件目录
     */
    private String getUserFileDir() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            userId = 0L;
        }
        return FileConstant.FILE_SAVE_DIR + "/" + userId + "/file";
    }

    /**
     * 列出当前用户已上传的文件
     */
    @Tool(description = "List all uploaded files available to read")
    public String listFiles() {
        String dirPath = getUserFileDir();
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return "暂无已上传的文件";
        }
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return "暂无已上传的文件";
        }
        return Arrays.stream(files)
                .filter(File::isFile)
                .map(f -> f.getName() + " (" + FileUtil.readableFileSize(f) + ")")
                .collect(Collectors.joining("\n"));
    }

    /**
     * 读取文件内容
     */
    @Tool(description = "Read content from a file")
    public String readFile(@ToolParam(description = "Name of a file to read") String fileName) {
        String err = ToolParamValidator.validateFileName(fileName);
        if (err != null) return err;
        String filePath = getUserFileDir() + "/" + fileName;
        if (!FileUtil.exist(filePath)) {
            return "文件不存在：" + fileName + "，可用文件列表：" + listFiles();
        }
        try {
            return FileUtil.readUtf8String(filePath);
        } catch (Exception e) {
            return "读取文件失败：" + e.getMessage();
        }
    }

    /**
     * 将内容写入文件
     */
    //写的是 LLM 生成的文本内容
    @Tool(description = "Write content to a file")
    public String writeFile(@ToolParam(description = "Name of the file to write") String fileName,
                            @ToolParam(description = "Content to write to the file") String content) {
        String err = ToolParamValidator.validateFileName(fileName);
        if (err != null) return err;
        err = ToolParamValidator.validateContent(content);
        if (err != null) return err;
        String dirPath = getUserFileDir();
        String filePath = dirPath + "/" + fileName;
        try {
            FileUtil.mkdir(dirPath);
            FileUtil.writeUtf8String(content, filePath);
            return "文件已写入：" + filePath;
        } catch (Exception e) {
            return "写入文件失败：" + e.getMessage();
        }
    }
}
