package com.exemplo.gitsync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GitHubRepoRequest {

    private String name;
    private String description;
    private boolean _private;
    private boolean auto_init;

}
