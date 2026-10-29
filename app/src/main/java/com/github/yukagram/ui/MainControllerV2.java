package com.github.yukagram.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.BuildConfig;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.config.Config;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.navigation.BackHeaderButton;
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
import org.thunderdog.challegram.util.text.TextColorSet;

import me.vkryl.android.widget.FrameLayoutFix;
import me.vkryl.core.lambda.FutureBool;
import tgx.td.ChatPosition;

public class MainControllerV2 extends ViewController<Void> implements MainControllerInterface {

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

  public ViewController<?> getPreparedControllerForPosition(int i) {
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
