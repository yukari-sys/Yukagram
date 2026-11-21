package com.github.yukagram.ui.emoji_keyboard;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.widget.TextView;

public class EmojiListController extends ViewController<EmojiKeyboardWrapped> {

    org.thunderdog.challegram.ui.EmojiListController thunderdogListController;

    public EmojiListController(@NonNull Context context, Tdlib tdlib, boolean onlyClassicEmoji) {
        thunderdogListController = new org.thunderdog.challegram.ui.EmojiListController(context, tdlib, onlyClassicEmoji);
        super(context, tdlib);
    }

    @Override
    public void setArguments(EmojiKeyboardWrapped args) {
        super.setArguments(args);

        thunderdogListController.setArguments(args.parent);
    }

    @Override
    protected View onCreateView(Context context) {
        return thunderdogListController.getValue();
    }

    @Override
    public int getId() {
        return 0;
    }
}
