package com.github.yukagram.ui.main_controller;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import com.github.yukagram.ui.widget.FoldersHeaderView;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.config.Config;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.telegram.ChatFoldersListener;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.tool.Screen;
import org.thunderdog.challegram.ui.ChatsController;
import org.thunderdog.challegram.unsorted.Settings;
import org.thunderdog.challegram.widget.ViewPager;
import org.thunderdog.challegram.widget.rtl.RtlViewPager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.vkryl.android.widget.FrameLayoutFix;
import tgx.td.ChatPosition;

public class DialogsController extends ViewController<MainControllerV2> implements
        OnPageChangeListener, ChatFoldersListener, View.OnLongClickListener, View.OnClickListener {

    private FrameLayoutFix container;
    private ViewPager pager;
    private Adapter adapter;
    private FoldersHeaderView header;

    public DialogsController(@NonNull Context context, Tdlib tdlib) {
        super(context, tdlib);
    }

    public void fillHeader() {
        header.fillButtons(getArguments(), mainChatListPosition, Arrays.stream(folders).map(o -> o.name.text.text).toList()); // FIXME
    }

    @Override
    protected View onCreateView(Context context) {
        header = new FoldersHeaderView(context);
        header.init(getArguments(), 0, this);
        header.setOnClickListener(this);
        header.setOnLongClickListener(this);
        fillHeader();

        adapter = new Adapter(context, this);
        pager = new RtlViewPager(context);
        pager.setOverScrollMode(Config.HAS_NICE_OVER_SCROLL_EFFECT ? View.OVER_SCROLL_IF_CONTENT_SCROLLS :View.OVER_SCROLL_NEVER);
        pager.addOnPageChangeListener(this);
        pager.setAdapter(adapter);

        container = new FrameLayoutFix(context);
        container.setLayoutParams(FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        container.addView(pager);
        container.addView(header);

        recalculatePaddings();

        return container;
    }

    public void recalculatePaddings() {
        header.post(() -> {
            FrameLayoutFix.LayoutParams params = FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params.topMargin = header.getMeasuredHeight();
            pager.setLayoutParams(params);
        });
    }

    @Override
    public int getId() {
        return 0;
    }

    public ChatsController getCurrentChatsController() {
        return getChatsController(pager.getCurrentItem());
    }

    public ChatsController getChatsController(int position) {
        return (ChatsController) adapter.cachedItems.get(position);
    }

    private ChatsController newChatsController(TdApi.ChatList chatList) {
        ChatsController c = new ChatsController(this.context, tdlib).setParent(getArguments());
        c.setArguments(new ChatsController.Arguments(chatList).setTag(null).setIsBase(true));
        c.setParentWrapper(getArguments());
        c.bindThemeListeners(this);
        return c;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        header.setSelectedIndex(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private int mainChatListPosition;
    private TdApi.ChatFolderInfo[] folders;

    public void initFolders() {
        mainChatListPosition = tdlib.mainChatListPosition();
        folders = tdlib.chatFolders();
    }

    @Override
    public boolean onLongClick(View v) {
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v instanceof FoldersHeaderView.Button) {
            int position = v.getId();
            pager.setCurrentItem(position);
        }
    }

    public static class Adapter extends PagerAdapter {

        private Context context;
        private DialogsController parent;
        private List<ViewController<?>> cachedItems;

        public Adapter(Context context, DialogsController dialogsController) {
            this.context = context;
            this.parent = dialogsController;
            this.cachedItems = new ArrayList<>();
        }

        @Override
        public int getCount() {
            return parent.folders.length + 1;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return object instanceof ViewController<?> c && c.getValue() == view;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            ViewController<?> c = position < cachedItems.size() ? cachedItems.get(position) : null;
            if (c == null) {
                if (position == parent.mainChatListPosition) {
                    c = parent.newChatsController(ChatPosition.CHAT_LIST_MAIN);
                } else {
                    int i = position > parent.mainChatListPosition ? position-1 : position;
                    c = parent.newChatsController(new TdApi.ChatListFolder(parent.folders[i].id));
                }
                while (position >= cachedItems.size()) {
                    cachedItems.add(null);
                }
                cachedItems.set(position, c);
            }
            container.addView(c.getValue());
            return c;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView(((ViewController<?>)object).getValue());
        }
    }

    public void onChatFoldersChanged (TdApi.ChatFolderInfo[] chatFolders, int mainChatListPosition) {
        this.folders = chatFolders;
        this.mainChatListPosition = mainChatListPosition;

        adapter.cachedItems.clear();
        adapter.notifyDataSetChanged();

        header.clear();
        fillHeader();
        header.invalidate();

        recalculatePaddings();
    }
}
