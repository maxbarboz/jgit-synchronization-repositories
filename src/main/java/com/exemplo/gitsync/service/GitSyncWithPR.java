package com.exemplo.gitsync.service;

import com.exemplo.gitsync.config.GitProperties;
import com.exemplo.gitsync.methods.GitConflictResolve;
import com.exemplo.gitsync.methods.GitHubMethods;
import com.exemplo.gitsync.methods.GitMethods;
import lombok.extern.log4j.Log4j2;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
@Log4j2
public class GitSyncWithPR implements CommandLineRunner {

    static final String BRANCH = "main";
    static final File TEMP_DIR = new File("repos");

    private final GitProperties gitProperties;
    private final GitHubMethods gitHubMethods;
    private final GitConflictResolve gitConflictResolve;

    @Autowired
    public GitSyncWithPR(GitProperties gitProperties, GitHubMethods gitHubMethods, GitConflictResolve gitConflictResolve) {
        this.gitProperties = gitProperties;
        this.gitHubMethods = gitHubMethods;
        this.gitConflictResolve = gitConflictResolve;
    }

    @Override
    public void run(String... args) throws Exception {
        final UsernamePasswordCredentialsProvider creds = new UsernamePasswordCredentialsProvider(gitProperties.getUser(), gitProperties.getToken());

        TEMP_DIR.mkdirs();

        File baseDir = new File(TEMP_DIR, "repo-base");
        File destDir = new File(TEMP_DIR, "repo-destino");

        log.info("\n");
        log.info("============================== INICIANDO MIGRAÇÃO DE CÓDIGO ==============================\n");
        log.info("STAGE 1 - Clonando repositório base");
        Git repoBase = GitMethods.clone(gitProperties.getRepoBase(), baseDir, BRANCH, creds);

        log.info("STAGE 2 - Clonando repositório destino");
        Git repoDestino = cloneRepositorioDestino(destDir, creds);

        log.info("STAGE 3 - Adicionando remote 'upstream'");
        GitMethods.remoteAdd(repoDestino, repoBase);

        log.info("STAGE 4 - Fazendo fetch de upstream");
        GitMethods.fetch(repoDestino, "upstream", creds);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
        String updateBranch = "update-from-base-" + timestamp;

        GitMethods.checkout(repoDestino, BRANCH, updateBranch);

        log.info("STAGE 5 - Fazendo merge na branch {}", updateBranch);
        Ref upstream = repoDestino.getRepository().findRef("refs/remotes/upstream/" + BRANCH);

        mergeAndValidacaoConflitos(repoDestino, upstream, updateBranch, creds);

        repoBase.close();
        repoDestino.close();
    }

    private void mergeAndValidacaoConflitos(Git repoDestino, Ref upstream, String updateBranch,
                                            UsernamePasswordCredentialsProvider creds) throws GitAPIException, IOException {
        String mensagem = "Merge realizado, não houve conflitos";
        MergeResult mergeResult = GitMethods.merge(repoDestino, upstream);

        if(mergeResult.getMergeStatus() == MergeResult.MergeStatus.CONFLICTING) {
            log.info("STAGE BÔNUS - Conflitos identificados, resolvendo antes de commitar.");
            gitConflictResolve.resolvendoConflitos(mergeResult, repoDestino);

            mensagem = "Merge resolvido automaticamente — mantendo versão remota";
        }

        log.info("STAGE 6 - Merge realizado com sucesso - Enviando branch de atualização");
        GitMethods.commitAndPush(repoDestino, mensagem, updateBranch, creds);

        log.info("STAGE 7 - Abrindo Pull Request, por favor aguarde.");
        gitHubMethods.abrirPullRequest(updateBranch, BRANCH, gitProperties);
    }

    private Git cloneRepositorioDestino(File destDir, UsernamePasswordCredentialsProvider creds) throws Exception {
        if(gitProperties.getRepoDestino().equals("repo-default") || !gitHubMethods.checkRepositoryExists(gitProperties)) {
            gitProperties.setRepoDestino(gitHubMethods.createRepository(gitProperties));
        }

        return GitMethods.clone(gitProperties.getRepoDestino(), destDir, BRANCH, creds);
    }

}

