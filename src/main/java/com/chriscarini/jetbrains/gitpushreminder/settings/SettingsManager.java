package com.chriscarini.jetbrains.gitpushreminder.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;


/**
 * The {@link SettingsManager} for this plugin; settings will be stored out to and read from {@code gitPushReminder.xml}.
 */
@State(name = "gitPushReminder", storages = @Storage(value = "gitPushReminder.xml", roamingType = RoamingType.PER_OS))
public class SettingsManager implements PersistentStateComponent<SettingsManager.GitPushReminderSettingsState> {
    private GitPushReminderSettingsState myState;

    public static SettingsManager getInstance() {
        return ApplicationManager.getApplication().getService(SettingsManager.class);
    }

    @NotNull
    @Override
    public GitPushReminderSettingsState getState() {
        if (myState == null) {
            myState = new GitPushReminderSettingsState();
        }
        return myState;
    }

    @Override
    public void loadState(@NotNull final GitPushReminderSettingsState gitPushReminderSettingsState) {
        myState = gitPushReminderSettingsState;
    }

    public static class GitPushReminderSettingsState {

        public boolean allowUncommitedChanges;
        public boolean allowUntrackedBranches;
        public boolean allowUntrackedFiles;
        public boolean checkAllBranches;
        public boolean showDialog;
        public boolean showSwitchDialog;

        public GitPushReminderSettingsState() {
            this.allowUncommitedChanges = false;
            this.allowUntrackedBranches = false;
            this.allowUntrackedFiles = false;
            this.checkAllBranches = false;
            this.showDialog = true;
            this.showSwitchDialog = false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GitPushReminderSettingsState that = (GitPushReminderSettingsState) o;
            return allowUncommitedChanges == that.allowUncommitedChanges &&
                    allowUntrackedBranches == that.allowUntrackedBranches &&
                    allowUntrackedFiles == that.allowUntrackedFiles &&
                    checkAllBranches == that.checkAllBranches &&
                    showDialog == that.showDialog &&
                    showSwitchDialog == that.showSwitchDialog;
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    allowUncommitedChanges,
                    allowUntrackedBranches,
                    allowUntrackedFiles,
                    checkAllBranches,
                    showDialog,
                    showSwitchDialog
            );
        }
    }
}
