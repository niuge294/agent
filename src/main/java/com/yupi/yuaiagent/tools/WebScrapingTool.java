package com.yupi.yuaiagent.tools;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 网页抓取工具
 */
public class WebScrapingTool {

    // ① 反爬：模拟真实浏览器 User-Agent
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    // ② 超时：连接 + 读取总超时
    private static final int TIMEOUT_MS = 15_000;
    // ② 重试：失败后最多重试次数
    private static final int MAX_RETRIES = 2;
    // ③ 截断：body 纯文本最大保留字符数
    private static final int MAX_BODY_LENGTH = 50_000;

    @Tool(description = "Scrape the content of a web page and return clean text")
    public String scrapeWebPage(
            @ToolParam(description = "URL of the web page to scrape") String url,
            @ToolParam(description = "Cookie string for authentication (optional)", required = false) String cookie) {
        String err = ToolParamValidator.validateUrl(url);
        if (err != null) return err;
        int retries = 0;
        Exception lastException = null;

        while (retries <= MAX_RETRIES) {
            try {
                Connection conn = Jsoup.connect(url)
                        // ① 反爬：Chrome UA + 完整请求头，模拟浏览器行为
                        .userAgent(USER_AGENT)
                        // ② 超时
                        .timeout(TIMEOUT_MS)
                        // ② 跟踪重定向
                        .followRedirects(true)
                        // ③ 防止下载超大文件
                        .maxBodySize(5 * 1024 * 1024)
                        // ① 反爬：标准 HTTP 请求头
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                        .header("Accept-Encoding", "gzip, deflate");

                // ⑤ Cookie 认证：登录态穿透
                if (cookie != null && !cookie.isEmpty()) {
                    conn.header("Cookie", cookie);
                }

                Document document = conn.get();

                // ④ 纯文本提取：标题 + body.text() 代替 html()
                StringBuilder sb = new StringBuilder();
                String title = document.title();
                if (!title.isEmpty()) {
                    sb.append(title).append("\n\n");
                }
                String bodyText = document.body() != null ? document.body().text() : document.text();
                if (bodyText.length() > MAX_BODY_LENGTH) {
                    bodyText = bodyText.substring(0, MAX_BODY_LENGTH) + "\n\n[...已截断，原文 " + bodyText.length() + " 字符]";
                }
                sb.append(bodyText);
                return sb.toString();

            } catch (Exception e) {
                lastException = e;
                retries++;
                // ② 重试：递增间隔，给服务器喘息时间
                if (retries <= MAX_RETRIES) {
                    try { Thread.sleep(1000L * retries); } catch (InterruptedException ignored) {}
                }
            }
        }
        return "网页抓取失败（重试 " + MAX_RETRIES + " 次后）: " + lastException.getMessage();
    }
}
