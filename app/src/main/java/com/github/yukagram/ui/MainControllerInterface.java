package com.github.yukagram.ui;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.github.yukagram.YuSettings;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.telegram.TdlibCounter;
import org.thunderdog.challegram.ui.MainController;
import org.thunderdog.challegram.util.text.Counter;
import org.thunderdog.challegram.util.text.TextColorSet;

import me.vkryl.core.lambda.FutureBool;

public interface MainControllerInterface {

    // hell nah
    enum MainControllerVersion { V1, V2 }

    static MainControllerVersion getCurrentVersion() {
        return YuSettings.Toggle.NewDialogsView.is() ? MainControllerVersion.V2 : MainControllerVersion.V1;
    }

    static MainControllerInterface create(Context context, Tdlib tdlib) {
        return switch (getCurrentVersion()) {
            case V1 -> new MainController(context, tdlib);
            case V2 -> new MainControllerV2(context, tdlib);
        };
    }

    static void updateCounter(Tdlib tdlib, TdApi.ChatList chatList, Counter unreadCounter, TdlibCounter counter, boolean b) {
        switch (getCurrentVersion()) {
            case V1 -> MainController.updateCounter(tdlib, chatList, unreadCounter, counter, b);
            case V2 -> MainControllerV2.updateCounter(tdlib, chatList, unreadCounter, counter, b);
        }
    }

    void shareIntent(Tdlib intentTdlib, String intentAction, Intent intent);

    ViewController<?> getPreparedControllerForPosition(int i);

    boolean showComposeWrap(ViewController<?> chatsController);

    interface UnreadCounterColorSet extends TextColorSet {

        static UnreadCounterColorSet create(FutureBool aFalse) {
            return switch (getCurrentVersion()) {
                case V1 -> new MainController.UnreadCounterColorSet(aFalse);
                case V2 -> new MainControllerV2.UnreadCounterColorSet(aFalse);
            };
        }

        void setCounter(Counter unreadCounter);
    }


    /* view controller shit here */
    boolean isFocused();
    boolean navigateTo(ViewController<?> c);
    View getValue();
}
