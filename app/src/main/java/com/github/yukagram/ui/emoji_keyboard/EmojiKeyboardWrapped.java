package com.github.yukagram.ui.emoji_keyboard;

import static org.thunderdog.challegram.widget.EmojiLayout.EMOJI_INSTALLED_CONTROLLER_ID;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.github.yukagram.ui.emoji_keyboard.widget.EmojiBottomBarView;

import org.thunderdog.challegram.R;
import org.thunderdog.challegram.config.Config;
import org.thunderdog.challegram.data.TGStickerSetInfo;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.unsorted.Settings;
import org.thunderdog.challegram.widget.EmojiLayout;
import org.thunderdog.challegram.widget.rtl.RtlViewPager;

import java.util.ArrayList;

import me.vkryl.android.widget.FrameLayoutFix;

/* it is wrapped into EmojiLayout so it is called wrapped */
public class EmojiKeyboardWrapped implements ViewPager.OnPageChangeListener, View.OnClickListener {
    private ViewController<?> context;
    private boolean allowMedia;
    private boolean animatedEmojiOnly;
    public EmojiLayout.Listener listener;
    private ViewController<?> themeProvider;
    private boolean useDarkMode;
    private boolean classicEmojiOnly;
    protected EmojiLayout parent;

    private RtlViewPager pager;
    private Adapter adapter;
    private EmojiBottomBarView bottomBar;

