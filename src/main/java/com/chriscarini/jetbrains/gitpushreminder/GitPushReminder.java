package com.chriscarini.jetbrains.gitpushreminder;

import com.chriscarini.jetbrains.gitpushreminder.messages.PluginMessages;
import com.chriscarini.jetbrains.gitpushreminder.settings.SettingsManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseHandler;
import com.intellij.openapi.ui.MessageConstants;
import com.intellij.openapi.ui.Messages;
import git4idea.GitLocalBranch;
import git4idea.GitReference;
import git4idea.GitRemoteBranch;
import git4idea.branch.GitBranchIncomingOutgoingManager;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GitPushReminder implements ProjectCloseHandler {

    private static final Logger LOG = Logger.getInstance(GitPushReminder.class);

    @Override
    public boolean canClose(@NotNull Project project) {
        final List<GitLocalBranch> branchesWithUnpushedCommits = GitRepositoryManager.getInstance(project)
            .getRepositories().stream()
            .map(gitRepository -> {
                if (SettingsManager.getInstance().getState().checkAllBranches) {
                    return checkAllBranches(project, gitRepository);
                }
                return checkCurrentBranchOnly(project, gitRepository);
            }).flatMap(Collection::stream)
            .collect(Collectors.toList());

        // If there are *NO* branches with outgoing/un-pushed commits, then we can close.
        boolean canClose = branchesWithUnpushedCommits.isEmpty();

        if (!canClose && SettingsManager.getInstance().getState().showDialog) {
            final int dialogResult = Messages.showOkCancelDialog(project,
                PluginMessages.get("git.push.reminder.closing.dialog.body.unpushed.branches",
                    branchesWithUnpushedCommits.stream()
                        .map(GitReference::getName)
                        .sorted()
                        .collect(Collectors.joining("</li><li>"))
                ),
                PluginMessages.get("git.push.reminder.closing.dialog.title"),
                PluginMessages.get("git.push.reminder.closing.dialog.button.close.anyway"),
                PluginMessages.get("git.push.reminder.closing.dialog.button.keep.project.open"),
                Messages.getWarningIcon()
            );

            return dialogResult == MessageConstants.OK;

        }

        return true;
    }

    @NotNull
    private Set<GitLocalBranch> checkAllBranches(
        @NotNull final Project project,
        final GitRepository gitRepository
    ) {
        final Set<GitLocalBranch> allBranchesWithUnpushedCommits = gitRepository.getBranches().getLocalBranches()
            .stream()
            .filter(gitLocalBranch -> isLocalBranchWithUnpushedCommits(project, gitRepository, gitLocalBranch))
            .collect(Collectors.toSet());

        LOG.debug("allBranchesWithUnpushedCommits: %s", allBranchesWithUnpushedCommits); //NON-NLS

        return allBranchesWithUnpushedCommits;
    }


    @NotNull
    private Set<GitLocalBranch> checkCurrentBranchOnly(
        @NotNull final Project project,
        @NotNull final GitRepository gitRepository
    ) {
        final GitLocalBranch currentBranch = gitRepository.getCurrentBranch();

        // If there is no current branch, it is safe to close (idk why this would happen, though)
        if (currentBranch == null) {
            return Collections.emptySet();
        }

        return isLocalBranchWithUnpushedCommits(project, gitRepository, currentBranch) ? Set.of(currentBranch) : Collections.emptySet();
    }

    @NotNull
    private Boolean isLocalBranchWithUnpushedCommits(
        @NotNull final Project project,
        @NotNull final GitRepository gitRepository,
        @NotNull final GitLocalBranch currentBranch
    ) {
        final GitRemoteBranch trackedBranch = currentBranch.findTrackedBranch(gitRepository);
        if (trackedBranch == null) {
            if (SettingsManager.getInstance().getState().countUntrackedBranchAsPushed) {
                return false;
            }
            return true;
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
}
