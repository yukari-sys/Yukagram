package com.github.yukagram.ui.main_controller;

import static androidx.viewpager.widget.ViewPager.SCROLL_STATE_DRAGGING;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SparseArrayCompat;
import androidx.core.util.ObjectsCompat;
import androidx.core.view.ViewCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import com.github.yukagram.ui.widget.FoldersHeaderView;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.config.Config;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.data.TD;
import org.thunderdog.challegram.navigation.NavigationController;
import org.thunderdog.challegram.navigation.TooltipOverlayView;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.navigation.ViewPagerTopView;
import org.thunderdog.challegram.telegram.ChatFolderStyle;
import org.thunderdog.challegram.telegram.ChatFoldersListener;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.telegram.TdlibManager;
import org.thunderdog.challegram.telegram.TdlibSettingsManager;
import org.thunderdog.challegram.telegram.TdlibUi;
import org.thunderdog.challegram.theme.ColorId;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.tool.Drawables;
import org.thunderdog.challegram.tool.Paints;
import org.thunderdog.challegram.tool.Screen;
import org.thunderdog.challegram.tool.UI;
import org.thunderdog.challegram.tool.Views;
import org.thunderdog.challegram.ui.ChatFolderIconSelector;
import org.thunderdog.challegram.ui.ChatsController;
import org.thunderdog.challegram.ui.EditChatFolderController;
import org.thunderdog.challegram.ui.EditChatFolderInviteLinkController;
import org.thunderdog.challegram.ui.SelectChatsController;
import org.thunderdog.challegram.ui.SettingsFoldersController;
import org.thunderdog.challegram.unsorted.Settings;
import org.thunderdog.challegram.widget.PopupLayout;
import org.thunderdog.challegram.widget.ViewPager;
import org.thunderdog.challegram.widget.rtl.RtlViewPager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import me.vkryl.android.widget.FrameLayoutFix;
import me.vkryl.core.StringUtils;
import tgx.td.ChatPosition;
import tgx.td.Td;

