package com.coderunner;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class LanguageConfig {
    private Map<String, LanguageDetails> supportedLanguages;

    @Data
    public static class LanguageDetails {
        private String image;
        private String[] cmd;
        private String ext;
    }
}