package com.github.yukagram.ui.main_controller;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.LinearLayout;
import android.widget.ViewFlipper;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.BuildConfig;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.component.dialogs.SearchManager;
import org.thunderdog.challegram.config.Config;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.navigation.BackHeaderButton;
import org.thunderdog.challegram.navigation.HeaderView;
import org.thunderdog.challegram.navigation.Menu;
import org.thunderdog.challegram.navigation.ViewController;
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

import java.util.Stack;

import me.vkryl.android.widget.FrameLayoutFix;
import me.vkryl.core.lambda.FutureBool;
import tgx.td.ChatPosition;

public class MainControllerV2 extends ViewController<Void> implements MainControllerInterface, Menu {

  public MainControllerV2(Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public int getId() {
    return R.id.controller_main;
  }

  @Override
  public CharSequence getName() {
    return Lang.getString(R.string.Chats);
  }

  private FrameLayoutFix mainWrap;
  private ViewFlipper flipper;
  private Stack<ViewController<?>> controllers;
  private SearchController searchController;

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

    controllers = new Stack<>();
    controllers.add(newDialogsController());

    flipper = new ViewFlipper(context);
    flipper.setLayoutParams(FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    Animation fadeIn = new Animation() {
      private float pivotX;
      private float pivotY;

      @Override
      protected void applyTransformation(float interpolatedTime, Transformation t) {
        float scale = 0.75f + interpolatedTime * 0.25f;
        t.setAlpha(interpolatedTime);
        t.getMatrix().setScale(scale, scale, pivotX, pivotY);
      }

      @Override
      public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);

        pivotX = resolveSize(Animation.RELATIVE_TO_PARENT, 0.5f, width, parentWidth);
        pivotY = resolveSize(Animation.RELATIVE_TO_PARENT, 0.5f, height, parentHeight);
      }
    };
    fadeIn.setDuration(150);
    fadeIn.setInterpolator(new DecelerateInterpolator());

    Animation fadeOut = new AlphaAnimation(1, 0);
    fadeOut.setDuration(150);
    fadeOut.setInterpolator(new DecelerateInterpolator());

    flipper.setInAnimation(fadeIn);
    flipper.setOutAnimation(fadeOut);
    flipper.addView(controllers.get(0).getValue()); // DialogsController

    mainWrap = new FrameLayoutFix(context);
    mainWrap.setLayoutParams(FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    mainWrap.addView(flipper);

    contentView.addView(mainWrap);

    UI.setSoftInputMode(UI.getContext(context), Config.DEFAULT_WINDOW_PARAMS);

    tdlib.wallpaper().ensureWallpaperAvailability();

    searchController = newSearchController();

    return contentView;
  }

  public DialogsController newDialogsController() {
    DialogsController c = new DialogsController(this.context, tdlib);
    c.setArguments(this);
    c.setParentWrapper(this);
    c.bindThemeListeners(this);
    return c;
  }

  public SearchController newSearchController() {
    SearchController c = new SearchController(context, tdlib);
    c.setArguments(this);
    c.setParentWrapper(this);
    c.bindThemeListeners(this);
    return c;
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
    return mainWrap;
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
    } else if (getCurrentController() instanceof Menu c) {
      c.fillMenuItems(id, header, menu);
    }
  }

  @Override
  public void openSearchMode() {
    super.openSearchMode();
    pushController(searchController);
  }

  @Override
  public void leaveSearchMode() {
    super.leaveSearchMode();
    popController();
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
    } else if (getCurrentController() instanceof Menu c) {
      c.onMenuItemPressed(id, view);
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