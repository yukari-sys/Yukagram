package com.github.yukagram.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.BuildConfig;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.component.dialogs.SearchManager;
import org.thunderdog.challegram.config.Config;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.data.TD;
import org.thunderdog.challegram.navigation.BackHeaderButton;
import org.thunderdog.challegram.navigation.HeaderView;
import org.thunderdog.challegram.navigation.Menu;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.telegram.ChatFilter;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.telegram.TdlibCounter;
import org.thunderdog.challegram.theme.ColorId;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.tool.Paints;
import org.thunderdog.challegram.tool.UI;
import org.thunderdog.challegram.ui.ChatsController;
import org.thunderdog.challegram.unsorted.Size;
import org.thunderdog.challegram.unsorted.Test;
import org.thunderdog.challegram.util.text.Counter;
import org.thunderdog.challegram.util.text.TextColorSet;

import me.vkryl.android.widget.FrameLayoutFix;
import me.vkryl.core.lambda.FutureBool;
import tgx.td.ChatPosition;

public class MainControllerV2 extends ViewController<Void> implements MainControllerInterface, Menu {

  public MainControllerV2(Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public int getId () {
    return R.id.controller_main;
  }

  @Override
  public CharSequence getName () {
    return Lang.getString(R.string.Chats);
  }

  private FrameLayoutFix mainWrap;
  private ChatsController chatsController;

  @Override
  protected View onCreateView (Context context) {
    if (BuildConfig.DEBUG) {
      Test.execute();
    }

    FrameLayoutFix contentView = new FrameLayoutFix(context) {
      @Override
      protected void onDraw (Canvas c) {
        c.drawRect(0, 0, getMeasuredWidth(), Size.getHeaderDrawerSize(), Paints.fillingPaint(Theme.getColor(ColorId.filling)));
      }
    };

    mainWrap = new FrameLayoutFix(context);
    mainWrap.setLayoutParams(FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    contentView.addView(mainWrap);

    UI.setSoftInputMode(UI.getContext(context), Config.DEFAULT_WINDOW_PARAMS);

    tdlib.wallpaper().ensureWallpaperAvailability();

    chatsController = new ChatsController(this.context, tdlib).setParent(this);
    chatsController.setArguments(new ChatsController.Arguments(ChatPosition.CHAT_LIST_MAIN).setTag(null).setIsBase(true));
    chatsController.setParentWrapper(this);
    chatsController.bindThemeListeners(this);
    mainWrap.addView(chatsController.getValue());

    return contentView;
  }

  public void shareIntent(Tdlib intentTdlib, String intentAction, Intent intent) {

  }

  public ViewController<?> getPreparedControllerForPosition(int unused) {
    return chatsController;
  }

  @Override
  public boolean showComposeWrap(ViewController<?> chatsController) {
    return false;
  }


  @Override
  protected int getBackButton () {
    return BackHeaderButton.TYPE_MENU;
  }

  @Override
  protected boolean useDrawer () {
    return true;
  }

  public boolean hasFolders() {
    return false; // TODO: folders
  }


  // Search

  protected View getSearchAntagonistView () {
    return mainWrap;
  }

  @Override
  protected int getSearchMenuId () {
    return R.id.menu_clear;
  }

  protected int getChatSearchFlags () {
    return SearchManager.FLAG_NEED_TOP_CHATS | SearchManager.FLAG_NEED_MESSAGES | SearchManager.FLAG_NEED_GLOBAL_SEARCH;
  }

  protected TdApi.ChatList getChatMessagesSearchChatList () {
    if (TD.isChatListArchive(chatsController.chatList())) {
      return ChatPosition.CHAT_LIST_ARCHIVE;
    }
    return null;
  }

  protected boolean filterChatMessageSearchResult (TdApi.Chat chat) {
    ChatsController c = chatsController;
    if (c != null) {
      TdApi.ChatList chatList = c.chatList();
      if (TD.isChatListFolder(chatList) && Config.SEARCH_MESSAGES_ONLY_IN_SELECTED_FOLDER) {
        if (ChatPosition.findPosition(chat, chatList) == null)
          return false;
      }
      ChatFilter filter = c.getFilter();
      if (filter != null && filter.canFilterMessages()) {
        return filter.accept(chat);
      }
    }
    return true;
  }

  protected int getChatMessagesSearchTitle () {
    ChatFilter filter = chatsController.getFilter();
    boolean isArchive = TD.isChatListArchive(chatsController.chatList());
    if (filter != null && filter.canFilterMessages())
      return filter.getMessagesStringRes(isArchive);
    if (isArchive)
      return R.string.MessagesArchive;
    return R.string.general_Messages;
  }

  // Menu

  @Override
  protected int getMenuId () {
    return R.id.menu_main;
  }

  @Override
  public void fillMenuItems(int id, HeaderView header, LinearLayout menu) {
    if (id == R.id.menu_main) {
      header.addLockButton(menu);
      header.addSearchButton(menu, this);
    } else if (id == R.id.menu_clear) {
      header.addClearButton(menu, getSearchHeaderIconColorId(), getSearchBackButtonResource());
    } else if (chatsController != null) {
      chatsController.fillMenuItems(id, header, menu);
    }
  }

  @Override
  public void onMenuItemPressed (int id, View view) {
    if (id == R.id.menu_btn_search) {
      tdlib.checkDeadlocks(() -> runOnUiThreadOptional(() -> {
        if (isFocused()) {
          openSearchMode();
        }
      }));
    } else if (id == R.id.menu_btn_clear) {
      clearSearchInput();
    } else if (chatsController != null) {
      chatsController.onMenuItemPressed(id, view);
    }
  }


  // Counter

  public static void updateCounter(Tdlib tdlib, TdApi.ChatList chatList, Counter unreadCounter, TdlibCounter counter, boolean b) {

  }

  public static class UnreadCounterColorSet implements MainControllerInterface.UnreadCounterColorSet {

    public UnreadCounterColorSet(FutureBool aFalse) {

    }

    @Override
    public int defaultTextColor() {
      return 0;
    }

    public void setCounter(Counter unreadCounter) {

    }
  }
}
