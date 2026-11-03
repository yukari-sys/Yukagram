package com.github.yukagram.ui.main_controller;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.widget.TextView;

public class SearchController extends ViewController<MainControllerV2> {
    public SearchController(@NonNull Context context, Tdlib tdlib) {
        super(context, tdlib);
    }

    @Override
    protected View onCreateView(Context context) {
        TextView v = new TextView(context);
        v.setText("asscheeks");
        return v;
    }

    @Override
    public int getId() {
        return 0;
    }
}
