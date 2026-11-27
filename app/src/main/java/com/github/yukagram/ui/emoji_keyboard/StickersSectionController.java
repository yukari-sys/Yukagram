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
import org.thunderdog.challegram.widget.emoji.header.MediaHeaderView;

import me.vkryl.android.widget.FrameLayoutFix;

public class StickersSectionController extends ViewController<EmojiKeyboardWrapped> implements View.OnClickListener {
    EmojiMediaListController mediaListController;
    ViewController<?> themeProvider;
    boolean useDarkMode;

    public StickersSectionController(@NonNull Context context, Tdlib tdlib, ViewController<?> themeProvider, boolean useDarkMode) {
        super(context, tdlib);

        mediaListController = new EmojiMediaListController(context, tdlib, EmojiMediaType.STICKER);

        this.themeProvider = themeProvider;
        this.useDarkMode = useDarkMode;
    }

    @Override
    public void setArguments(EmojiKeyboardWrapped args) {
        super.setArguments(args);

        mediaListController.setArguments(args.parent);
    }

    FrameLayoutFix container;
    FrameLayoutFix header;
    MediaHeaderView mediaHeaderView;

    @Override
    protected View onCreateView(Context context) {
        container = new FrameLayoutFix(context);
        container.setLayoutParams(FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        int headerSize = EmojiLayout.getHeaderSize();
        header = new FrameLayoutFix(context) {
            @Override
            public boolean onTouchEvent (MotionEvent event) {
                super.onTouchEvent(event);
                return true;
            }
        };
        if (useDarkMode) {
            header.setBackgroundColor(Theme.getColor(ColorId.filling, ThemeId.NIGHT_BLACK));
        } else {
            ViewSupport.setThemedBackground(header, ColorId.filling, themeProvider);
        }
        header.setLayoutParams(FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, headerSize));

        if (getArguments().isAllowMedia()) {
            mediaHeaderView = new MediaHeaderView(context);
            mediaHeaderView.init(getArguments().parent, themeProvider, this);
            container.addView(mediaHeaderView);
        } else {
            mediaHeaderView = null; // todo: stub / check on null each time
        }

        container.addView(mediaListController.getValue());
        container.addView(header);

        return container;
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public void onClick(View v) {

    }
}
