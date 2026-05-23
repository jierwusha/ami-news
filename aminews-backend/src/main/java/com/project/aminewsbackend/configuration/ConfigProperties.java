package com.project.aminewsbackend.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@ConfigurationProperties(prefix = "app")
public class ConfigProperties {

    private String defaultSubscribeListPath;

    public String getDefaultSubscribeListPath() {
        // 如果是绝对路径，直接返回
        File file = new File(defaultSubscribeListPath);
        if (file.isAbsolute()) {
            return file.getAbsolutePath();
        }
        // 尝试从 classpath 获取
        java.net.URL resource = getClass().getClassLoader().getResource(defaultSubscribeListPath);
        if (resource != null) {
            return new File(resource.getFile()).getAbsolutePath();
        }
        // 否则拼接为项目根目录下的路径
        return new File(System.getProperty("user.dir"), defaultSubscribeListPath).getAbsolutePath();
    }

    public void setDefaultSubscribeListPath(String defaultSubscribeListPath) {
        this.defaultSubscribeListPath = defaultSubscribeListPath;
    }
}
