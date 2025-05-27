package com.exemplo.gitsync.config;

import lombok.Getter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@ToString
@Component
public class GitProperties {

    @Value("${git.repo.base}")
    private String repoBase;

    @Value("${git.repo.destino}")
    private String repoDestino;

    @Value("${git.owner}")
    private String owner;

    @Value("${git.repoName}")
    private String repoName;

    @Value("${git.branch}")
    private String branch;

    @Value("${git.user}")
    private String user;

    @Value("${git.token}")
    private String token;

}
