package com.github.yukagram.ui.emoji_keyboard;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.widget.TextView;

public class StickersListController extends ViewController<Void> {

    public StickersListController(@NonNull Context context, Tdlib tdlib) {
        super(context, tdlib);
    }

    @Override
    protected View onCreateView(Context context) {
        TextView view = new TextView(context);
        view.setText("o algo 3");
        return view;
    }

    @Override
    public int getId() {
        return 0;
    }
}
