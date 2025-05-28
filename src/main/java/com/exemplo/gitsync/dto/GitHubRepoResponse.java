package com.exemplo.gitsync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GitHubRepoResponse {

    private Long id;
    private String name;
    private String full_name;
    private String html_url;

}
