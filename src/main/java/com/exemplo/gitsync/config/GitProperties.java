package com.exemplo.gitsync.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ToString
@Configuration
@ConfigurationProperties(prefix = "git")
public class GitProperties {

    private String repoBase;
    private String repoDestino;
    private String owner;
    private String repoName;
    private String branch;
    private String user;
    private String token;

}