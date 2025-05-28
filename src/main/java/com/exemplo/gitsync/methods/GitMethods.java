package com.exemplo.gitsync.methods;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URISyntaxException;

@Component
public class GitMethods {

    private GitMethods() {
        //vazio
    }

    public static Git clone(String url, File directory, String branch, UsernamePasswordCredentialsProvider creds) throws GitAPIException {
        return Git.cloneRepository()
                .setURI(url)
                .setDirectory(directory)
                .setBranch(branch)
                .setCredentialsProvider(creds)
                .call();
    }

    public static void fetch(Git repo, String remote, UsernamePasswordCredentialsProvider creds) throws GitAPIException {
        repo.fetch()
                .setRemote(remote)
                .setCredentialsProvider(creds)
                .call();
    }

    public static void checkout(Git repo, String branch, String updateBranch) throws GitAPIException {
        repo.checkout()
                .setCreateBranch(true)
                .setName(updateBranch)
                .setStartPoint("origin/" + branch)
                .call();
    }

    public static void remoteAdd(Git repo, Git repoBase) throws GitAPIException, URISyntaxException {
        repo.remoteAdd()
                .setName("upstream")
                .setUri(new URIish(repoBase.getRepository().getConfig().getString("remote", "origin", "url")))
                .call();
    }

    public static MergeResult merge(Git repo, Ref upstream) throws GitAPIException {
        return repo.merge().include(upstream).call();
    }

    public static void commitAndPush(Git repo, String timestamp, String updateBranch, UsernamePasswordCredentialsProvider creds) throws GitAPIException {
        repo.commit()
                .setMessage("Atualizando do repo base em " + timestamp)
                .call();

        repo.push()
                .setRemote("origin")
                .setCredentialsProvider(creds)
                .add(updateBranch)
                .call();
    }

}
