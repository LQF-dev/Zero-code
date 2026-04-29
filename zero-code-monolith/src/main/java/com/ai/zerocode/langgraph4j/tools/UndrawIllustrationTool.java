package com.ai.zerocode.langgraph4j.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import com.ai.zerocode.langgraph4j.model.ImageResource;
import com.ai.zerocode.langgraph4j.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 图片收集工具（插画图片）
 */
@Slf4j
@Component
public class UndrawIllustrationTool {

    private static final String UNDRAW_SEARCH_URL = "https://undraw.co/search?term=%s";
    private static final String UNDRAW_DATA_API_URL = "https://undraw.co/_next/data/%s/search/%s.json?term=%s";
    private static final Pattern BUILD_ID_PATTERN = Pattern.compile("\"buildId\":\"([^\"]+)\"");

    @Tool("搜索插画图片，用于网站美化和装饰")
    public List<ImageResource> searchIllustrations(@P("搜索关键词") String query) {
        List<ImageResource> imageList = new ArrayList<>();
        if (StrUtil.isBlank(query)) {
            return imageList;
        }
        int searchCount = 12;
        String encodedQuery = encodePathSegment(query);
        String buildId = fetchBuildId(encodedQuery);
        if (StrUtil.isBlank(buildId)) {
            log.warn("未能获取 unDraw buildId，query={}", query);
            return imageList;
        }
        String apiUrl = String.format(UNDRAW_DATA_API_URL, buildId, encodedQuery, encodedQuery);

        // 使用 try-with-resources 自动释放 HTTP 资源
        try (HttpResponse response = HttpRequest.get(apiUrl).timeout(10000).execute()) {
            if (!response.isOk()) {
                log.warn("unDraw 数据接口请求失败，status={}, url={}", response.getStatus(), apiUrl);
                return imageList;
            }
            JSONObject result = JSONUtil.parseObj(response.body());
            JSONObject pageProps = result.getJSONObject("pageProps");
            if (pageProps == null) {
                return imageList;
            }
            JSONArray initialResults = pageProps.getJSONArray("initialResults");
            if (initialResults == null || initialResults.isEmpty()) {
                return imageList;
            }
            int actualCount = Math.min(searchCount, initialResults.size());
            for (int i = 0; i < actualCount; i++) {
                JSONObject illustration = initialResults.getJSONObject(i);
                String title = illustration.getStr("title", "插画");
                String media = illustration.getStr("media", "");
                if (StrUtil.isNotBlank(media)) {
                    imageList.add(ImageResource.builder()
                            .category(ImageCategoryEnum.ILLUSTRATION)
                            .description(title)
                            .url(media)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("搜索插画失败：{}", e.getMessage(), e);
        }
        return imageList;
    }

    private String fetchBuildId(String encodedQuery) {
        String searchUrl = String.format(UNDRAW_SEARCH_URL, encodedQuery);
        try (HttpResponse response = HttpRequest.get(searchUrl).timeout(10000).execute()) {
            if (!response.isOk()) {
                log.warn("unDraw 搜索页请求失败，status={}, url={}", response.getStatus(), searchUrl);
                return null;
            }
            String body = response.body();
            Matcher matcher = BUILD_ID_PATTERN.matcher(body);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return null;
        } catch (Exception e) {
            log.error("获取 unDraw buildId 失败：{}", e.getMessage(), e);
            return null;
        }
    }

    private String encodePathSegment(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
