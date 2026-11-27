package com.github.yukagram.ui.emoji_keyboard;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.support.ViewSupport;
import org.thunderdog.challegram.telegram.EmojiMediaType;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.theme.ColorId;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.theme.ThemeId;
import org.thunderdog.challegram.ui.EmojiMediaListController;
import org.thunderdog.challegram.widget.EmojiLayout;
import org.thunderdog.challegram.widget.TextView;
import org.thunderdog.challegram.widget.emoji.header.MediaHeaderView;

import me.vkryl.android.widget.FrameLayoutFix;

public class GifSectionController extends ViewController<EmojiKeyboardWrapped> {
    EmojiMediaListController mediaListController;
    ViewController<?> themeProvider;

    public GifSectionController(@NonNull Context context, Tdlib tdlib, ViewController<?> themeProvider) {
        super(context, tdlib);

        mediaListController = new EmojiMediaListController(context, tdlib, EmojiMediaType.GIF);

        this.themeProvider = themeProvider;
    }

    @Override
    public void setArguments(EmojiKeyboardWrapped args) {
        super.setArguments(args);

        mediaListController.setArguments(args.parent);
    }

    FrameLayoutFix container;

    @Override
    protected View onCreateView(Context context) {
        container = new FrameLayoutFix(context);
        container.setLayoutParams(FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        container.addView(mediaListController.getValue());

        return container;
    }

    @Override
    public int getId() {
        return 0;
    }
}
