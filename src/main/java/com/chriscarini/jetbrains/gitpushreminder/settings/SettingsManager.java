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

        public boolean checkAllBranches;
        public boolean countUntrackedBranchAsPushed;
        public boolean showDialog;
        public boolean preventClose;

        public GitPushReminderSettingsState() {
            this.checkAllBranches = false;
            this.countUntrackedBranchAsPushed = false;
            this.showDialog = true;
            this.preventClose = false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GitPushReminderSettingsState that = (GitPushReminderSettingsState) o;
            return checkAllBranches == that.checkAllBranches && countUntrackedBranchAsPushed == that.countUntrackedBranchAsPushed && showDialog == that.showDialog && preventClose == that.preventClose;
        }

        @Override
        public int hashCode() {
            return Objects.hash(checkAllBranches, countUntrackedBranchAsPushed, showDialog, preventClose);
        }
    }
}
