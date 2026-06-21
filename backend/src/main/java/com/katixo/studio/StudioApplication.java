package com.katixo.studio;

import com.katixo.ai.config.AiProperties;
import com.katixo.studio.config.KatixoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Single monolith: Katixo Studio (image/video/design + Copilot) AND the doc-AI
 * extraction/reconciliation pipeline ({@code com.katixo.ai.*}) in one application — for personal,
 * single-user use where only one or two things run at a time.
 *
 * <p>Component scanning, plus JPA entity and repository scanning, all span both feature roots:
 * Studio lives under {@code com.katixo.studio} and the doc-AI pipeline under {@code com.katixo.ai}.
 * (Spring Boot would otherwise default entity/repository scanning to this class's own package only.)
 */
@SpringBootApplication(scanBasePackages = {"com.katixo.studio", "com.katixo.ai"})
@EntityScan(basePackages = {"com.katixo.studio", "com.katixo.ai"})
@EnableJpaRepositories(basePackages = {"com.katixo.studio", "com.katixo.ai"})
@EnableConfigurationProperties({KatixoProperties.class, AiProperties.class})
public class StudioApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudioApplication.class, args);
    }
}