    public void init(
        ViewController<?> context,
        boolean allowMedia,
        boolean animatedEmojiOnly,
        @NonNull EmojiLayout.Listener listener,
        @Nullable ViewController<?> themeProvider,
        boolean useDarkMode,
        boolean classicEmojiOnly,
        EmojiLayout parent
    ) {
        this.allowMedia = allowMedia;
        this.animatedEmojiOnly = animatedEmojiOnly;
        this.listener = listener;
        this.themeProvider = themeProvider;
        this.useDarkMode = useDarkMode;
        this.classicEmojiOnly = classicEmojiOnly;
        this.parent = parent;

        adapter = new Adapter(context, themeProvider, this);
        pager = new RtlViewPager(parent.getContext());
        pager.setOverScrollMode(Config.HAS_NICE_OVER_SCROLL_EFFECT ? View.OVER_SCROLL_IF_CONTENT_SCROLLS :View.OVER_SCROLL_NEVER);
        pager.addOnPageChangeListener(this);
        pager.setAdapter(adapter);
        pager.setLayoutParams(FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        int position;
        if (allowMedia) {
            position = Settings.instance().getEmojiPosition();
            if (pager.getCurrentItem() != position) {
                pager.setCurrentItem(position, false);
            }
        } else {
            position = 0;
        }

        bottomBar = new EmojiBottomBarView(parent.getContext());
        bottomBar.init(themeProvider, position, allowMedia, allowMedia);
        bottomBar.setOnClickListener(this);

        parent.addView(pager);
        parent.addView(bottomBar);

        parent.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public void setAllowMedia(boolean allowMedia) {
        bottomBar.setAllowMedia(allowMedia);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        bottomBar.animator.setForcedValue(position + positionOffset);
    }

    @Override
    public void onPageSelected(int position) {
        bottomBar.setSelectedIndex(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onClick(View v) {
        if (v instanceof EmojiBottomBarView.Button) {
            if (v.getId() == R.id.btn_tabEmojis) pager.setCurrentItem(0);
            else if (v.getId() == R.id.btn_tabGifs) pager.setCurrentItem(1);
            else if (v.getId() == R.id.btn_tabStickers) pager.setCurrentItem(2);
        }
    }

    // EmojiMediaType
    public int getCurrentEmojiSection() {
        return pager.getCurrentItem();
    }

    public ViewController<EmojiKeyboardWrapped> getSectionController(int index) {
        return adapter.cachedItems[index];
    }

    public void reset() {

    }

    public void destroy() {

    }

    public void invalidateAll() {

    }

    public int getCurrentItem() {
        return pager.getCurrentItem();
    }

    public void moveHeader(int totalDy) {

    }

    public void onTextChanged(CharSequence charSequence) {
        
    }

    public void setIgnoreMovement(boolean ignoreMovement) {

    }

    private boolean allowPremiumFeatures;

    public void setAllowPremiumFeatures(boolean allowPremiumFeatures) {
        this.allowPremiumFeatures = allowPremiumFeatures;
    }

    public void checkAllowPremiumFeatures() {
        if (getSectionController(0) instanceof EmojiSectionController section) {
            section.setIsPremium((allowPremiumFeatures || parent.parentController.tdlib().hasPremium()) && !classicEmojiOnly, false);
        }
    }

    public boolean isAllowMedia() {
        return allowMedia;
    }

    public boolean isAllowPremiumFeatures() {
        return allowPremiumFeatures;
    }

    public void resetScrollState(boolean silent) {

    }

    public void setCurrentStickerSectionByPosition(int controllerId, int i, boolean isStickerSection, boolean animated) {
        if (controllerId == R.id.controller_emojiLayoutStickers) {
            // todo
        } else if (controllerId == EMOJI_INSTALLED_CONTROLLER_ID) {
            if (getSectionController(0) instanceof EmojiSectionController section) {
                section.emojiHeaderView.setCurrentStickerSectionByPosition(i + (isStickerSection ? 1: 0), animated);
            }
        }
    }

    public void setShowRecents(boolean showRecents) {
        if (getSectionController(2) instanceof StickersSectionController section) {
            section.mediaHeaderView.setShowRecents(showRecents);
        }
    }

    public void setShowFavorite(boolean showFavorite) {
        if (getSectionController(2) instanceof StickersSectionController section) {
            section.mediaHeaderView.setShowFavorite(showFavorite);
        }
    }

    public void setStickerSets(ArrayList<TGStickerSetInfo> stickers, boolean showFavorite, boolean showRecents, boolean showTrending, boolean isFound) {
        if (getSectionController(2) instanceof StickersSectionController section) {
            section.mediaHeaderView.setStickerSets(stickers, showFavorite, showRecents, showTrending, isFound);
        }
    }

    public void setEmojiPacks(ArrayList<TGStickerSetInfo> stickers) {
        if (getSectionController(0) instanceof EmojiSectionController section) {
            section.emojiHeaderView.setStickerSets(stickers);
        }
    }

    public void invalidateStickerSets() {
        if (getSectionController(2) instanceof StickersSectionController section) {
            section.mediaHeaderView.invalidateStickerSets();
        }
    }

    public static class Adapter extends PagerAdapter {
        ViewController<?> context;
        EmojiKeyboardWrapped parent;
        ViewController<?> themeProvider;

        public Adapter(
            ViewController<?> context,
            ViewController<?> themeProvider,
            EmojiKeyboardWrapped parent
        ) {
            this.context = context;
            this.parent = parent;
            this.themeProvider = themeProvider;
        }

        @SuppressWarnings("unchecked")
        public ViewController<EmojiKeyboardWrapped>[] cachedItems = new ViewController[3];

        @Override
        public int getCount() {
            return parent.allowMedia ? 3 : 1;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            ViewController<EmojiKeyboardWrapped> c = cachedItems[position];
            if (c == null) {
                switch (position) {
                    case 0 -> c = new EmojiSectionController(
                        container.getContext(),
                        context.tdlib(),
                        themeProvider,
                        parent.classicEmojiOnly,
                        parent.useDarkMode
                    );
                    case 1 -> c = new GifSectionController(
                        container.getContext(),
                        context.tdlib(),
                        themeProvider
                    );
                    case 2 -> c = new StickersSectionController(
                        container.getContext(),
                        context.tdlib(),
                        themeProvider,
                        parent.useDarkMode
                    );
                    default -> throw new RuntimeException("motherfucker");
                }
                c.setArguments(parent);
                cachedItems[position] = c;
            }
            container.addView(c.getValue());
            return c;
        }


        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView(((ViewController<?>)object).getValue());
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return object instanceof ViewController<?> c && c.getValue() == view;
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return switch (object) {
                case EmojiSectionController _ -> 0;
                case GifSectionController _ -> 1;
                case StickersSectionController _ -> 2;
                default -> POSITION_NONE;
            };
        }
    }
}
