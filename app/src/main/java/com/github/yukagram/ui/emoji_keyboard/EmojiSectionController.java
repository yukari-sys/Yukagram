package com.github.yukagram.ui.emoji_keyboard;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import org.thunderdog.challegram.R;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.support.ViewSupport;
import org.thunderdog.challegram.telegram.EmojiMediaType;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.theme.ColorId;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.theme.ThemeId;
import org.thunderdog.challegram.ui.EmojiListController;
import org.thunderdog.challegram.ui.EmojiMediaListController;
import org.thunderdog.challegram.ui.EmojiStatusListController;
import org.thunderdog.challegram.widget.EmojiLayout;
import org.thunderdog.challegram.widget.emoji.header.EmojiHeaderView;
import org.thunderdog.challegram.widget.emoji.section.EmojiSection;
import org.thunderdog.challegram.widget.emoji.section.EmojiSectionView;

import java.util.ArrayList;

import me.vkryl.android.widget.FrameLayoutFix;

public class EmojiSectionController extends ViewController<EmojiKeyboardWrapped> implements View.OnClickListener, View.OnLongClickListener {

    private EmojiListController emojiListController;
    private boolean useDarkMode;
    private boolean onlyClassicEmoji;
    private ViewController<?> themeProvider;

    private FrameLayoutFix container;
    public FrameLayoutFix header;
    public EmojiHeaderView emojiHeaderView;

    public EmojiSectionController(
        @NonNull Context context,
        Tdlib tdlib,
        ViewController<?> themeProvider,
        boolean onlyClassicEmoji,
        boolean useDarkMode
    ) {
        emojiListController = new EmojiListController(context, tdlib, onlyClassicEmoji);

        this.useDarkMode = useDarkMode;
        this.onlyClassicEmoji = onlyClassicEmoji;
        this.themeProvider = themeProvider;

        super(context, tdlib);
    }

    @Override
    public void setArguments(EmojiKeyboardWrapped args) {
        emojiListController.setArguments(args.parent);

        super.setArguments(args);
    }

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

        ArrayList<EmojiSection> emojiSections = new ArrayList<>(2);
        emojiSections.add(new EmojiSection(getArguments().parent, EmojiSection.SECTION_EMOJI_TRENDING, R.drawable.outline_whatshot_24, R.drawable.baseline_whatshot_24).setMakeFirstTransparent());
        emojiSections.add(new EmojiSection(getArguments().parent, EmojiSection.SECTION_EMOJI_RECENT, R.drawable.baseline_access_time_24, R.drawable.baseline_watch_later_24)/*.setFactor(1f, false)*/.setMakeFirstTransparent().setOffsetHalf(false));

        ArrayList<EmojiSection> expandableSections = new ArrayList<>(6);
        expandableSections.add(new EmojiSection(getArguments().parent, EmojiSection.SECTION_EMOJI_SMILEYS, R.drawable.baseline_emoticon_outline_24, R.drawable.baseline_emoticon_24).setMakeFirstTransparent());
        expandableSections.add(new EmojiSection(getArguments().parent,  EmojiSection.SECTION_EMOJI_ANIMALS, R.drawable.deproko_baseline_animals_outline_24, R.drawable.deproko_baseline_animals_24));/*.setIsPanda(!useDarkMode)*/
        expandableSections.add(new EmojiSection(getArguments().parent,  EmojiSection.SECTION_EMOJI_FOOD, R.drawable.baseline_restaurant_menu_24, R.drawable.baseline_restaurant_menu_24));
        expandableSections.add(new EmojiSection(getArguments().parent,  EmojiSection.SECTION_EMOJI_TRAVEL, R.drawable.baseline_directions_car_24, R.drawable.baseline_directions_car_24));
        expandableSections.add(new EmojiSection(getArguments().parent,  EmojiSection.SECTION_EMOJI_SYMBOLS, R.drawable.deproko_baseline_lamp_24, R.drawable.deproko_baseline_lamp_filled_24));
        expandableSections.add(new EmojiSection(getArguments().parent,  EmojiSection.SECTION_EMOJI_FLAGS, R.drawable.deproko_baseline_flag_outline_24, R.drawable.deproko_baseline_flag_filled_24).setMakeFirstTransparent());

        emojiHeaderView = new EmojiHeaderView(context, getArguments().parent, themeProvider, emojiSections, expandableSections, getArguments().isAllowMedia());
        emojiHeaderView.setSectionsOnClickListener(this);
        emojiHeaderView.setSectionsOnLongClickListener(this);
        emojiHeaderView.setAllowMedia(false);
        getArguments().checkAllowPremiumFeatures();

        header.addView(emojiHeaderView);

        container.addView(emojiListController.getValue());
        container.addView(header);

        return container;
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();

        if (viewId == R.id.btn_section) {
            EmojiSection section = ((EmojiSectionView) v).getSection();

            if (section.index >= 0) {
                emojiListController.showEmojiSection(section.index);
            } else if (section.index == EmojiSection.SECTION_EMOJI_TRENDING) {
                emojiListController.showTrending();
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    public void setIsPremium(boolean isPremium, boolean animated) {
        emojiHeaderView.setIsPremium(isPremium, animated);
    }
}
