package com.resume.agent;

import com.resume.agent.config.AgentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableConfigurationProperties(AgentProperties.class)
public class KnowledgeAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeAgentApplication.class, args);
    }
}
