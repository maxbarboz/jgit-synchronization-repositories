package com.exemplo.gitsync;

import com.exemplo.gitsync.config.GitProperties;
import com.exemplo.gitsync.dto.PullRequestRequest;
import com.exemplo.gitsync.feign.GitHubClient;
import com.exemplo.gitsync.methods.GitMethods;
import lombok.extern.log4j.Log4j2;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Log4j2
public class GitSyncWithPR implements CommandLineRunner {

    static final String BRANCH = "main";
    static final File TEMP_DIR = new File("repos");

    private final GitProperties gitProperties;
    private final GitHubClient client;

    @Autowired
    public GitSyncWithPR(GitProperties gitProperties, GitHubClient client) {
        this.gitProperties = gitProperties;
        this.client = client;
    }

    @Override
    public void run(String... args) throws GitAPIException, URISyntaxException, IOException {
        final UsernamePasswordCredentialsProvider creds = new UsernamePasswordCredentialsProvider(gitProperties.getUser(), gitProperties.getToken());

        TEMP_DIR.mkdirs();

        File baseDir = new File(TEMP_DIR, "repo-base");
        File destDir = new File(TEMP_DIR, "repo-destino");

        log.info("\n");
        log.info("============================== INICIANDO MIGRAÇÃO DE CÓDIGO ==============================\n");
        log.info("STAGE 1 - Clonando repositório base");
        Git repoBase = GitMethods.clone(gitProperties.getRepoBase(), baseDir, BRANCH, creds);

        log.info("STAGE 2 - Clonando repositório destino");
        Git repoDestino = GitMethods.clone(gitProperties.getRepoDestino(), destDir, BRANCH, creds);

        log.info("STAGE 3 - Adicionando remote 'upstream'");
        GitMethods.remoteAdd(repoDestino, repoBase);

        log.info("STAGE 4 - Fazendo fetch de upstream");
        GitMethods.fetch(repoDestino, "upstream", creds);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
        String updateBranch = "update-from-base-" + timestamp;

        GitMethods.checkout(repoDestino, BRANCH, updateBranch);

        log.info("STAGE 5 - Fazendo merge na branch " + updateBranch);
        Ref upstream = repoDestino.getRepository().findRef("refs/remotes/upstream/" + BRANCH);

        if (GitMethods.merge(repoDestino, upstream)) {
            log.info("STAGE 6 - Merge realizado com sucesso - Enviando branch de atualização");
            GitMethods.commitAndPush(repoDestino, timestamp, updateBranch, creds);

            log.info("STAGE 7 - Abrindo Pull Request, por favor aguarde.");
            abrirPullRequest(updateBranch);
        } else {
            log.info("Merge falhou ou gerou conflitos. Verificar manualmente.");
        }

        repoBase.close();
        repoDestino.close();
    }

    public void abrirPullRequest(String branchName) {
        PullRequestRequest request = PullRequestRequest.builder()
                .title("Atualização do projeto base (" + branchName + ")")
                .head(branchName)
                .base(BRANCH)
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
}

