package com.chriscarini.jetbrains.gitpushreminder.messages;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;


public final class PluginMessages {
  @NonNls
  private static final String BUNDLE = "messages.gitPushReminder";

  @NonNls
  private static final ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE);

  public static String get(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    String value = bundle.getString(key);

    if (params.length > 0) {
      return MessageFormat.format(value, params);
    }

    return value;
  }
}
