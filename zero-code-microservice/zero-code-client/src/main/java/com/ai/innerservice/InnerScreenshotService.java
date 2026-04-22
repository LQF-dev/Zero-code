package com.ai.innerservice;

/**
 * 内部截图服务
 */
public interface InnerScreenshotService {

    String generateAndUploadScreenshot(String webUrl);
}