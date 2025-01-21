package com.chriscarini.jetbrains.gitpushreminder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.intellij.ide.SaveAndSyncHandler;
import com.intellij.openapi.progress.util.BackgroundTaskUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.VcsManagedFilesHolder;
import com.intellij.util.Time;
import git4idea.index.GitFileStatus;
import git4idea.repo.GitRepositoryManager;
import git4idea.status.GitStagingAreaHolder;
import org.jetbrains.annotations.NotNull;

import com.chriscarini.jetbrains.gitpushreminder.messages.PluginMessages;
import com.chriscarini.jetbrains.gitpushreminder.settings.SettingsManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseHandler;
import com.intellij.openapi.ui.MessageConstants;
import com.intellij.openapi.ui.Messages;

public class GitPushReminder implements ProjectCloseHandler {
    private static final int TIME_BETWEEN = 2 * Time.SECOND;
    private long lastRanTime = System.currentTimeMillis() - TIME_BETWEEN - 1;
    private final List<String> body = new ArrayList<>();

    /**
     * We can close the project if/when all the following criteria are met:
     * <p>
     * 0. If "Show dialog?" IS UNCHECKED --> TRUE
     * 1. "Allow uncommitted changes" is UNCHECKED &&& There are uncommitted changes --> Show Dialog
     * 2. "Allow untracked branches" is UNCHECKED &&& There are untracked branches --> Show Dialog
     * 3. "Allow untracked files" is UNCHECKED &&& There are untracked files --> Show Dialog
     * 4. Otherwise, Show Dialog (if needed) and do what the user wants, else TRUE
     *
     * @param project project to check
     * @return true if the project can be closed, false otherwise
     */
    @Override
    public boolean canClose(@NotNull Project project) {
        // Sometimes upon program exit (not project close), two dialogs are shown. 
        // `lastRanTime` is a mechanism to prevent that from happening.
        if (System.currentTimeMillis() - lastRanTime < TIME_BETWEEN) {
            return true;
        }

        /*
         *  - Show dialog?
         *      - If checked, a dialog will be shown if there are any un-pushed branches, untracked files, or uncommitted changes.
         *      - If unchecked, no dialog box will be shown and the project will be allowed to close without notice.
         *
         *  0. If "Show dialog?" IS UNCHECKED --> TRUE
         */
        if (!SettingsManager.getInstance().getState().showDialog) {
            return true;
        }

        // Clear any previous body of the dialog on each run
        body.clear();

        // Try to update the state of repos
        SaveAndSyncHandler.getInstance().refreshOpenFiles();
        BackgroundTaskUtil.syncPublisher(project, VcsManagedFilesHolder.TOPIC).updatingModeChanged();
        GitRepositoryManager.getInstance(project).getRepositories().forEach(gitRepository -> {
            BackgroundTaskUtil.syncPublisher(project, GitStagingAreaHolder.TOPIC).stagingAreaChanged(gitRepository);
        });

        /*
         *  - Allow uncommitted changes
         *      - If checked, uncommited changes in changelists will be 'allowed'.
         *      - If unchecked, a dialog will be shown warning that uncommitted changes will need to be committed.
         *
         *  1. If "Allow uncommitted changes" is UNCHECKED &&& There are uncommitted changes --> Show Dialog
         */
        if (!SettingsManager.getInstance().getState().allowUncommittedChanges) {
            final List<GitFileStatus> untrackedCommittedFiles = GitHelper.getFilesWithUncommittedChanges(project);
            addToDialogBody(
                    untrackedCommittedFiles,
                    PluginMessages.get("git.push.reminder.closing.dialog.body.uncommitted.changes.title"),
                    (gitFileStatus) -> gitFileStatus.getPath().getName()
            );
        }

        /*
         *  - Allow untracked branches
         *      - If checked, local branches with no tracked remote branch will be 'allowed'.
         *      - If unchecked, a dialog will be shown warning that local branches will need to have a tracked remote branch.
         *
         *  2. If "Allow untracked branches" is UNCHECKED &&& There are untracked branches --> Show Dialog
         */
        if (!SettingsManager.getInstance().getState().allowUntrackedBranches) {
            final List<GitHelper.RepositoryAndBranch> untrackedBranches = GitHelper.getBranchesWithUnpushedCommits(project, SettingsManager.getInstance().getState().checkAllBranches);
            addToDialogBody(
                    untrackedBranches,
                    PluginMessages.get("git.push.reminder.closing.dialog.body.untracked.branches.title"),
                    repoAndBranch -> String.format("%s (%s)", repoAndBranch.branch().getName(), repoAndBranch.repository().getRoot().getName()) //NON-NLS
            );
        }

        /*
         *  - Allow untracked files
         *      - If checked, untracked files which are not added to a changelist will be 'allowed'.
         *      - If unchecked, a dialog will be shown warning that untracked files will need to become tracked.
         *
         *  3. If "Allow untracked files" is UNCHECKED &&& There are untracked files --> Show Dialog
         */
        if (!SettingsManager.getInstance().getState().allowUntrackedFiles) {
            final List<FilePath> untrackedFiles = GitHelper.getUntrackedFiles(project);
            addToDialogBody(
                    untrackedFiles,
                    PluginMessages.get("git.push.reminder.closing.dialog.body.untracked.files.title"),
                    FilePath::getName
            );
        }

        /*
         *  4. Otherwise,
         *      - if the user opted to "Keep Project Open" in the dialog -> FALSE
         *      - Otherwise, TRUE
         */
        boolean okToClose = body.isEmpty() || doesUserWantProjectClosed(project, body);

        // Note: `lastRanTime` needs to be updated *AFTER* the dialog is shown.
        lastRanTime = System.currentTimeMillis();

        return okToClose;
    }

    private <T> void addToDialogBody(
            final List<T> checkList,
            final String title,
            final Function<T, String> mapper
    ) {
        if (!checkList.isEmpty()) {
            final String listItems = checkList.stream()
                    .map(mapper)
                    .sorted()
                    .collect(Collectors.joining("</li><li>"));
            this.body.add(PluginMessages.get("git.push.reminder.closing.dialog.body.list.template", title, listItems));
        }
    }

    private boolean doesUserWantProjectClosed(@NotNull final Project project, @NotNull final List<String> body) {
        final int dialogResult = Messages.showOkCancelDialog(project,
                PluginMessages.get("git.push.reminder.closing.dialog.body.template", String.join("", body)),
                PluginMessages.get("git.push.reminder.closing.dialog.title"),
                PluginMessages.get("git.push.reminder.closing.dialog.button.close.anyway"),
                PluginMessages.get("git.push.reminder.closing.dialog.button.keep.project.open"),
                Messages.getWarningIcon()
        );

        return dialogResult == MessageConstants.OK;
    }
}
