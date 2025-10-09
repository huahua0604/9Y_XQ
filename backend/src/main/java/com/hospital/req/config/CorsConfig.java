package com.hospital.req.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig {
    @Value("${app.allow-origins:http://localhost:5173}")
    private String allowOriginsCsv;

    /** 统一创建 CORS 配置对象 */
    private CorsConfiguration buildCfg() {
        CorsConfiguration cfg = new CorsConfiguration();

        List<String> items = Arrays.stream(allowOriginsCsv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());

        // 当 allowCredentials=true 时，不允许使用通配符 * 作为具体 AllowedOrigin
        boolean usePatterns = items.stream().anyMatch(s -> s.contains("*"));
        if (usePatterns) {
            items.forEach(cfg::addAllowedOriginPattern);
        } else {
            items.forEach(cfg::addAllowedOrigin);
        }

        cfg.setAllowCredentials(true); // 若纯 JWT 无 Cookie，也可以设为 false
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS","HEAD"));
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","Accept","X-Requested-With","Origin"));
        cfg.setExposedHeaders(List.of("Content-Disposition","Authorization","X-Total-Count"));
        cfg.setMaxAge(3600L); // 预检缓存（秒）

        return cfg;
    }

    /** 提供给 Spring Security 的 CorsConfigurationSource */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", buildCfg());
        return source;
    }

    /** 顶层 CorsFilter：保证异常/未命中过滤器链时也能写出 CORS 头 */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", buildCfg());
        return new CorsFilter(source);
    }
}

