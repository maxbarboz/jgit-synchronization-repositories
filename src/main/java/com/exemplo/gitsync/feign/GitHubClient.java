package com.exemplo.gitsync.feign;

import com.exemplo.gitsync.dto.PullRequestRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "github-client", url = "${github.api-url}")
public interface GitHubClient {

    @PostMapping("/repos/{owner}/{repo}/pulls")
    Object criarPullRequest(@PathVariable("owner") String owner, @PathVariable("repo") String repo, @RequestHeader("Authorization") String authorization,
            @RequestHeader("Accept") String accept, @RequestBody PullRequestRequest request
    );

}
