package com.medicare.config;

import com.medicare.DesktopServerBridge;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class DesktopServerReadyListener implements ApplicationListener<WebServerInitializedEvent> {

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        DesktopServerBridge.setPort(event.getWebServer().getPort());
    }
}


