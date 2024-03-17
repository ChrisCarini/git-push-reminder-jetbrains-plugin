package com.chriscarini.jetbrains.gitpushreminder;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.chriscarini.jetbrains.gitpushreminder.GitHelper.RepositoryAndBranch;
import com.chriscarini.jetbrains.gitpushreminder.messages.PluginMessages;
import com.chriscarini.jetbrains.gitpushreminder.settings.SettingsManager;
import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.BranchChangeListener;

import git4idea.push.GitPushSource;

public class GitBranchChangeListener implements BranchChangeListener {

    private final Project project;

    public GitBranchChangeListener(Project project) {
        this.project = project;
    }

    @Override
    public void branchWillChange(@NotNull String branchName) {
        if (!SettingsManager.getInstance().getState().showSwitchDialog) {
            return;
        }

        List<RepositoryAndBranch> branchesWithUnpushedCommits = GitHelper.getBranchesWithUnpushedCommits(project, false);
        boolean showMessage = !branchesWithUnpushedCommits.isEmpty();

        if (showMessage) {
            RepositoryAndBranch repositoryAndBranch = branchesWithUnpushedCommits.get(0);
            final int dialogResult = Messages.showOkCancelDialog(project,
                    PluginMessages.get("git.push.reminder.switching.dialog.body.unpushed.branches",
                            repositoryAndBranch.branch().getName()
                    ),
                    PluginMessages.get("git.push.reminder.closing.dialog.title", ""),
                    PluginMessages.get("git.push.reminder.switching.dialog.button.push"),
                    PluginMessages.get("git.push.reminder.switching.dialog.button.dontpush"),
                    Messages.getWarningIcon()
            );

            if (dialogResult == 0) {
                new VcsPushDialog(project,
                        List.of(repositoryAndBranch.repository()),
                        List.of(repositoryAndBranch.repository()),
                        repositoryAndBranch.repository(),
                        GitPushSource.create(repositoryAndBranch.branch())
                ).show();
            }
        }
    }

    @Override
    public void branchHasChanged(@NotNull String branchName) {
        // Nothing needs to be done
    }
}