public class DialogsController extends ViewController<MainControllerV2> implements
        OnPageChangeListener, ChatFoldersListener, View.OnLongClickListener, View.OnClickListener,
        Settings.ChatFolderSettingsListener,
        TdlibSettingsManager.ChatListPositionListener {

    private FrameLayoutFix container;
    private ViewPager pager;
    private Adapter adapter;
    private FoldersHeaderView header;

    public DialogsController(@NonNull Context context, Tdlib tdlib) {
        super(context, tdlib);

        Settings.instance().addChatFolderSettingsListener(this);
        tdlib.listeners().subscribeToChatFoldersUpdates(this);
        tdlib.settings().addChatListPositionListener(this);
    }

    public void fillHeader() {
        header.currentSelectedIndex = pager == null ? 0 : pager.getCurrentItem();
        header.fillButtons(getArguments(), mainChatListPosition, folders);
        doINeedAHeader();
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
        pager.setCurrentItem(mainChatListPosition);

        container = new FrameLayoutFix(context);
        container.setLayoutParams(FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        container.addView(pager);
        container.addView(header);

        recalculatePaddings();

        return container;
    }

    public boolean hasFolders() {
        return folders.length != 0;
    }

    public void recalculatePaddings() {
        header.post(() -> {
            int addSomething = playerOffset;
            {
                FrameLayoutFix.LayoutParams params = FrameLayoutFix.newParams(header.getLayoutParams());
                params.topMargin = addSomething;
                header.setLayoutParams(params);
            }
            {
                FrameLayoutFix.LayoutParams params = FrameLayoutFix.newParams(pager.getLayoutParams());
                params.topMargin = header.getVisibility() == View.VISIBLE ? (header.getMeasuredHeight() + addSomething) : addSomething;
                pager.setLayoutParams(params);
            }
        });
    }

    @Override
    public boolean canSlideBackFrom (NavigationController navigationController, float x, float y) {
        if (!super.canSlideBackFrom(navigationController, x, y)) {
            return false;
        }

        Rect rect = new Rect();
        header.getHitRect(rect);

        return pager.getCurrentItem() == 0 && !rect.contains((int) x, (int) y);
    }

    @Override
    public int getId() {
        return 0;
    }

    public ChatsController getCurrentChatsController() {
        return (ChatsController) adapter.cachedItems.get(pager.getCurrentItem());
    }

    public ChatsController getChatsController(int position) {
        ChatsController c = (ChatsController) adapter.cachedItems.get(position);
        if (c == null) {
            TdApi.ChatList ch = getChatList(position);
            if (ch != null)
                return (ChatsController) adapter.instantiateItem(pager, position);
        }
        return null;
    }

    public TdApi.ChatList getChatList(int position) {
        return getChatList(position, null);
    }

    public TdApi.ChatList getChatList(int position, TdApi.ChatList dumb) {
        if (position == mainChatListPosition) {
            return ChatPosition.CHAT_LIST_MAIN;
        }
        int x = position > mainChatListPosition ? position-1 : position;
        for (int i = 0; i < folders.length; i++) {
            TdApi.ChatListFolder f = new TdApi.ChatListFolder(folders[i].id);
            if (Td.equalsTo(f, dumb) && x == i)
                return f;
            if (!tdlib.settings().isChatFolderEnabled(f.chatFolderId)) {
                x++;
                continue;
            }
            if (x == i)
                return f;
        }
        return null;
    }

    public TdApi.ChatList dumbGetChatList(int position) {
        if (position == mainChatListPosition) {
            return ChatPosition.CHAT_LIST_MAIN;
        }
        int x = position > mainChatListPosition ? position-1 : position;
        for (int i = 0; i < folders.length; i++) {
            TdApi.ChatListFolder f = new TdApi.ChatListFolder(folders[i].id);
            if (x == i)
                return f;
        }
        return null;
    }

    private int indexOfChatList (TdApi.ChatList chatList) {
        return indexOfChatList(chatList, false);
    }

    private int indexOfChatList (TdApi.ChatList chatList, boolean fumbduck) {
        int fi = 0;
        int e = 0;
        for (int i = 0; i < folders.length + 1; i++) {
            if (mainChatListPosition == i) {
                if (Td.equalsTo(chatList, ChatPosition.CHAT_LIST_MAIN)) return i - e;
            } else {
                TdApi.ChatListFolder f = new TdApi.ChatListFolder(folders[fi++].id);
                if (fumbduck && Td.equalsTo(chatList, f)) return i - e;
                if (!tdlib.settings().isChatFolderEnabled(f.chatFolderId)) {
                    e++;
                    continue;
                }
                if (Td.equalsTo(chatList, f)) return i - e;
            }
        }
        return -1;
    }

    private ChatsController newChatsController(TdApi.ChatList chatList) {
        ChatsController c = new ChatsController(this.context, tdlib).setParent(getArguments());
        c.setArguments(new ChatsController.Arguments(chatList).setTag(null).setIsBase(true));
        c.setParentWrapper(getArguments());
        c.bindThemeListeners(this);
        return c;
    }

    boolean doAnimate = true;
    int animateOn = -1;

    @Override
    public void onPageScrolled(int position, float factor, int factorPixels) {
        int start = pager.getCurrentItem();
        int end = position == start ? position + 1 : position;
        float f_factor = position == start ? factor : 1 - factor;

        if (f_factor == 0 && animateOn == start) doAnimate = true;
        if (!doAnimate) return;

        header.scrollTo(start, end, f_factor);
    }

    @Override
    public void onPageSelected(int position) {
        header.setSelectedIndex(position, false, true);
    }

    boolean allowClickTabs = true;

    @Override
    public void onPageScrollStateChanged(int state) {
        allowClickTabs = state != SCROLL_STATE_DRAGGING;
    }

    int playerOffset = 0;

    @Override
    protected boolean applyPlayerOffset(float factor, float top) {
        playerOffset = (int)(top * factor);
        recalculatePaddings();
        return super.applyPlayerOffset(factor, top);
    }

    private int mainChatListPosition;
    private TdApi.ChatFolderInfo[] folders;

    public void initFolders() {
        mainChatListPosition = tdlib.mainChatListPosition();
        folders = tdlib.chatFolders();
    }

    @Override
    public void onClick(View v) {
        if (v instanceof FoldersHeaderView.Button) {
            int position = indexOfChatList(dumbGetChatList(v.getId()));
            if (!allowClickTabs) return;
            header.setSelectedIndex(position, true);
            header.animationStart = pager.getCurrentItem();
            header.animationEnd = position;
            animateOn = position;
            doAnimate = false;
            pager.setCurrentItem(position);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v instanceof FoldersHeaderView.Button) {
            int position = v.getId();
            showChatListOptions(dumbGetChatList(position));
        }
        return true;
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
            return parent.folders.length + 1 - (int)Arrays.stream(parent.folders).filter(o -> !parent.tdlib.settings().isChatFolderEnabled(o.id)).count();
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
                c = parent.newChatsController(parent.getChatList(position));
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

        @Override
        public int getItemPosition(@NonNull Object object) {
            int index = parent.indexOfChatList(((ChatsController) object).chatList());
            if (index == -1) return POSITION_NONE;
            return index;
        }
    }

    public void refreshWholeStuff() {
        tdlib.runOnUiThread(() -> {
            adapter.notifyDataSetChanged();

            header.clear();
            fillHeader();
            header.invalidate();

            recalculatePaddings();
        });
    }

    public int visibleFoldersCount() {
        return (int) Arrays.stream(folders).filter(o -> tdlib.settings().isChatFolderEnabled(o.id)).count();
    }

    public String doINeedAHeader() {
        if (visibleFoldersCount() == 0) {
            header.setVisibility(View.GONE);
            header.post(this::recalculatePaddings);
            return "no, you don't";
        } else {
            header.setVisibility(View.VISIBLE);
            header.post(this::recalculatePaddings);
            return "yes, you do";
        }
    }

    @Override
    public void onChatFoldersChanged (TdApi.ChatFolderInfo[] chatFolders, int mainChatListPosition) {
        this.folders = chatFolders;
        this.mainChatListPosition = mainChatListPosition;

        doINeedAHeader();
        refreshWholeStuff();
    }

    @Override
    public void onChatListStateChanged (Tdlib tdlib, TdApi.ChatList chatList, boolean isEnabled) {
        if (!isEnabled) {
            int i = indexOfChatList(chatList, true);
            header.removeButton(i);
            if (header.currentSelectedIndex == i) {
                header.currentSelectedIndex = header.currentSelectedIndex == 0 ? 0 : (header.currentSelectedIndex -1);
                header.setSelectedIndex(header.currentSelectedIndex, false, false, true);
            }
            if (0 <= i && i < adapter.cachedItems.size())
                adapter.cachedItems.remove(i);
            adapter.notifyDataSetChanged();
            if (pager.getCurrentItem() == i) {
                pager.setCurrentItem(pager.getCurrentItem() == 0 ? 0 : (pager.getCurrentItem() -1));
            }
            doINeedAHeader();
        } else {
            refreshWholeStuff();
        }
    }

    private void showChatListOptions (TdApi.ChatList chatList) {
        boolean isMain = TD.isChatListMain(chatList);
        boolean isFolder = TD.isChatListFolder(chatList);
        boolean isArchive = TD.isChatListArchive(chatList);
        int chatFolderId = isFolder ? ((TdApi.ChatListFolder) chatList).chatFolderId : 0;
        TdApi.ChatFolderInfo chatFolderInfo;
        CharSequence title;
        if (isMain) {
            title = Lang.getString(R.string.CategoryMain);
            chatFolderInfo = null;
        } else if (isArchive) {
            title = Lang.getString(R.string.CategoryArchive);
            chatFolderInfo = null;
        } else if (isFolder) {
            chatFolderInfo = tdlib.chatFolderInfo(chatFolderId);
            title = chatFolderInfo != null ? TD.toCharSequence(chatFolderInfo.name) : null;
        } else {
            title = null;
            chatFolderInfo = null;
        }
        Options.Builder options = new Options.Builder();
        if (!StringUtils.isEmptyOrBlank(title)) {
            options.info(title);
        }
        if (tdlib.hasUnreadChats(chatList)) {
            options.item(new OptionItem(R.id.btn_markFolderAsRead, Lang.getString(R.string.MarkFolderAsRead), OptionColor.NORMAL, R.drawable.baseline_done_all_24));
        }
        if (isFolder) {
            options.item(new OptionItem(R.id.btn_editFolder, Lang.getString(R.string.EditFolder), OptionColor.NORMAL, R.drawable.baseline_edit_24));
            int chatFolderStyle = tdlib.settings().chatFolderStyle();
            options.item(new OptionItem(R.id.btn_changeFolderIcon, Lang.getString(R.string.ChatFolderChangeIcon), OptionColor.NORMAL, R.drawable.baseline_image_24));
            options.item(new OptionItem(R.id.btn_folderIncludeChats, Lang.getString(R.string.ChatFolderAddChats), OptionColor.NORMAL, R.drawable.baseline_add_24));
            if (chatFolderInfo != null) {
                options.item(new OptionItem(R.id.btn_shareFolder, Lang.getString(R.string.ShareFolder), OptionColor.NORMAL, R.drawable.baseline_share_arrow_24));
            }
        }
        if (isMain) {
            if (!Config.RESTRICT_HIDING_MAIN_LIST) {
                options.item(new OptionItem(R.id.btn_hideFolder, Lang.getString(R.string.HideAllChats), OptionColor.NORMAL, R.drawable.baseline_eye_off_24));
            }
        } else if (isFolder || isArchive) {
            options.item(new OptionItem(R.id.btn_hideFolder, Lang.getString(R.string.HideFolder), OptionColor.NORMAL, R.drawable.baseline_eye_off_24));
        }
        if (isFolder) {
            options.item(new OptionItem(R.id.btn_removeFolder, Lang.getString(R.string.RemoveFolder), OptionColor.RED, R.drawable.baseline_delete_24));
        }
        if (options.itemCount() > 0) {
            options.item(OptionItem.SEPARATOR);
        }
        options.item(new OptionItem(R.id.btn_chatFolders, Lang.getString(R.string.EditFolders), OptionColor.NORMAL, R.drawable.baseline_edit_folders_24));
        showOptions(options.build(), (v, id) -> {
            if (id == R.id.btn_editFolder) {
                tdlib.send(new TdApi.GetChatFolder(chatFolderId), (chatFolder, error) -> runOnUiThreadOptional(() -> {
                    if (error != null) {
                        UI.showError(error);
                    } else {
                        EditChatFolderController controller = new EditChatFolderController(context, tdlib);
                        controller.setArguments(new EditChatFolderController.Arguments(chatFolderId, chatFolder));
                        getArguments().navigateTo(controller);
                    }
                }));
            } else if (id == R.id.btn_hideFolder) {
                tdlib.settings().setChatListEnabled(chatList, false);

                if (header != null && !StringUtils.isEmptyOrBlank(title)) {
                    CharSequence text = TD.isChatListArchive(chatList) ?
                            Lang.getString(R.string.HideArchiveFolderInfo) :
                            Lang.getString(R.string.HideFolderInfo2, title);
                    context()
                            .tooltipManager()
                            .builder(header)
                            .locate((targetView, outRect) -> outRect.left = outRect.right = Screen.dp(56f) - (int) targetView.getX())
                            .controller(this)
                            .show(tdlib, text)
                            .hideDelayed(3500, TimeUnit.MILLISECONDS);
                }
            } else if (id == R.id.btn_folderIncludeChats) {
                tdlib.send(new TdApi.GetChatFolder(chatFolderId), (chatFolder, error) -> runOnUiThreadOptional(() -> {
                    if (error != null) {
                        UI.showError(error);
                    } else {
                        boolean showChatTypes = !chatFolder.isShareable;
                        SelectChatsController controller = new SelectChatsController(context, tdlib);
                        controller.setArguments(SelectChatsController.Arguments.includedChats(chatFolderId, chatFolder, showChatTypes));
                        getArguments().navigateTo(controller);
                    }
                }));
            } else if (id == R.id.btn_changeFolderIcon) {
                tdlib.send(new TdApi.GetChatFolder(chatFolderId), (chatFolder, error) -> runOnUiThreadOptional(() -> {
                    if (error != null) {
                        UI.showError(error);
                    } else {
                        ChatFolderIconSelector.show(this, chatFolder, selectedIcon ->
                                tdlib.setChatFolderIcon(chatFolderId, selectedIcon, Config.CHAT_FOLDERS_UNSET_DEFAULT_ICONS)
                        );
                    }
                }));
            } else if (id == R.id.btn_shareFolder) {
                TdApi.ChatFolderInfo info = ObjectsCompat.requireNonNull(chatFolderInfo);
                if (info.hasMyInviteLinks) {
                    tdlib.send(new TdApi.GetChatFolderInviteLinks(chatFolderId), (result, error) -> {
                        if (error != null) {
                            UI.showError(error);
                        } else {
                            runOnUiThreadOptional(() -> showChatFolderInviteLinks(chatFolderId, title, result.inviteLinks));
                        }
                    });
                } else {
                    return onCreateChatFolderInviteLinkClick(v, chatFolderId, info.isShareable, /* chatFolderInviteLinkCount */ 0L);
                }
            } else if (id == R.id.btn_removeFolder) {
                tdlib.ui().showDeleteChatFolderOrLeaveChats(this, chatFolderId);
            } else if (id == R.id.btn_chatFolders) {
                getArguments().navigateTo(new SettingsFoldersController(context, tdlib));
            } else if (id == R.id.btn_markFolderAsRead) {
                tdlib.readAllChats(chatList, /* after */ null);
            }
            return true;
        });
    }


    private void showChatFolderInviteLinks (int chatFolderId, @Nullable CharSequence title, TdApi.ChatFolderInviteLink[] inviteLinks) {
        Options.Builder options = new Options.Builder();
        if (!StringUtils.isEmptyOrBlank(title)) {
            options.info(title);
        }
        long chatFolderInviteLinkCount = inviteLinks.length;
        long chatFolderInviteLinkCountMax = tdlib.chatFolderInviteLinkCountMax();
        boolean canCreateInviteLink = chatFolderInviteLinkCount < chatFolderInviteLinkCountMax;
        int createNewLinkOptionColor = canCreateInviteLink ? OptionColor.BLUE : OptionColor.INACTIVE;
        SparseArrayCompat<TdApi.ChatFolderInviteLink> inviteLinkById = new SparseArrayCompat<>(inviteLinks.length);
        for (TdApi.ChatFolderInviteLink inviteLink : inviteLinks) {
            int id = ViewCompat.generateViewId();
            inviteLinkById.append(id, inviteLink);
            String name = StringUtils.isEmptyOrInvisible(inviteLink.name) ? StringUtils.urlWithoutProtocol(inviteLink.inviteLink) : inviteLink.name;
            options.item(new OptionItem(id, name, OptionColor.NORMAL, R.drawable.baseline_link_24));
        }
        options.item(new OptionItem(R.id.btn_createInviteLink, Lang.getString(R.string.CreateANewLink), createNewLinkOptionColor, R.drawable.baseline_add_link_24));
        PopupLayout popupLayout = showOptions(options.build(), (view, id) -> {
            if (id == R.id.btn_createInviteLink) {
                return onCreateChatFolderInviteLinkClick(view, chatFolderId, true, chatFolderInviteLinkCount);
            }
            TdApi.ChatFolderInviteLink inviteLink = ObjectsCompat.requireNonNull(inviteLinkById.get(id));
            showChatFolderInviteLinkOptions(chatFolderId, inviteLink);
            return true;
        });
        if (!canCreateInviteLink) {
            View button = popupLayout.getBoundView().findViewById(R.id.btn_createInviteLink);
            if (button instanceof TextView) {
                TextView textView = (TextView) button;
                Views.setMediumText(textView, textView.getText());
                Drawable[] drawables = textView.getCompoundDrawables();
                Drawable lock = Drawables.get(R.drawable.baseline_lock_16);
                lock.setColorFilter(Paints.getColorFilter(Theme.textAccentColor()));
                addThemeFilterListener(lock, ColorId.text);
                button.setTag(R.id.tag_tooltip_location_provider, (TooltipOverlayView.LocationProvider) (targetView, outRect) -> {
                    outRect.set(lock.getBounds());
                    int top = (targetView.getHeight() - outRect.height()) / 2;
                    int left = Lang.rtl() ? targetView.getPaddingLeft() : targetView.getWidth() - targetView.getPaddingRight() - outRect.width();
                    outRect.offsetTo(left, top);
                    outRect.inset(0, -Screen.dp(10f));
                });
                if (Lang.rtl()) {
                    textView.setCompoundDrawablesWithIntrinsicBounds(lock, null, drawables[2], null);
                } else {
                    textView.setCompoundDrawablesWithIntrinsicBounds(drawables[0], null, lock, null);
                }
            }
        }
    }

    private boolean onCreateChatFolderInviteLinkClick (View view, int chatFolderId, boolean isChatFolderShareable, long chatFolderInviteLinkCount) {
        if (chatFolderInviteLinkCount == 0 && !isChatFolderShareable && !tdlib.canAddShareableFolder()) {
            showShareableFoldersLimitReached(view);
            return false;
        }
        if (chatFolderInviteLinkCount >= tdlib.chatFolderInviteLinkCountMax()) {
            showChatFolderInviteLinksLimitReached(view);
            return false;
        }
        createChatFolderInviteLink(chatFolderId);
        return true;
    }


    private void showShareableFoldersLimitReached (View view) {
        UI.forceVibrateError(view);
        PopupLayout popupLayout = PopupLayout.parentOf(view);
        tdlib.ui().showLimitReachedInfo(this, popupLayout.tooltipManager(), view, TdlibUi.PremiumLimit.SHAREABLE_FOLDER_COUNT);
    }

    private void showChatFolderInviteLinksLimitReached (View view) {
        UI.forceVibrateError(view);
        PopupLayout popupLayout = PopupLayout.parentOf(view);
        tdlib.ui().showLimitReachedInfo(this, popupLayout.tooltipManager(), view, TdlibUi.PremiumLimit.CHAT_FOLDER_INVITE_LINK_COUNT);
    }

    private void showChatFolderInviteLinkOptions (int chatFolderId, TdApi.ChatFolderInviteLink inviteLink) {
        String title = StringUtils.isEmptyOrInvisible(inviteLink.name) ? StringUtils.urlWithoutProtocol(inviteLink.inviteLink) : inviteLink.name;
        Options.Builder options = new Options.Builder();
        options.info(title);
        options.item(new OptionItem(R.id.btn_shareLink, Lang.getString(R.string.ShareLink), OptionColor.NORMAL, R.drawable.baseline_share_arrow_24));
        options.item(new OptionItem(R.id.btn_copyLink, Lang.getString(R.string.InviteLinkCopy), OptionColor.NORMAL, R.drawable.baseline_content_copy_24));
        options.item(new OptionItem(R.id.btn_edit, Lang.getString(R.string.InviteLinkEdit), OptionColor.NORMAL, R.drawable.baseline_edit_24));
        showOptions(options.build(), (view, id) -> {
            if (id == R.id.btn_shareLink) {
                tdlib.ui().shareUrl(this, inviteLink.inviteLink);
            } else if (id == R.id.btn_copyLink) {
                UI.copyText(inviteLink.inviteLink, R.string.CopiedLink);
            } else if (id == R.id.btn_edit) {
                tdlib.send(new TdApi.GetChatFolder(chatFolderId), (chatFolder, error) ->  runOnUiThreadOptional(() -> {
                    if (error != null) {
                        UI.showError(error);
                    } else {
                        EditChatFolderInviteLinkController controller = new EditChatFolderInviteLinkController(context, tdlib);
                        controller.setArguments(new EditChatFolderInviteLinkController.Arguments(chatFolderId, chatFolder, inviteLink));
                        getArguments().navigateTo(controller);
                    }
                }));
            } else {
                throw new UnsupportedOperationException();
            }
            return true;
        });
    }

    @AnyThread
    private void createChatFolderInviteLink (int chatFolderId) {
        tdlib.send(new TdApi.GetChatFolder(chatFolderId), (chatFolder, error) -> {
            if (error != null) {
                UI.showError(error);
            } else {
                createChatFolderInviteLink(chatFolderId, chatFolder);
            }
        });
    }

    @AnyThread
    private void createChatFolderInviteLink (int chatFolderId, TdApi.ChatFolder chatFolder) {
        if (TD.countIncludedChatTypes(chatFolder) > 0 || TD.countExcludedChatTypes(chatFolder) > 0) {
            runOnUiThreadOptional(() -> {
                CharSequence message = Lang.getMarkdownString(this, R.string.ChatFolderInviteLinkChatTypesNotSupported);
                if (showFolderTooltip(chatFolderId, message)) {
                    View topView = header;
                    if (topView != null) {
                        UI.forceVibrateError(topView);
                    }
                } else {
                    UI.showCustomToast(message, Toast.LENGTH_LONG, 0);
                }
            });
            return;
        }
        tdlib.send(new TdApi.GetChatsForChatFolderInviteLink(chatFolderId), (chats, error) -> {
            if (error != null) {
                UI.showError(error);
            } else {
                createChatFolderInviteLink(chatFolderId, chatFolder, chats.chatIds);
            }
        });
    }

    @AnyThread
    private void createChatFolderInviteLink (int chatFolderId, TdApi.ChatFolder chatFolder, long[] shareableChatIds) {
        if (shareableChatIds.length == 0) {
            UI.showCustomToast(R.string.ChatFolderInviteLinkNoChatsToShare, Toast.LENGTH_SHORT, 0);
            return;
        }
        tdlib.createChatFolderInviteLink(chatFolderId, /* name */ "", shareableChatIds, (inviteLink, error) -> runOnUiThreadOptional(() -> {
            if (error != null) {
                UI.showError(error);
            } else {
                EditChatFolderInviteLinkController controller = new EditChatFolderInviteLinkController(context, tdlib);
                controller.setArguments(new EditChatFolderInviteLinkController.Arguments(chatFolderId, chatFolder, shareableChatIds, inviteLink));
                getArguments().navigateTo(controller);
            }
        }));
    }


    private boolean showFolderTooltip (int chatFolderId, CharSequence message) {
        return showFolderTooltip(new TdApi.ChatListFolder(chatFolderId), message);
    }

    private boolean showFolderTooltip (TdApi.ChatList chatList, CharSequence message) {
        if (header == null) {
            return false;
        }
        int index = indexOfChatList(chatList);
        if (index == -1) {
            return false;
        }
        View topView = header;
        Rect itemRect = new Rect();
        header.buttons.get(index).getHitRect(itemRect);
        int topViewX = Math.round(topView.getX());
        int topViewParentWidth = ((View) topView.getParent()).getWidth();
        int horizontalInset = Screen.dp(16f);
        itemRect.left = Math.max(itemRect.left, horizontalInset - topViewX);
        itemRect.right = Math.min(itemRect.right, topViewParentWidth - topViewX - horizontalInset);
        if (itemRect.isEmpty()) {
            return false;
        }
        context()
                .tooltipManager()
                .builder(topView)
                .locate((targetView, outRect) -> outRect.set(itemRect))
                .controller(this)
                .show(tdlib, message)
                .hideDelayed(3500, TimeUnit.MILLISECONDS);
        return true;
    }
}
