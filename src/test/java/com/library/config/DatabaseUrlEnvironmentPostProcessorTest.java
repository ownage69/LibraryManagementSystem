package com.library.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    void toJdbcPropertiesShouldConvertRenderPostgresUrl() {
        Map<String, Object> properties = DatabaseUrlEnvironmentPostProcessor.toJdbcProperties(
                "postgresql://library_user:secret@dpg-example:5432/library"
        );

        assertThat(properties)
                .containsEntry(
                        "spring.datasource.url",
                        "jdbc:postgresql://dpg-example:5432/library"
                )
                .containsEntry("spring.datasource.username", "library_user")
                .containsEntry("spring.datasource.password", "secret");
    }

    @Test
    void toJdbcPropertiesShouldDecodeCredentials() {
        Map<String, Object> properties = DatabaseUrlEnvironmentPostProcessor.toJdbcProperties(
                "postgres://user%40mail:p%40ss%3Aword@localhost/library"
        );

        assertThat(properties)
                .containsEntry("spring.datasource.username", "user@mail")
                .containsEntry("spring.datasource.password", "p@ss:word");
    }
}
