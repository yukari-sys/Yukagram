package com.github.yukagram.ui.main_controller;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.github.yukagram.YuSettings;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.telegram.TdlibCounter;
import org.thunderdog.challegram.ui.MainController;
import org.thunderdog.challegram.util.text.Counter;
import org.thunderdog.challegram.util.text.TextColorSet;

import me.vkryl.core.lambda.FutureBool;

public abstract class AbstractMainController<T> extends ViewController<T> {

    public AbstractMainController(@NonNull Context context, Tdlib tdlib) {
        super(context, tdlib);
    }

    // hell nah
    public enum MainControllerVersion { V1, V2 }

    public static MainControllerVersion getCurrentVersion() {
        return YuSettings.Toggle.NewDialogsView.is() ? MainControllerVersion.V2 : MainControllerVersion.V1;
    }

    public static AbstractMainController<?> create(Context context, Tdlib tdlib) {
        return switch (getCurrentVersion()) {
            case V1 -> new MainController(context, tdlib);
            case V2 -> new MainControllerV2(context, tdlib);
        };
    }

    public static void updateCounter(Tdlib tdlib, TdApi.ChatList chatList, Counter unreadCounter, TdlibCounter counter, boolean b) {
        switch (getCurrentVersion()) {
            case V1 -> MainController.updateCounter(tdlib, chatList, unreadCounter, counter, b);
            case V2 -> MainControllerV2.updateCounter(tdlib, chatList, unreadCounter, counter, b);
        }
    }

    public abstract void shareIntent(Tdlib intentTdlib, String intentAction, Intent intent);

    public abstract ViewController<?> getPreparedControllerForPosition(int i);

    public abstract boolean showComposeWrap(ViewController<?> chatsController);

    public interface UnreadCounterColorSet extends TextColorSet {

        static UnreadCounterColorSet create(FutureBool aFalse) {
            return switch (getCurrentVersion()) {
                case V1 -> new MainController.UnreadCounterColorSet(aFalse);
                case V2 -> new MainControllerV2.UnreadCounterColorSet(aFalse);
            };
        }

        void setCounter(Counter unreadCounter);
    }
}
