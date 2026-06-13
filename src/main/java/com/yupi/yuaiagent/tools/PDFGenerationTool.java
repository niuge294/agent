package com.yupi.yuaiagent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.yupi.yuaiagent.constant.FileConstant;
import com.yupi.yuaiagent.context.UserContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF 生成工具 — 支持文本 + 嵌入图片
 */
public class PDFGenerationTool {

    // 匹配 ![alt](url) 或 ![alt](url)
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^]]*)]\\(([^)]+)\\)");
    private static final int MAX_IMAGE_WIDTH = 450;

    @Tool(description = "Generate a PDF file with given content (supports markdown images)")
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            userId = 0L;
        }
        String fileDir = FileConstant.FILE_SAVE_DIR + "/" + userId + "/pdf";
        String filePath = fileDir + "/" + fileName;
        try {
            FileUtil.mkdir(fileDir);
            String fontPath = new ClassPathResource("fonts/simfang.ttf").getFile().getAbsolutePath();
            PdfFont font = PdfFontFactory.createFont(fontPath,
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);

            // 解析内容：切分文本段落和图片
            List<Object> segments = parseContent(content);

            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {

                for (Object seg : segments) {
                    if (seg instanceof String text) {
                        if (!text.isBlank()) {
                            Paragraph p = new Paragraph(text).setFont(font);
                            document.add(p);
                        }
                    } else if (seg instanceof Image img) {
                        // 图片居中
                        img.setHorizontalAlignment(HorizontalAlignment.CENTER);
                        document.add(img);
                    }
                }
            }
            return "PDF generated successfully to: " + filePath;
        } catch (Exception e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }

    /**
     * 解析内容，将 markdown 图片替换为 Image 对象
     */
    private List<Object> parseContent(String content) throws IOException {
        List<Object> segments = new ArrayList<>();
        Matcher matcher = IMAGE_PATTERN.matcher(content);
        int lastEnd = 0;

        while (matcher.find()) {
            // 图片前面的文本
            String textBefore = content.substring(lastEnd, matcher.start());
            if (!textBefore.isBlank()) {
                segments.add(textBefore);
            }

            // 图片：下载并嵌入
            String alt = matcher.group(1);
            String url = matcher.group(2);
            try {
                // 下载图片，30 秒超时
                byte[] imageBytes = HttpUtil.createGet(url).timeout(30000).execute().bodyBytes();
                byte[] pdfBytes = imageBytes;
                // WebP 等 iText 不支持的格式，先转 PNG
                String lowerUrl = url.toLowerCase();
                if (lowerUrl.endsWith(".webp") || lowerUrl.contains(".webp?")) {
                    BufferedImage bi = ImageIO.read(new ByteArrayInputStream(imageBytes));
                    if (bi != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bi, "png", baos);
                        pdfBytes = baos.toByteArray();
                    }
                }
                Image image = new Image(ImageDataFactory.create(pdfBytes));
                image.setAutoScale(true);
                if (image.getImageWidth() > MAX_IMAGE_WIDTH) {
                    image.scaleToFit(MAX_IMAGE_WIDTH, Float.MAX_VALUE);
                }
                segments.add(image);
            } catch (Exception e) {
                segments.add("[图片下载失败: " + url + " — " + e.getMessage() + "]");
            }

            lastEnd = matcher.end();
        }

        // 剩余文本
        if (lastEnd < content.length()) {
            segments.add(content.substring(lastEnd));
        }

        return segments;
    }
}
