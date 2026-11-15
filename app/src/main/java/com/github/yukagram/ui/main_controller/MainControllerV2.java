package com.github.yukagram.ui.main_controller;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ViewFlipper;

import androidx.annotation.Nullable;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.BuildConfig;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.component.dialogs.SearchManager;
import org.thunderdog.challegram.config.Config;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.navigation.BackHeaderButton;
import org.thunderdog.challegram.navigation.HeaderView;
import org.thunderdog.challegram.navigation.Menu;
import org.thunderdog.challegram.navigation.MoreDelegate;
import org.thunderdog.challegram.navigation.NavigationController;
import org.thunderdog.challegram.navigation.SelectDelegate;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.telegram.ConnectionState;
import org.thunderdog.challegram.telegram.GlobalAccountListener;
import org.thunderdog.challegram.telegram.GlobalConnectionListener;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.telegram.TdlibAccount;
import org.thunderdog.challegram.telegram.TdlibCounter;
import org.thunderdog.challegram.telegram.TdlibManager;
import org.thunderdog.challegram.telegram.TdlibUi;
import org.thunderdog.challegram.theme.ColorId;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.tool.Paints;
import org.thunderdog.challegram.tool.UI;
import org.thunderdog.challegram.unsorted.Size;
import org.thunderdog.challegram.unsorted.Test;
import org.thunderdog.challegram.ui.ChatsController;
import org.thunderdog.challegram.util.text.Counter;

import java.util.Stack;

import me.vkryl.android.widget.FrameLayoutFix;
import me.vkryl.core.lambda.FutureBool;

