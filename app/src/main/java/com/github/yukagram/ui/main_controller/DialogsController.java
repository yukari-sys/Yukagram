package com.github.yukagram.ui.main_controller;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import org.thunderdog.challegram.config.Config;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.ui.ChatsController;
import org.thunderdog.challegram.widget.ViewPager;
import org.thunderdog.challegram.widget.rtl.RtlViewPager;

import me.vkryl.android.widget.FrameLayoutFix;
import tgx.td.ChatPosition;

public class DialogsController extends ViewController<MainControllerV2> implements androidx.viewpager.widget.ViewPager.OnPageChangeListener {

    private FrameLayoutFix container;
    private ViewPager pager;
    private Adapter adapter;

    public DialogsController(@NonNull Context context, Tdlib tdlib) {
        super(context, tdlib);
    }

    @Override
    protected View onCreateView(Context context) {

        adapter = new Adapter(context, this);
        pager = new RtlViewPager(context);
        pager.setOverScrollMode(Config.HAS_NICE_OVER_SCROLL_EFFECT ? View.OVER_SCROLL_IF_CONTENT_SCROLLS :View.OVER_SCROLL_NEVER);
        pager.addOnPageChangeListener(this);
        pager.setAdapter(adapter);
        pager.setLayoutParams(FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        container = new FrameLayoutFix(context);
        container.setLayoutParams(FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        container.addView(pager);

        return container;
    }

    @Override
    public int getId() {
        return 0;
    }

    public ChatsController newChatsController(int position) {
        ChatsController c = new ChatsController(this.context, tdlib).setParent(getArguments());
        c.setArguments(new ChatsController.Arguments(ChatPosition.CHAT_LIST_MAIN).setTag(null).setIsBase(true));
        c.setParentWrapper(this);
        c.bindThemeListeners(this);
        return c;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    public static class Adapter extends PagerAdapter {

        private Context context;
        private DialogsController parent;

        public Adapter(Context context, DialogsController dialogsController) {
            this.context = context;
            this.parent = dialogsController;
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return object instanceof ViewController<?> c && c.getValue() == view;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
           ViewController<?> c;
           c = parent.newChatsController(position);
           container.addView(c.getValue());
           return c;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            if (object instanceof ViewController<?>) {
                container.removeViewAt(position);
            }
        }
    }
}
