package com.exemplo.gitsync.feign;

import com.exemplo.gitsync.dto.GitHubRepoRequest;
import com.exemplo.gitsync.dto.GitHubRepoResponse;
import com.exemplo.gitsync.dto.PullRequestRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "github-client", url = "${github.api-url}")
public interface GitHubClient {

    @PostMapping("/repos/{owner}/{repo}/pulls")
    Object criarPullRequest(
            @PathVariable("owner") String owner,
            @PathVariable("repo") String repo,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("Accept") String accept,
            @RequestBody PullRequestRequest request
    );

    @PostMapping("/user/repos")
    GitHubRepoResponse createRepository(
            @RequestHeader("Authorization") String authorization,
            @RequestBody GitHubRepoRequest request
    );

    @GetMapping("/repos/{owner}/{repo}")
    ResponseEntity<String> checkRepositoryExists(
            @PathVariable("owner") String owner,
            @PathVariable("repo") String repo,
            @RequestHeader("Authorization") String authHeader
    );

}
