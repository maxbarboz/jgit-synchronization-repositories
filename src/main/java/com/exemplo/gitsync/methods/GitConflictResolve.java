package com.exemplo.gitsync.methods;

import lombok.extern.log4j.Log4j2;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@Component
@Log4j2
public class GitConflictResolve {

    public void resolvendoConflitos(MergeResult mergeResult, Git git) throws IOException, GitAPIException {
        Map<String, int[][]> conflicts = mergeResult.getConflicts();

        if (conflicts != null) {
            for (String path : conflicts.keySet()) {
                log.info("Arquivo em conflito: {}", path);

                ObjectId objectId = getObjectIdForStage(git.getRepository(), path, 3);

                if (objectId != null) {
                    byte[] content = git.getRepository().open(objectId).getBytes();

                    File file = new File(git.getRepository().getWorkTree(), path);
                    Files.createDirectories(file.getParentFile().toPath());
                    Files.write(file.toPath(), content);

                    addFileToIndex(git.getRepository(), path, objectId);
                } else {
                    log.info("Não encontrou versão remota para {}", path);
                }
            }

            git.add().addFilepattern(".").call();
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

}
