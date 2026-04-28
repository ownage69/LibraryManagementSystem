package com.library.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "databaseUrlProperties";
    private static final String DATABASE_URL = "DATABASE_URL";
    private static final String JDBC_DATABASE_URL = "JDBC_DATABASE_URL";
    private static final String SPRING_DATASOURCE_URL = "SPRING_DATASOURCE_URL";

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application
    ) {
        if (hasExplicitJdbcUrl(environment)) {
            return;
        }

        String databaseUrl = environment.getProperty(DATABASE_URL);
        if (!StringUtils.hasText(databaseUrl)) {
            return;
        }

        environment.getPropertySources()
                .addFirst(new MapPropertySource(
                        PROPERTY_SOURCE_NAME,
                        toJdbcProperties(databaseUrl)
                ));
    }

    static Map<String, Object> toJdbcProperties(String databaseUrl) {
        URI uri = URI.create(databaseUrl);
        String scheme = uri.getScheme();

        if (!"postgres".equals(scheme) && !"postgresql".equals(scheme)) {
            throw new IllegalArgumentException(
                    "DATABASE_URL must use postgres:// or postgresql:// scheme"
            );
        }

        if (!StringUtils.hasText(uri.getHost()) || !StringUtils.hasText(uri.getPath())) {
            throw new IllegalArgumentException("DATABASE_URL must include host and database");
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", toJdbcUrl(uri));
        addCredentials(uri, properties);
        return properties;
    }

    private boolean hasExplicitJdbcUrl(ConfigurableEnvironment environment) {
        return StringUtils.hasText(environment.getProperty(SPRING_DATASOURCE_URL))
                || StringUtils.hasText(environment.getProperty(JDBC_DATABASE_URL))
                || StringUtils.hasText(System.getProperty("spring.datasource.url"));
    }

    private static String toJdbcUrl(URI uri) {
        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                .append(uri.getHost());

        if (uri.getPort() > 0) {
            jdbcUrl.append(':').append(uri.getPort());
        }

        jdbcUrl.append(uri.getPath());

        if (StringUtils.hasText(uri.getQuery())) {
            jdbcUrl.append('?').append(uri.getQuery());
        }

        return jdbcUrl.toString();
    }

    private static void addCredentials(URI uri, Map<String, Object> properties) {
        String userInfo = uri.getUserInfo();
        if (!StringUtils.hasText(userInfo)) {
            return;
        }

        int separatorIndex = userInfo.indexOf(':');
        if (separatorIndex < 0) {
            properties.put("spring.datasource.username", decode(userInfo));
            return;
        }

        properties.put(
                "spring.datasource.username",
                decode(userInfo.substring(0, separatorIndex))
        );
        properties.put(
                "spring.datasource.password",
                decode(userInfo.substring(separatorIndex + 1))
        );
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
