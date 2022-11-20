package com.chriscarini.jetbrains.gitpushreminder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.chriscarini.jetbrains.gitpushreminder.settings.SettingsManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import git4idea.GitLocalBranch;
import git4idea.GitRemoteBranch;
import git4idea.branch.GitBranchIncomingOutgoingManager;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

public class GitHelper {

    private static final Logger LOG = Logger.getInstance(GitHelper.class);

    static List<RepositoryAndBranch> getBranchesWithUnpushedCommits(Project project, boolean checkAllBranches) {
        return GitRepositoryManager.getInstance(project)
                                   .getRepositories().stream()
                                   .map(gitRepository -> {
                                       if (checkAllBranches) {
                                           return checkAllBranches(project, gitRepository);
                                       }
                                       return checkCurrentBranchOnly(project, gitRepository);
                                   }).flatMap(Collection::stream)
                                   .collect(Collectors.toList());
    }

    @NotNull
    private static Set<RepositoryAndBranch> checkAllBranches(
            @NotNull final Project project,
            final GitRepository gitRepository
    ) {
        final Set<RepositoryAndBranch> allBranchesWithUnpushedCommits = gitRepository.getBranches().getLocalBranches()
                                                                                .stream()
                                                                                .filter(gitLocalBranch -> isLocalBranchWithUnpushedCommits(project, gitRepository, gitLocalBranch))
                                                                                .map(branch -> new RepositoryAndBranch(gitRepository, branch))
                                                                                .collect(Collectors.toSet());

        LOG.debug("allBranchesWithUnpushedCommits: %s", allBranchesWithUnpushedCommits); //NON-NLS

        return allBranchesWithUnpushedCommits;
    }

    @NotNull
    private static Set<RepositoryAndBranch> checkCurrentBranchOnly(
            @NotNull final Project project,
            @NotNull final GitRepository gitRepository
    ) {
        final GitLocalBranch currentBranch = gitRepository.getCurrentBranch();

        // If there is no current branch, it is safe to close (idk why this would happen, though)
        if (currentBranch == null) {
            return Collections.emptySet();
        }

        return isLocalBranchWithUnpushedCommits(project, gitRepository, currentBranch) ? Set.of(new RepositoryAndBranch(gitRepository, currentBranch)) : Collections.emptySet();
    }

    @NotNull
    private static Boolean isLocalBranchWithUnpushedCommits(
            @NotNull final Project project,
            @NotNull final GitRepository gitRepository,
            @NotNull final GitLocalBranch currentBranch
    ) {
        final GitRemoteBranch trackedBranch = currentBranch.findTrackedBranch(gitRepository);
        if (trackedBranch == null) {
            return !SettingsManager.getInstance().getState().countUntrackedBranchAsPushed;
        }

        final String currentBranchName = currentBranch.getName();

        final boolean outgoingCommits = GitBranchIncomingOutgoingManager.getInstance(project).hasOutgoingFor(gitRepository, currentBranchName);

        LOG.info(String.format(
                "Project: %s  -  Outgoing Commits: %5s  -  Current Branch: %s", //NON-NLS
                project.getName(),
                outgoingCommits,
                currentBranchName
        ));

        return outgoingCommits;
    }

    public record RepositoryAndBranch(GitRepository repository, GitLocalBranch branch) {
    }
}