public class MainControllerV2 
    extends 
        AbstractMainController<Void> 
    implements 
        Menu, SelectDelegate, MoreDelegate
{

  public MainControllerV2(Context context, Tdlib tdlib) {
    super(context, tdlib);

    init();
  }

  @Override
  public int getId() {
    return R.id.controller_main;
  }

  @Override
  public CharSequence getName() {
    return getDefaultTitle();
  }

  private FrameLayoutFix mainWrap;
  private ViewFlipper flipper;
  private Stack<ViewController<?>> controllers;
  private SearchController searchController;
  private View dialogsView;
  private ConnectionTitleController connectionTitleController;

  public void init() {
    controllers = new Stack<>();
    DialogsController c = newDialogsController();
    dialogsView = c.getValue();
    controllers.add(c);

    searchController = newSearchController();
    connectionTitleController = newTitleController();
  }

  @Override
  protected View onCreateView(Context context) {
    if (BuildConfig.DEBUG) {
      Test.execute();
    }

    FrameLayoutFix contentView = new FrameLayoutFix(context) {
      @Override
      protected void onDraw(Canvas c) {
        c.drawRect(0, 0, getMeasuredWidth(), Size.getHeaderDrawerSize(), Paints.fillingPaint(Theme.getColor(ColorId.filling)));
      }
    };

    flipper = new ViewFlipper(context);
    flipper.setLayoutParams(FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    Animation fadeIn = new AlphaAnimation(0, 1);
    fadeIn.setDuration(150);
    fadeIn.setInterpolator(new DecelerateInterpolator());

    Animation fadeOut = new AlphaAnimation(1, 0);
    fadeOut.setDuration(150);
    fadeOut.setInterpolator(new DecelerateInterpolator());

    flipper.setInAnimation(fadeIn);
    flipper.setOutAnimation(fadeOut);
    flipper.addView(dialogsView);

    mainWrap = new FrameLayoutFix(context);
    mainWrap.setLayoutParams(FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    mainWrap.addView(flipper);

    contentView.addView(mainWrap);

    UI.setSoftInputMode(UI.getContext(context), Config.DEFAULT_WINDOW_PARAMS);

    tdlib.wallpaper().ensureWallpaperAvailability();

    return contentView;
  }

  @Override
  public View getCustomHeaderCell() {
    return connectionTitleController.getValue();
  }

  private DialogsController newDialogsController() {
    DialogsController c = new DialogsController(this.context, tdlib);
    c.setArguments(this);
    c.setParentWrapper(this);
    c.bindThemeListeners(this);
    return c;
  }

  private SearchController newSearchController() {
    SearchController c = new SearchController(context, tdlib);
    c.setArguments(this);
    c.setParentWrapper(this);
    c.bindThemeListeners(this);
    return c;
  }

  private ConnectionTitleController newTitleController() {
    ConnectionTitleController c = new ConnectionTitleController(context, tdlib);
    c.setArguments(this);
    c.setParentWrapper(this);
    c.bindThemeListeners(this);
    c.init();
    return c;
  }
  
  public String getDefaultTitle() {
    return Lang.getString(R.string.AppName);
  }

  public HeaderView getHeaderView() {
    return headerView;
  }

  public void pushController(ViewController<?> controller) {
    controllers.add(controller);
    int index = flipper.getDisplayedChild();
    flipper.addView(controller.getValue(), index + 1);
    flipper.showNext();
  }

  public void popController() {
    controllers.pop();
    int index = flipper.getDisplayedChild();
    flipper.showPrevious();
    flipper.removeViewAt(index);
  }

  public void shareIntent(Tdlib intentTdlib, String intentAction, Intent intent) {

  }

  public ViewController<?> getCurrentController() {
    return controllers.get(controllers.size() - 1);
  }

  public ViewController<?> getPreparedControllerForPosition(int index) {
    return controllers.get(index);
  }

  /**
   * The controller that currently owns the visible content: {@link DialogsController} is just a pager
   * wrapper around a {@link ChatsController}, so unwrap it to reach the controller that actually
   * implements the select mode / bulk actions menu.
   */
  @Nullable
  protected ViewController<?> getCurrentContentController() {
    if (controllers == null || controllers.isEmpty()) {
      return null;
    }
    ViewController<?> c = getCurrentController();
    if (c instanceof DialogsController) {
      ChatsController chatsController = ((DialogsController) c).getCurrentChatsController();
      if (chatsController != null) {
        return chatsController;
      }
    }
    return c;
  }

  @Override
  public boolean showComposeWrap(ViewController<?> chatsController) {
    return false;
  }


  @Override
  protected int getBackButton() {
    return BackHeaderButton.TYPE_MENU;
  }

  @Override
  protected boolean useDrawer() {
    return true;
  }

  public boolean hasFolders() {
    return false; // TODO: folders
  }


  // Search

  protected View getSearchAntagonistView() {
    return dialogsView;
  }

  @Override
  protected int getSearchMenuId() {
    return R.id.menu_clear;
  }

  protected int getChatSearchFlags() {
    return SearchManager.FLAG_NEED_TOP_CHATS | SearchManager.FLAG_NEED_MESSAGES | SearchManager.FLAG_NEED_GLOBAL_SEARCH;
  }

  // Menu

  @Override
  protected int getMenuId() {
    return R.id.menu_main;
  }

  @Override
  public void fillMenuItems(int id, HeaderView header, LinearLayout menu) {
    if (id == R.id.menu_main) {
      header.addLockButton(menu);
      header.addSearchButton(menu, this);
    } else if (id == R.id.menu_clear) {
      header.addClearButton(menu, getSearchHeaderIconColorId(), getSearchBackButtonResource());
    } else if (getCurrentContentController() instanceof Menu c) {
      c.fillMenuItems(id, header, menu);
    }
  }

  @Override
  public void openSearchMode() {
    if (inSearchMode() || searchController == null) {
      return;
    }
    searchController.getValue();
    searchController.attachSearchAdapter();
    super.openSearchMode();
    if (!inSearchMode()) {
      return;
    }
    pushController(searchController);
    searchController.onOpen();
  }

  @Override
  public void leaveSearchMode() {
    boolean searchPushed = controllers != null && controllers.size() > 1;
    super.leaveSearchMode();
    if (searchPushed) {
      popController();
    }
    clearSearchInput();
  }

  @Override
  protected void applySearchTransformFactor(float factor, boolean isOpening) {
    super.applySearchTransformFactor(factor, isOpening);
    if (searchController != null) {
      searchController.onParentSearchTransform(factor, isOpening);
    }
  }

  @Override
  protected void onEnterSearchMode() {
    super.onEnterSearchMode();
    if (searchController != null) {
      searchController.onEnterSearchMode();
    }
  }

  @Override
  protected void onAfterLeaveSearchMode() {
    super.onAfterLeaveSearchMode();
    if (searchController != null) {
      searchController.onAfterLeaveSearchMode();
    }
  }

  @Override
  protected void onSearchInputChanged(String input) {
    super.onSearchInputChanged(input);
    if (searchController != null) {
      searchController.onSearchInputChanged(input);
    }
  }

  @Override
  protected boolean needPreventiveKeyboardHide() {
    return inSearchMode() && searchController != null && searchController.inChatSearchMode();
  }

  @Override
  protected boolean useGraySearchHeader() {
    return inSearchMode() || super.useGraySearchHeader();
  }

  @Override
  public int getRootColorId() {
    return inSearchMode() ? ColorId.background : super.getRootColorId();
  }

  @Override
  public void onMenuItemPressed(int id, View view) {
    if (id == R.id.menu_btn_search) {
      tdlib.checkDeadlocks(() -> runOnUiThreadOptional(() -> {
        if (isFocused()) {
          openSearchMode();
        }
      }));
    } else if (id == R.id.menu_btn_clear) {
      clearSearchInput();
    } else if (getCurrentContentController() instanceof Menu c) {
      c.onMenuItemPressed(id, view);
    }
  }


  // Select mode & bulk actions

  @Override
  public int getSelectMenuId() {
    ViewController<?> c = getCurrentContentController();
    return c != null ? c.getSelectMenuId() : super.getSelectMenuId();
  }

  @Override
  public void finishSelectMode(int position) {
    ViewController<?> c = getCurrentContentController();
    if (c instanceof SelectDelegate) {
      ((SelectDelegate) c).finishSelectMode(position);
    }
  }

  @Override
  public void onEnterSelectMode() {
    super.onEnterSelectMode();
    ViewController<?> c = getCurrentContentController();
    if (c != null) {
      c.onEnterSelectMode();
    }
  }

  @Override
  public void onLeaveSelectMode() {
    super.onLeaveSelectMode();
    ViewController<?> c = getCurrentContentController();
    if (c != null) {
      c.onLeaveSelectMode();
    }
  }

  @Override
  public void onMoreItemPressed(int id) {
    ViewController<?> c = getCurrentContentController();
    if (c instanceof MoreDelegate) {
      ((MoreDelegate) c).onMoreItemPressed(id);
    }
  }


  // Counter

  public static void updateCounter(Tdlib tdlib, TdApi.ChatList chatList, Counter unreadCounter, TdlibCounter counter, boolean b) {

  }

  public static class UnreadCounterColorSet implements AbstractMainController.UnreadCounterColorSet {

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