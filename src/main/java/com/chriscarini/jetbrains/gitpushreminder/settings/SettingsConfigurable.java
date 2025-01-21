package com.chriscarini.jetbrains.gitpushreminder.settings;

import com.chriscarini.jetbrains.gitpushreminder.messages.PluginMessages;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * A {@link Configurable} for Git Push Reminder plugin.
 */
public class SettingsConfigurable implements Configurable {

    private final JPanel mainPanel = new JBPanel<>();

    private final JBCheckBox checkAllBranchesField = new JBCheckBox();
    private final JBCheckBox allowUncommittedChangesField = new JBCheckBox();
    private final JBCheckBox allowUntrackedBranchField = new JBCheckBox();
    private final JBCheckBox allowUntrackedFilesField = new JBCheckBox();
    private final JBCheckBox showDialogField = new JBCheckBox();
    private final JBCheckBox showSwitchDialogField = new JBCheckBox();

    public SettingsConfigurable() {
        buildMainPanel();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return PluginMessages.get("git.push.reminder.settings.display.name");
    }

    private void buildMainPanel() {
        mainPanel.setLayout(new VerticalFlowLayout(true, false));

        mainPanel.add(
            FormBuilder.createFormBuilder()
                .addLabeledComponent(PluginMessages.get("git.push.reminder.settings.check.all.branches.label"), checkAllBranchesField)
                .addTooltip(PluginMessages.get("git.push.reminder.settings.check.all.branches.tooltip"))
                .addSeparator()
                .addLabeledComponent(PluginMessages.get("git.push.reminder.settings.allow.uncommitted.changes.label"), allowUncommittedChangesField)
                .addTooltip(PluginMessages.get("git.push.reminder.settings.allow.uncommitted.changes.tooltip"))
                .addSeparator()
                .addLabeledComponent(PluginMessages.get("git.push.reminder.settings.allow.untracked.branches.label"), allowUntrackedBranchField)
                .addTooltip(PluginMessages.get("git.push.reminder.settings.allow.untracked.branches.tooltip"))
                .addSeparator()
                .addLabeledComponent(PluginMessages.get("git.push.reminder.settings.allow.untracked.files.label"), allowUntrackedFilesField)
                .addTooltip(PluginMessages.get("git.push.reminder.settings.allow.untracked.files.tooltip"))
                .addSeparator()
                .addLabeledComponent(PluginMessages.get("git.push.reminder.settings.show.dialog.label"), showDialogField)
                .addTooltip(PluginMessages.get("git.push.reminder.settings.show.dialog.tooltip"))
                .addSeparator()
                .addLabeledComponent(PluginMessages.get("git.push.reminder.settings.show.switch.dialog.label"), showSwitchDialogField)
                .addTooltip(PluginMessages.get("git.push.reminder.settings.show.switch.dialog.tooltip"))
                .getPanel()
        );
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        // Set the user input field to contain the currently saved settings
        setUserInputFieldsFromSavedSettings();

        return mainPanel;
    }

    @Override
    public boolean isModified() {
        return !getSettingsFromUserInput().equals(getSettings());
    }

    /**
     * Apply the settings; saves the current user input list to the {@link SettingsManager}, and updates the table.
     */
    @Override
    public void apply() {
        final SettingsManager.GitPushReminderSettingsState settingsState = getSettingsFromUserInput();
        SettingsManager.getInstance().loadState(settingsState);
        setUserInputFieldsFromSavedSettings();
    }


    @NotNull
    private SettingsManager.GitPushReminderSettingsState getSettingsFromUserInput() {
        final SettingsManager.GitPushReminderSettingsState settingsState = new SettingsManager.GitPushReminderSettingsState();

        settingsState.checkAllBranches = checkAllBranchesField.isSelected();
        settingsState.allowUncommittedChanges = allowUncommittedChangesField.isSelected();
        settingsState.allowUntrackedBranches = allowUntrackedBranchField.isSelected();
        settingsState.allowUntrackedFiles = allowUntrackedFilesField.isSelected();
        settingsState.showDialog = showDialogField.isSelected();
        settingsState.showSwitchDialog = showSwitchDialogField.isSelected();

        return settingsState;
    }

    /**
     * Get the saved settings and update the user input field.
     */
    private void setUserInputFieldsFromSavedSettings() {
        updateUserInputFields(getSettings());
    }

    /**
     * Update the user input field based on the input value provided by {@code val}
     *
     * @param settings The {@link SettingsManager.GitPushReminderSettingsState} for the plugin.
     */
    private void updateUserInputFields(@Nullable final SettingsManager.GitPushReminderSettingsState settings) {
        if (settings == null) {
            return;
        }

        checkAllBranchesField.setSelected(settings.checkAllBranches);
        allowUncommittedChangesField.setSelected(settings.allowUncommittedChanges);
        allowUntrackedBranchField.setSelected(settings.allowUntrackedBranches);
        allowUntrackedFilesField.setSelected(settings.allowUntrackedFiles);
        showDialogField.setSelected(settings.showDialog);
        showSwitchDialogField.setSelected(settings.showSwitchDialog);
    }

    @NotNull
    private SettingsManager.GitPushReminderSettingsState getSettings() {
        return SettingsManager.getInstance().getState();
    }
}
