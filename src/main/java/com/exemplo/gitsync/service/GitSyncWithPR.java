package com.exemplo.gitsync.service;

import com.exemplo.gitsync.config.GitProperties;
import com.exemplo.gitsync.dto.PullRequestRequest;
import com.exemplo.gitsync.feign.GitHubClient;
import com.exemplo.gitsync.methods.GitHubMethods;
import com.exemplo.gitsync.methods.GitMethods;
import lombok.extern.log4j.Log4j2;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@Log4j2
public class GitSyncWithPR implements CommandLineRunner {

    static final String BRANCH = "main";
    static final File TEMP_DIR = new File("repos");

    private final GitProperties gitProperties;
    private final GitHubMethods gitHubMethods;

    @Autowired
    public GitSyncWithPR(GitProperties gitProperties, GitHubMethods gitHubMethods) {
        this.gitProperties = gitProperties;
        this.gitHubMethods = gitHubMethods;
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

        log.info("STAGE 5 - Fazendo merge na branch " + updateBranch);
        Ref upstream = repoDestino.getRepository().findRef("refs/remotes/upstream/" + BRANCH);

        mergeAndValidacaoConflitos(repoDestino, upstream, timestamp, updateBranch, creds);

        repoBase.close();
        repoDestino.close();
    }

    private void mergeAndValidacaoConflitos(Git repoDestino, Ref upstream, String timestamp, String updateBranch,
                                            UsernamePasswordCredentialsProvider creds) throws GitAPIException, IOException {
        MergeResult mergeResult = GitMethods.merge(repoDestino, upstream);

        if (mergeResult.getMergeStatus().isSuccessful()) {
            log.info("STAGE 6 - Merge realizado com sucesso - Enviando branch de atualização");
            GitMethods.commitAndPush(repoDestino, timestamp, updateBranch, creds);

            log.info("STAGE 7 - Abrindo Pull Request, por favor aguarde.");
            gitHubMethods.abrirPullRequest(updateBranch, BRANCH, gitProperties);
        } else if(mergeResult.getMergeStatus() == MergeResult.MergeStatus.CONFLICTING) {
            resolvendoConflitos(mergeResult, repoDestino);
        } else {
            log.info("Merge falhou - Verificar manualmente.");
        }
    }

    private void resolvendoConflitos(MergeResult mergeResult, Git git) throws IOException, GitAPIException {
        System.out.println("Conflitos detectados:");
        Map<String, int[][]> conflicts = mergeResult.getConflicts();

        if (conflicts != null) {
            for (String path : conflicts.keySet()) {
                System.out.println("Arquivo em conflito: " + path);

                // 🔥 Resolver mantendo versão REMOTA (stage 3)
                ObjectId objectId = getObjectIdForStage(git.getRepository(), path, 3);

                if (objectId != null) {
                    byte[] content = git.getRepository().open(objectId).getBytes();

                    File file = new File(git.getRepository().getWorkTree(), path);
                    Files.createDirectories(file.getParentFile().toPath());
                    Files.write(file.toPath(), content);

                    // Adiciona ao index
                    addFileToIndex(git.getRepository(), path, objectId);
                } else {
                    System.out.println("Não encontrou versão remota para " + path);
                }
            }

            // Commit do merge resolvido
            git.commit()
                    .setMessage("Merge resolvido automaticamente — mantendo versão remota")
                    .call();

            System.out.println("Conflitos resolvidos e merge commit realizado.");
        }
    }

    private static ObjectId getObjectIdForStage(Repository repository, String path, int stage) throws IOException {
        DirCache dirCache = repository.readDirCache();
        int entryCount = dirCache.getEntryCount();
        for (int i = 0; i < entryCount; i++) {
            DirCacheEntry entry = dirCache.getEntry(i);
            if (entry.getPathString().equals(path) && entry.getStage() == stage) {
                return entry.getObjectId();
            }
        }
        return null;
    }

    private static void addFileToIndex(Repository repository, String path, ObjectId objectId) throws IOException {
        DirCache dirCache = repository.lockDirCache();
        DirCacheEditor editor = dirCache.editor();

        editor.add(new DirCacheEditor.PathEdit(path) {
            @Override
            public void apply(DirCacheEntry ent) {
                ent.setObjectId(objectId);
                ent.setFileMode(FileMode.REGULAR_FILE);
            }
        });

        editor.commit();
        dirCache.unlock();
    }

    private Git cloneRepositorioDestino(File destDir, UsernamePasswordCredentialsProvider creds) throws Exception {
        if(!gitHubMethods.checkRepositoryExists(gitProperties)) {
            gitHubMethods.createRepository(gitProperties);
        }

        return GitMethods.clone(gitProperties.getRepoDestino(), destDir, BRANCH, creds);
    }

}

