package com.yupi.yuaiagent.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网页搜索工具
 */
public class WebSearchTool {

    // SearchAPI 的搜索接口地址
    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";

    private final String apiKey;

    public WebSearchTool(String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "Search for information from Baidu Search Engine")
    public String searchWeb(
            @ToolParam(description = "Search query keyword") String query) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query);
        paramMap.put("api_key", apiKey);
        paramMap.put("engine", "baidu");
        try {
            String response = HttpUtil.get(SEARCH_API_URL, paramMap);
            // 取出返回结果的前 5 条
            JSONObject jsonObject = JSONUtil.parseObj(response);
            // 提取 organic_results 部分
            JSONArray organicResults = jsonObject.getJSONArray("organic_results");
            List<Object> objects = organicResults.subList(0, 5);
            // 拼接搜索结果为字符串
            String result = objects.stream().map(obj -> {
                JSONObject tmpJSONObject = (JSONObject) obj;
                return tmpJSONObject.toString();
            }).collect(Collectors.joining(","));
            return result;
        } catch (Exception e) {
            return "Error searching Baidu: " + e.getMessage();
        }
    }

    /**
     * 搜索图片
     */
    @Tool(description = "Search for images from Google Images, returns image URLs")
    public String searchImages(@ToolParam(description = "Image search query keyword") String query) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query);
        paramMap.put("api_key", apiKey);
        paramMap.put("engine", "google_images");
        try {
            String response = HttpUtil.get(SEARCH_API_URL, paramMap);
            JSONObject jsonObject = JSONUtil.parseObj(response);
            JSONArray images = jsonObject.getJSONArray("images");
            if (images == null || images.isEmpty()) {
                return "未找到相关图片";
            }
            // 取前 5 张，转成 markdown 图片格式
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(5, images.size()); i++) {
                JSONObject img = images.getJSONObject(i);
                JSONObject original = img.getJSONObject("original");
                String title = img.getStr("title", "");
                String url = original != null ? original.getStr("link", "") : "";
                if (!url.isEmpty()) {
                    sb.append("![").append(title).append("](").append(url).append(")\n");
                }
            }
            return sb.length() > 0 ? sb.toString() : "未找到相关图片";
        } catch (Exception e) {
            return "图片搜索失败：" + e.getMessage();
        }
    }
}
