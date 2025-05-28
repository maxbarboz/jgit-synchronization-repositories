package com.exemplo.gitsync.methods;

import com.exemplo.gitsync.config.GitProperties;
import com.exemplo.gitsync.dto.GitHubRepoRequest;
import com.exemplo.gitsync.dto.GitHubRepoResponse;
import com.exemplo.gitsync.dto.PullRequestRequest;
import com.exemplo.gitsync.feign.GitHubClient;
import com.exemplo.gitsync.utils.Util;
import feign.FeignException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class GitHubMethods {

    private final GitHubClient client;

    @Autowired
    public GitHubMethods(GitHubClient client) {
        this.client = client;
    }

    public void abrirPullRequest(String branchName, String branch, GitProperties gitProperties) {
        PullRequestRequest request = PullRequestRequest.builder()
                .title("Atualização do projeto base (" + branchName + ")")
                .head(branchName)
                .base(branch)
                .body("Merge automático das atualizações do projeto base. Por favor, revise e aceite.")
                .build();

        try {
            Object response = client.criarPullRequest(gitProperties.getOwner(), gitProperties.getRepoName(),
                    "token " + gitProperties.getToken(), "application/vnd.github+json", request);
            log.info("FINALIZADO - Pull Request criado com sucesso: {}", response);
            log.info("\n");
            log.info("==========================================================================================\n");
        } catch (Exception e) {
            log.error("Falha ao criar Pull Request", e);
        }
    }

    public String createRepository(GitProperties gitProperties) {
        GitHubRepoRequest request = new GitHubRepoRequest(
                gitProperties.getRepoName(),
                "Repositório criado via API com Feign",
                false,
                true
        );

        return client.createRepository(Util.generateBearerToken(gitProperties.getToken()), request).getHtml_url();
    }

    public Boolean checkRepositoryExists(GitProperties gitProperties) throws Exception {
        try {
            ResponseEntity<String> response = client.checkRepositoryExists(gitProperties.getOwner(),
                    gitProperties.getRepoName(), Util.generateBearerToken(gitProperties.getToken()));

            return response.getStatusCode().equals(HttpStatus.OK) ? Boolean.TRUE : repoInexistente(response.getStatusCode().value());
        } catch (FeignException ex) {
            return repoInexistente(ex.status());
        }
    }

    private Boolean repoInexistente(int statusCode) throws Exception {
        if(statusCode == HttpStatus.NOT_FOUND.value()) {
            log.debug("Repositório não existe, seguindo para a criação");
            return Boolean.FALSE;
        }

        throw new Exception();
    }
}
