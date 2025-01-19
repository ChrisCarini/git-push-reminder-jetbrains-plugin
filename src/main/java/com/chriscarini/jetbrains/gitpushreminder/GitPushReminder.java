package com.chriscarini.jetbrains.gitpushreminder;

import com.chriscarini.jetbrains.gitpushreminder.messages.PluginMessages;
import com.chriscarini.jetbrains.gitpushreminder.settings.SettingsManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseHandler;
import com.intellij.openapi.ui.MessageConstants;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class GitPushReminder implements ProjectCloseHandler {

    @Override
    public boolean canClose(@NotNull Project project) {
        if (SettingsManager.getInstance().getState().showDialogOnUnpushedCommits && doNotCloseOnUnpushedCommits(project)) {
            return false;
        }
        if (doNotCloseOnUntrackedFilesOrUncommitedChanges(project)) {
            return false;
        }

        return true;
    }

    private boolean doNotCloseOnUnpushedCommits(@NotNull Project project) {
        final List<GitHelper.RepositoryAndBranch> branchesWithUnpushedCommits = GitHelper.getBranchesWithUnpushedCommits(project, SettingsManager.getInstance().getState().checkAllBranches);

        if (branchesWithUnpushedCommits.isEmpty()) {
            // If there are *NO* branches with outgoing/un-pushed commits, then we can close.
            return false;
        }

        final int dialogResult = Messages.showOkCancelDialog(project,
            PluginMessages.get("git.push.reminder.closing.dialog.body.unpushed.branches",
                branchesWithUnpushedCommits.stream()
                    .sorted((o1, o2) -> {
                        if (o1.repository().getRoot().getName().equals(o2.repository().getRoot().getName())) {
                            return o1.branch().getName().compareTo(o2.branch().getName());
                        }
                        return o1.repository().getRoot().getName().compareTo(o2.repository().getRoot().getName());
                    })
                    .map(repoAndBranch -> String.format(
                            "%s (%s)", //NON-NLS
                            repoAndBranch.branch().getName(),
                            repoAndBranch.repository().getRoot().getName()
                        )
                    )
                    .collect(Collectors.joining("</li><li>"))
            ),
            PluginMessages.get("git.push.reminder.closing.dialog.title", branchesWithUnpushedCommits.size()),
            PluginMessages.get("git.push.reminder.closing.dialog.button.close.anyway"),
            PluginMessages.get("git.push.reminder.closing.dialog.button.keep.project.open"),
            Messages.getWarningIcon()
        );

        // On cancel keep the project open
        return dialogResult == MessageConstants.CANCEL;
    }

    private boolean doNotCloseOnUntrackedFilesOrUncommitedChanges(@NotNull Project project) {
        final int countUntrackedFiles = GitHelper.countUntrackedFiles(project);
        // Count the files with uncommitted changes is not entirely truth worthy. If open a project without any change,
        // make a modification and directly close the project, the change is ofter not detected. But if you reopen that
        // same project and close it again, the change is detected. Also, after an explicit save, the change is always
        // detected. So in daily use, this most likely will work good enough.
        final int countFilesWithUncommittedChanges = GitHelper.countFilesWithUncommittedChanges(project);
        if (countUntrackedFiles == 0 && countFilesWithUncommittedChanges == 0) {
            return false;
        }

        final int dialogResult = Messages.showOkCancelDialog(project,
            PluginMessages.get("git.push.reminder.closing.dialog.body.untracked-or-uncommitted.files"),
            PluginMessages.get("git.push.reminder.closing.dialog.title.untracked-or-uncommitted.files", countUntrackedFiles + countFilesWithUncommittedChanges),
            PluginMessages.get("git.push.reminder.closing.dialog.button.close.anyway"),
            PluginMessages.get("git.push.reminder.closing.dialog.button.keep.project.open"),
            Messages.getWarningIcon()
        );

        // On cancel keep the project open
        return dialogResult == MessageConstants.CANCEL;
    }
}
