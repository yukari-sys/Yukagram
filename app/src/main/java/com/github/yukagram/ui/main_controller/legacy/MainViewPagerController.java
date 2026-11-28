/*
 * This file is a part of Telegram X
 * Copyright © 2014 (tgx-android@pm.me)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * File created on 21/11/2016
 */
package com.github.yukagram.ui.main_controller.legacy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.collection.SparseArrayCompat;
import androidx.core.util.ObjectsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;

import com.github.yukagram.ui.main_controller.AbstractMainController;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.component.attach.CustomItemAnimator;
import org.thunderdog.challegram.component.dialogs.SearchManager;
import org.thunderdog.challegram.component.user.RemoveHelper;
import org.thunderdog.challegram.config.Config;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.data.TGFoundChat;
import org.thunderdog.challegram.data.TGFoundMessage;
import org.thunderdog.challegram.navigation.BackHeaderButton;
import org.thunderdog.challegram.navigation.HeaderView;
import org.thunderdog.challegram.navigation.Menu;
import org.thunderdog.challegram.navigation.MoreDelegate;
import org.thunderdog.challegram.navigation.NavigationController;
import org.thunderdog.challegram.navigation.PagerHeaderView;
import org.thunderdog.challegram.navigation.SelectDelegate;
import org.thunderdog.challegram.navigation.SimpleHeaderView;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.navigation.ViewPagerHeaderView;
import org.thunderdog.challegram.navigation.ViewPagerHeaderViewCompact;
import org.thunderdog.challegram.navigation.ViewPagerTopView;
import org.thunderdog.challegram.telegram.TGLegacyManager;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.telegram.TdlibMessageViewer;
import org.thunderdog.challegram.telegram.TdlibUi;
import org.thunderdog.challegram.theme.ColorId;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.tool.Paints;
import org.thunderdog.challegram.tool.Screen;
import org.thunderdog.challegram.tool.UI;
import org.thunderdog.challegram.tool.Views;
import org.thunderdog.challegram.ui.ListItem;
import org.thunderdog.challegram.ui.SettingHolder;
import org.thunderdog.challegram.ui.SettingsAdapter;
import org.thunderdog.challegram.unsorted.Size;
import org.thunderdog.challegram.util.OptionDelegate;
import org.thunderdog.challegram.v.CustomRecyclerView;
import org.thunderdog.challegram.widget.BaseView;
import org.thunderdog.challegram.widget.BetterChatView;
import org.thunderdog.challegram.widget.ForceTouchView;
import org.thunderdog.challegram.widget.ListInfoView;
import org.thunderdog.challegram.widget.VerticalChatView;
import org.thunderdog.challegram.widget.ViewPager;
import org.thunderdog.challegram.widget.rtl.RtlViewPager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.vkryl.android.AnimatorUtils;
import me.vkryl.android.widget.FrameLayoutFix;
import me.vkryl.core.ArrayUtils;
import me.vkryl.core.collection.LongSparseIntArray;

public abstract class MainViewPagerController<T> extends AbstractMainController<T> implements ViewPager.OnPageChangeListener, ViewPagerTopView.OnItemClickListener,
  OptionDelegate, SelectDelegate, Menu, MoreDelegate {
  public MainViewPagerController(Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  protected static final int NO_POSITION = -1;

  public interface ScrollToTopDelegate {
    void onScrollToTopRequested ();
  }

  private RtlViewPager pager;
  private ViewPagerAdapter adapter;
  protected @Nullable PagerHeaderView headerCell;

  @Override
  protected int getTransformHeaderHeight () {
    return getHeaderHeight();
  }

  protected int getMenuButtonsWidth () {
    return 0; // override for performance
  }

  /*@Override
  public boolean onBackPressed (boolean fromTop) {
    int currentPosition = getCurrentPagerItemPosition();
    ViewController<?> currentController = getCachedControllerForPosition(currentPosition);
    if (currentController != null && currentController.onBackPressed(fromTop)) {
      return true;
    }
    SparseArrayCompat<ViewController<?>> controllers = getAllCachedControllers();
    if (controllers != null) {
      for (int i = 0; i < controllers.size(); i++) {
        int position = controllers.keyAt(i);
        if (position != currentPosition) {
          ViewController<?> controller = controllers.valueAt(position);
          if (controller != null && controller.onBackPressed(fromTop)) {
            return true;
          }
        }
      }
    }
    return super.onBackPressed(fromTop);
  }*/

  @Override
  protected void handleLanguageDirectionChange () {
    super.handleLanguageDirectionChange();
    Views.setScrollBarPosition(chatSearchView);
    if (pager != null) {
      pager.checkRtl();
    }
    if (getTitleStyle() == TITLE_STYLE_COMPACT_BIG && headerCell != null) {
      TextView textView = headerCell.getView().findViewById(R.id.text_title);
      if (Views.setGravity(textView, Gravity.TOP | (Lang.rtl() ? Gravity.RIGHT : Gravity.LEFT))) {
        FrameLayout.LayoutParams params = ((FrameLayout.LayoutParams) textView.getLayoutParams());
        if (Lang.rtl()) {
          params.rightMargin = Screen.dp(68f);
          params.leftMargin = 0;
        } else {
          params.leftMargin = Screen.dp(68f);
          params.rightMargin = 0;
        }
        Views.updateLayoutParams(textView);
      }
    }
  }

  @Override
  public void handleLanguagePackEvent (int event, int arg1) {
    switch (event) {
      case Lang.EVENT_PACK_CHANGED:
        updateHeader();
        break;
      case Lang.EVENT_STRING_CHANGED:
        // TODO update specific strings?
        updateHeader();
        break;
      case Lang.EVENT_DIRECTION_CHANGED:
      case Lang.EVENT_DATE_FORMAT_CHANGED:
        // nothing to update
        break;
    }
  }

  private void updateHeader () {
    if (headerCell != null) {
      List<ViewPagerTopView.Item> sections = getPagerSectionItems();
      if (sections != null) {
        if (sections.size() != getPagerItemCount()) {
          throw new IllegalArgumentException("sections.size() != " + getPagerItemCount());
        }
        headerCell.getTopView().setItems(sections);
        headerCell.getTopView().setVisibility(sections.size() <= 1 ? View.GONE : View.VISIBLE);
      }
    }
  }

  @ColorId
  protected int getDrawerReplacementColorId () {
    return ColorId.filling;
  }

  @Override
  public abstract boolean supportsBottomInset ();

  @Override
  @CallSuper
  protected void onBottomInsetChanged (int extraBottomInset, int extraBottomInsetWithoutIme, boolean isImeInset) {
    super.onBottomInsetChanged(extraBottomInset, extraBottomInsetWithoutIme, isImeInset);
    if (chatSearchView != null) {
      chatSearchView.setPadding(0, 0, 0, extraBottomInsetWithoutIme);
    }
    if (adapter != null) {
      for (ViewController<?> c : adapter.attachedControllers) {
        c.setBottomInset(extraBottomInset, extraBottomInsetWithoutIme);
      }
    }
  }

  @Override
  protected View onCreateView (Context context) {
    FrameLayoutFix contentView = new FrameLayoutFix(context) {
      @Override
      protected void onDraw (Canvas c) {
        c.drawRect(0, 0, getMeasuredWidth(), Size.getHeaderDrawerSize(), Paints.fillingPaint(Theme.getColor(getDrawerReplacementColorId())));
      }
    };
    contentView.setWillNotDraw(false);

    List<ViewPagerTopView.Item> sections = getPagerSectionItems();
    if (sections != null) {
      if (sections.size() != getPagerItemCount()) {
        throw new IllegalArgumentException("sections.size() != " + getPagerItemCount());
      }
      switch (getTitleStyle()) {
        case TITLE_STYLE_BIG: {
          headerCell = new ViewPagerHeaderView(context);
          ((ViewPagerHeaderView) headerCell).initWithController(this);
          break;
        }
        case TITLE_STYLE_COMPACT:
        case TITLE_STYLE_COMPACT_BIG: {
          headerCell = new ViewPagerHeaderViewCompact(context);
          FrameLayoutFix.LayoutParams params = (FrameLayoutFix.LayoutParams) ((ViewPagerHeaderViewCompact) headerCell).getRecyclerView().getLayoutParams();
          if (getBackButton() != BackHeaderButton.TYPE_NONE && getMenuButtonsWidth() != 0) {
            if (Lang.rtl()) {
              params.rightMargin = Screen.dp(56f);
              params.leftMargin = getMenuButtonsWidth();
            } else {
              params.leftMargin = Screen.dp(56f);
              params.rightMargin = getMenuButtonsWidth();
            }
          }
          if (useCenteredTitle()) {
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER_HORIZONTAL;
          }
          if (getTitleStyle() == TITLE_STYLE_COMPACT_BIG) {
            headerCell.getTopView().setItemPadding(Screen.dp(12f));
            TextView title = SimpleHeaderView.newTitle(context);
            title.setTextColor(Theme.headerTextColor());
            addThemeTextColorListener(title, ColorId.headerText);
            title.setId(R.id.text_title);
            Views.setMediumText(title, getName());
            ((ViewPagerHeaderViewCompact) headerCell).addView(title);
          }
          break;
        }
      }
      headerCell.getTopView().setOnItemClickListener(this);
      headerCell.getTopView().setItems(sections);
      headerCell.getTopView().setVisibility(sections.size() <= 1 ? View.GONE : View.VISIBLE);
      addThemeInvalidateListener(headerCell.getTopView());
    }

    FrameLayoutFix.LayoutParams params = FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    switch (getTitleStyle()) {
      case TITLE_STYLE_BIG:
      case TITLE_STYLE_COMPACT_BIG:
        params.topMargin = Size.getHeaderDrawerSize();
        break;
      case TITLE_STYLE_COMPACT:
        params.topMargin = 0;
        break;
    }

    adapter = new ViewPagerAdapter(context, this);
    addAttachStateListener(attachListener);
    addFocusListener(focusListener);
    pager = new RtlViewPager(context);
    pager.setLayoutParams(params);
    pager.setOverScrollMode(Config.HAS_NICE_OVER_SCROLL_EFFECT ? View.OVER_SCROLL_IF_CONTENT_SCROLLS : View.OVER_SCROLL_NEVER);
    pager.addOnPageChangeListener(new androidx.viewpager.widget.ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled (int position, float positionOffset, int positionOffsetPixels) {
        positionOffset = ViewPager.clampPositionOffset(positionOffset);
        boolean needUpdateAttachState = currentPosition != position || (currentPositionOffset == 0f) != (positionOffset == 0f);
        currentPosition = position;
        currentPositionOffset = positionOffset;
        if (needUpdateAttachState) {
          updateAttachedControllers();
        }
        context().checkDisallowScreenshots();
      }

      @Override
      public void onPageSelected (int position) { }

      @Override
      public void onPageScrollStateChanged (int state) { }
    });
    pager.addOnPageChangeListener(this);
    pager.setAdapter(adapter);
    if (!overridePagerParent()) {
      contentView.addView(pager);
    }

    onCreateView(context, contentView, pager);

    int startPosition = adapter.reversePosition(0);
    if (startPosition != 0) {
      pager.setCurrentItem(startPosition);
    }

    return contentView;
  }

  private void updateControllerState (ViewController<?> c, int position) {
    boolean attachState = c.getAttachState();
    boolean desiredAttachState = getAttachState() && adapter.attachedControllers.contains(c);

    if (desiredAttachState) {
      if (currentPositionOffset == 0f || Math.abs(currentPositionOffset) == 1f) {
        desiredAttachState = (int) (currentPosition + currentPositionOffset) == position;
      } else {
        desiredAttachState = position == currentPosition || position == currentPosition + Math.signum(currentPositionOffset);
      }
    }

    boolean isFocused = c.isFocused();
    boolean desiredFocusState = desiredAttachState && isFocused();
    if (desiredFocusState) {
      desiredFocusState = currentPositionOffset == 0f && position == currentPosition;
    }

    if (isFocused != desiredFocusState && !desiredFocusState) {
      c.onBlur();
    }
    if (attachState != desiredAttachState) {
      c.onAttachStateChanged(navigationController, desiredAttachState);
    }
    if (isFocused != desiredFocusState && desiredFocusState) {
      c.onFocus();
    }
  }

  private final AttachListener attachListener = (context, navigation, isAttached) -> {
    updateAttachedControllers();
  };

  private final FocusStateListener focusListener = (c, isFocused) -> {
    updateAttachedControllers();
  };

  private void updateAttachedControllers () {
    for (ViewController<?> c : adapter.attachedControllers) {
      updateControllerState(c, adapter.getControllerPosition(c));
    }
  }

  protected boolean overridePagerParent () {
    return false;
  }

  @Override
  public View getViewForApplyingOffsets () {
    switch (getTitleStyle()) {
      case TITLE_STYLE_BIG:
      case TITLE_STYLE_COMPACT_BIG:
        return null;
      case TITLE_STYLE_COMPACT:
        return pager;
    }
    return null;
  }

  public final void changeName (CharSequence newName) {
    if (getTitleStyle() == TITLE_STYLE_COMPACT_BIG && headerCell != null) {
      TextView view = headerCell.getView().findViewById(R.id.text_title);
      if (view != null) {
        Views.setMediumText(view, newName);
      }
    }
  }

  public final ViewPager getViewPager () {
    return pager;
  }

  @Override
  public void onEnterSelectMode() {
    super.onEnterSelectMode();
    getViewPager().setPagingEnabled(false);
    getCurrentPagerItem().onEnterSelectMode();
  }

  @Override
  public void onLeaveSelectMode () {
    super.onLeaveSelectMode();
    getViewPager().setPagingEnabled(true);
    getCurrentPagerItem().onLeaveSelectMode();
  }

  @Override
  public int getSelectMenuId() {
    ViewController<?> c = getCurrentPagerItem();
    return c != null ? c.getSelectMenuId() : super.getSelectMenuId();
  }

  @Override
  public void finishSelectMode (int position) {
    ViewController<?> c = getCurrentPagerItem();
    if (c instanceof SelectDelegate) {
      ((SelectDelegate) c).finishSelectMode(position);
    }
  }

  @Override
  public void fillMenuItems (int id, HeaderView header, LinearLayout menu) {
    ViewController<?> c = getCurrentPagerItem();
    if (c instanceof Menu) {
      ((Menu) c).fillMenuItems(id, header, menu);
    }
  }

  @Override
  public void onMoreItemPressed (int id) {
    ViewController<?> c = getCurrentPagerItem();
    if (c instanceof MoreDelegate) {
      ((MoreDelegate) c).onMoreItemPressed(id);
    }
  }

  @Override
  public void onMenuItemPressed (int id, View view) {
    ViewController<?> c = getCurrentPagerItem();
    if (c instanceof Menu) {
      ((Menu) c).onMenuItemPressed(id, view);
    }
  }

  @Override
  public View getCustomHeaderCell () {
    return headerCell != null ? headerCell.getView() : null;
  }

  @Override
  protected int getHeaderHeight () {
    switch (getTitleStyle()) {
      case TITLE_STYLE_BIG:
      case TITLE_STYLE_COMPACT_BIG:
        return Size.getHeaderPortraitSize() + Size.getHeaderDrawerSize();
      case TITLE_STYLE_COMPACT:
        return Size.getHeaderPortraitSize();
    }
    return super.getHeaderHeight();
  }

  private int scrollState;

  protected static final int TITLE_STYLE_COMPACT = 1;
  protected static final int TITLE_STYLE_BIG = 2;
  protected static final int TITLE_STYLE_COMPACT_BIG = 3;

  @IntDef({TITLE_STYLE_COMPACT, TITLE_STYLE_BIG, TITLE_STYLE_COMPACT_BIG})
  @Retention(RetentionPolicy.SOURCE)
  public @interface TitleStyle {}

  protected @TitleStyle int getTitleStyle () {
    return TITLE_STYLE_COMPACT;
  }

  /*protected final boolean useBigTitle () {
    return false;
  }*/

  protected boolean useCenteredTitle () {
    return false;
  }

  @Override
  public final void onPageScrollStateChanged (int state) {
    scrollState = state;
    if (state != ViewPager.SCROLL_STATE_SETTLING && headerCell != null) {
      headerCell.getTopView().resetFromTo();
    }
  }

  /**
   * @param position position from logic perspective
   * @param actualPosition position from viewPager perspective
   */
  public void onPageSelected (int position, int actualPosition) {
    // override
  }

  @Override
  public final void onPageSelected (int position) {
    onPageSelected(adapter.reversePosition(position), position);
    context.notifyBackPressAvailabilityChanged();
  }

  private boolean disallowKeyboardHideOnPageScrolled;

  public void setDisallowKeyboardHideOnPageScrolled (boolean disallowKeyboardHideOnPageScrolled) {
    this.disallowKeyboardHideOnPageScrolled = disallowKeyboardHideOnPageScrolled;
  }

  public boolean isDisallowKeyboardHideOnPageScrolled () {
    return disallowKeyboardHideOnPageScrolled;
  }

  @Override
  public void onPageScrolled (int position, float positionOffset, int positionOffsetPixels) {
    positionOffset = ViewPager.clampPositionOffset(positionOffset);
    if (headerCell != null) {
      headerCell.getTopView().setSelectionFactor((float) position + positionOffset);
    }
    onPageScrolled(adapter.reversePosition(position), position, positionOffset, positionOffsetPixels);
    if (getKeyboardState() && !disallowKeyboardHideOnPageScrolled) {
      hideSoftwareKeyboard();
    }
  }

  public void onPageScrolled (int position, int actualPosition, float actualPositionOffset, int actualPositionOffsetPixels) {
    // override
  }

  public final boolean isAtFirstPosition () {
    return adapter.reversePosition(pager.getCurrentItem()) == 0;
  }

  protected final void replaceController (long itemId, ViewController<?> newController) {
    replaceController(itemId, newController, /* notifyAdapter */ true);
  }

  protected final void replaceController (long itemId, ViewController<?> newController, boolean notifyAdapter) {
    int position = getPagerItemPosition(itemId);
    if (position != NO_POSITION) {
      ViewController<?> currentController = adapter.getCachedItemByPosition(position);
      if (currentController != null) {
        currentController.destroy();
      }
      newController.setParentWrapper(this);
      newController.bindThemeListeners(this);
      adapter.cachedItems.put(position, newController);
      adapter.cachedPositions.put(itemId, position);
      if (notifyAdapter) {
        adapter.notifyDataSetChanged();
      }
    } else {
      newController.destroy();
    }
  }

  protected final void notifyPagerItemsChanged() {
    adapter.notifyDataSetChanged();
  }

  public final boolean scrollToFirstPosition () {
    if (!isAtFirstPosition()) {
      setCurrentPagerPosition(adapter.reversePosition(0), /* animated */ true);
      return true;
    }
    return false;
  }

  public final int getCurrentPagerItemPosition() {
    return adapter.reversePosition(pager.getCurrentItem());
  }

  public final long getCurrentPagerItemId () {
    return getPagerItemId(getCurrentPagerItemPosition());
  }

  public final void notifyPagerItemPositionsChanged () {
    if (headerCell != null) {
      updateHeader();
    }
    if (adapter != null) {
      int itemCount = getPagerItemCount();
      SparseArrayCompat<ViewController<?>> cachedItems = new SparseArrayCompat<>(itemCount);
      LongSparseIntArray cachedPositions = new LongSparseIntArray(itemCount);
      for (int position = 0; position < itemCount; position++) {
        long itemId = getPagerItemId(position);
        int oldPosition = adapter.getCachedItemPosition(itemId);
        if (oldPosition != NO_POSITION) {
          ViewController<?> cachedItem = adapter.getCachedItemByPosition(oldPosition);
          cachedItems.put(position, cachedItem);
          cachedPositions.put(itemId, position);
          adapter.cachedItems.remove(oldPosition);
          adapter.cachedPositions.delete(itemId);
        }
      }
      if (inTransformMode() && headerView != null) {
        int currentPosition = adapter.reversePosition(pager.getCurrentItem());
        for (int index = 0; index < adapter.cachedItems.size(); index++) {
          int position = adapter.cachedItems.keyAt(index);
          if (currentPosition == position) {
            if (headerView.inSelectMode()) {
              headerView.finishSelectMode();
            } else if (headerView.inSearchMode()) {
              headerView.closeSearchMode(true, /* after */ null);
            } else if (headerView.inCustomMode()) {
              headerView.closeCustomMode();
            }
            break;
          }
        }
      }
      adapter.destroyCachedItems();
      adapter.cachedItems = cachedItems;
      adapter.cachedPositions = cachedPositions;
      adapter.notifyDataSetChanged();
      if (headerCell != null) {
        headerCell.getTopView().setSelectionFactor(pager.getCurrentItem());
      }
    }
  }

  public final ViewController<?> getCurrentPagerItem () {
    return getCachedControllerForPosition(getCurrentPagerItemPosition());
  }

  private int currentPosition;
  private float currentPositionOffset;

  @Override
  public boolean shouldDisallowScreenshots () {
    if ((isSearchContentVisible && chatSearchDisallowScreenshots) || super.shouldDisallowScreenshots()) {
      return true;
    }
    if (isSearchAntagonistHidden()) {
      return false;
    }
    ViewController<?> controller = getCurrentPagerItem();
    if (controller != null && controller.shouldDisallowScreenshots()) {
      return true;
    }
    if (currentPositionOffset != 0f) {
      int targetPosition = currentPosition + (currentPositionOffset > 0f ? 1 : -1);
      int position = adapter.reversePosition(targetPosition);
      ViewController<?> otherController = getCachedControllerForPosition(position);
      if (otherController != null && otherController.shouldDisallowScreenshots()) {
        return true;
      }
    }
    return (isSearchContentVisible && chatSearchDisallowScreenshots) || super.shouldDisallowScreenshots();
  }

  protected void setCurrentPagerPosition (int position, boolean animated) {
    if (headerCell != null && animated) {
      headerCell.getTopView().setFromTo(pager.getCurrentItem(), position);
    }
    pager.setCurrentItem(position, animated);
  }

  @Override
  public boolean onOptionItemPressed (View optionItemView, int id) {
    ViewController<?> c = adapter.getCachedItemByPosition(pager.getCurrentItem());
    return c instanceof OptionDelegate && ((OptionDelegate) c).onOptionItemPressed(optionItemView, id);
  }

  public int getPagerItemPosition (long itemId) {
    if (adapter != null) {
      int cachedItemPosition = adapter.getCachedItemPosition(itemId);
      if (cachedItemPosition != NO_POSITION) {
        return cachedItemPosition;
      }
    }
    int pagerItemCount = getPagerItemCount();
    for (int position = 0; position < pagerItemCount; position++) {
      if (getPagerItemId(position) == itemId) {
        return position;
      }
    }
    return NO_POSITION;
  }

  public final @Nullable ViewController<?> getCachedControllerForId (@IdRes int id) {
    return adapter != null ? adapter.getCachedItemByControllerId(id) : null;
  }

  public final @Nullable ViewController<?> getCachedControllerForItemId (long itemId) {
    return adapter != null ? adapter.getCachedItemByItemId(itemId) : null;
  }

  public final @Nullable ViewController<?> getCachedControllerForPosition (int position) {
    return adapter != null ? adapter.getCachedItemByPosition(position) : null;
  }

  public final @Nullable SparseArrayCompat<ViewController<?>> getAllCachedControllers () {
    return adapter != null ? adapter.cachedItems : null;
  }

  public final @NonNull ViewController<?> getPreparedControllerForPosition (int position) {
    if (adapter == null)
      getValue();
    ViewController<?> c = adapter.prepareViewController(position);
    c.getValue();
    return c;
  }

  public final void prepareControllerForPosition (int position, @Nullable Runnable after) {
    if (adapter != null && adapter.cachedItems.get(position = adapter.reversePosition(position)) == null) {
      ViewController<?> c = adapter.prepareViewController(position);
      if (c != null) {
        if (after != null) {
          c.postOnAnimationExecute(after);
        }
        c.getValue();
        return;
      }
    }
    if (after != null) {
      after.run();
    }
  }

  private @Nullable List<ViewPagerTopView.Item> cachedPagerSectionItems;

  protected abstract int getPagerItemCount ();
  protected long getPagerItemId (int position) {
    return position;
  }
  protected abstract void onCreateView (Context context, FrameLayoutFix contentView, ViewPager pager);
  protected abstract ViewController<?> onCreatePagerItemForPosition (Context context, int position);
  protected @Nullable abstract CharSequence[] getPagerSections ();
  protected @Nullable List<ViewPagerTopView.Item> getPagerSectionItems () {
    CharSequence[] pagerSections = getPagerSections();
    if (pagerSections == null) {
      return cachedPagerSectionItems = null;
    }
    if (pagerSections.length == 0) {
      return cachedPagerSectionItems = Collections.emptyList();
    }
    if (cachedPagerSectionItems != null && cachedPagerSectionItems.size() == pagerSections.length) {
      boolean hasChanges = false;
      for (int i = 0; i < pagerSections.length; i++) {
        if (!ObjectsCompat.equals(pagerSections[i], cachedPagerSectionItems.get(i).string)) {
          hasChanges = true;
          break;
        }
      }
      if (!hasChanges) {
        return cachedPagerSectionItems;
      }
    }
    List<ViewPagerTopView.Item> pagerSectionItems = new ArrayList<>(pagerSections.length);
    for (CharSequence pagerSection : pagerSections) {
      pagerSectionItems.add(new ViewPagerTopView.Item(pagerSection));
    }
    cachedPagerSectionItems = pagerSectionItems;
    return pagerSectionItems;
  }

  @Override
  public void onPagerItemClick (int index) {
    if (getCurrentPagerItemPosition() == index) {
      ViewController<?> c = adapter.getCachedItemByPosition(pager.getCurrentItem());
      if (c instanceof ScrollToTopDelegate) {
        ((ScrollToTopDelegate) c).onScrollToTopRequested();
      }
    } else if (pager.isPagingEnabled()) {
      setCurrentPagerPosition(index, true);
    }
  }

  @Override
  public void destroy () {
    super.destroy();
    if (chatSearchManager != null) {
      TGLegacyManager.instance().removeEmojiListener(chatSearchAdapter);
      Views.destroyRecyclerView(chatSearchView);
    }
    if (chatSearchViewport != null) {
      chatSearchViewport.performDestroy();
    }
    if (adapter != null) {
      adapter.destroyCachedItems();
    }
  }

  @Override
  public boolean canSlideBackFrom (NavigationController navigationController, float x, float y) {
    return y <= HeaderView.getTopOffset() + getHeaderHeight() - Size.getHeaderDrawerSize() || (pager != null && (isAtFirstPosition() /*|| !pager.isPagingEnabled()*/) && scrollState == ViewPager.SCROLL_STATE_IDLE);
  }

  public static class ViewPagerAdapter extends PagerAdapter {
    private final Context context;
    private final MainViewPagerController<?> parent;
    private /*final*/ SparseArrayCompat<ViewController<?>> cachedItems;
    private /*final*/ LongSparseIntArray cachedPositions;

    public ViewPagerAdapter (Context context, MainViewPagerController<?> parent) {
      this.context = context;
      this.parent = parent;
      int itemCount = parent.getPagerItemCount();
      this.cachedItems = new SparseArrayCompat<>(itemCount);
      this.cachedPositions = new LongSparseIntArray(itemCount);
    }

    public int getCachedItemPosition (long itemId) {
      return cachedPositions.get(itemId, NO_POSITION);
    }

    public @Nullable ViewController<?> getCachedItemByPosition (int position) {
      return position != NO_POSITION ? cachedItems.get(position) : null;
    }

    public @Nullable ViewController<?> getCachedItemByItemId (long itemId) {
      return getCachedItemByPosition(getCachedItemPosition(itemId));
    }

    public @Nullable ViewController<?> getCachedItemByControllerId (@IdRes int id) {
      int size = cachedItems.size();
      for (int i = 0; i < size; i++) {
        ViewController<?> c = cachedItems.valueAt(i);
        if (c.getId() == id) {
          return c;
        }
      }
      return null;
    }

    @Override
    public int getCount () {
      return parent.getPagerItemCount();
    }

    private final Set<ViewController<?>> attachedControllers = new HashSet<>();

    @Override
    public void destroyItem (ViewGroup container, int position, @NonNull Object object) {
      ViewController<?> c = (ViewController<?>) object;
      container.removeView(c.getValue());
      attachedControllers.remove(c);
      parent.updateControllerState(c, position);
    }

    public void destroyCachedItems () {
      final int count = cachedItems.size();
      for (int i = 0; i < count; i++) {
        ViewController<?> c = cachedItems.valueAt(i);
        if (!c.isDestroyed()) {
          c.destroy();
        }
      }
      cachedItems.clear();
      cachedPositions.clear();
    }

    private int reversePosition (int position) {
      return position; // FIXME RTL Lang.rtl() ? getCount() - position - 1 : position;
    }

    @Override
    public int getItemPosition (@NonNull Object object) {
      if (object instanceof ViewController<?>) {
        return getControllerPosition((ViewController<?>) object);
      } else {
        return POSITION_NONE;
      }
    }

    public int getControllerPosition (ViewController<?> controller) {
      int count = cachedItems.size();
      for (int i = 0; i < count; i++) {
        if (cachedItems.valueAt(i) == controller) {
          return reversePosition(cachedItems.keyAt(i));
        }
      }
      return POSITION_NONE;
    }

    public ViewController<?> prepareViewController (int position) {
      ViewController<?> c = cachedItems.get(position);
      if (c == null) {
        c = parent.onCreatePagerItemForPosition(context, position);
        c.setParentWrapper(parent);
        c.bindThemeListeners(parent);
        cachedItems.put(position, c);
        cachedPositions.put(parent.getPagerItemId(position), position);
      }
      return c;
    }

    @Override
    @NonNull
    public Object instantiateItem (@NonNull ViewGroup container, int position) {
      ViewController<?> c = prepareViewController(reversePosition(position));
      container.addView(c.getValue());
      c.setBottomInset(parent.extraBottomInset, parent.extraBottomInsetWithoutIme);
      attachedControllers.add(c);
      parent.updateControllerState(c, position);
      if ((position == parent.currentPosition || (parent.currentPositionOffset != 0f && position == parent.currentPosition + (parent.currentPositionOffset > 0f ? 1 : -1))) && c.shouldDisallowScreenshots()) {
        parent.context().checkDisallowScreenshots();
      }
      return c;
    }

    @Override
    public boolean isViewFromObject (@NonNull View view, @NonNull Object object) {
      return object instanceof ViewController && ((ViewController<?>) object).getWrapUnchecked() == view;
    }
  }

  // Search chats

  private CustomRecyclerView chatSearchView;
  private TdlibMessageViewer.Viewport chatSearchViewport;
  private SettingsAdapter chatSearchAdapter;
  private SearchManager chatSearchManager;
  private boolean chatSearchDisallowScreenshots;

  /**
   * @return true, if event is consumed. False, if chat should be handled with default action (open)
   * */
  protected boolean onFoundChatClick (View view, TGFoundChat chat) {
    return false;
  }

  protected boolean canSelectFoundChat (TGFoundChat chat) {
    return false;
  }

  protected boolean canInteractWithFoundChat (TGFoundChat chat) {
    return true;
  }

  protected static ListItem searchValueOf (@IdRes int id, TGFoundChat chat, boolean canClear) {
    return new ListItem(ListItem.TYPE_CHAT_BETTER, id).setData(chat).setLongId(chat.getId()).setBoolValue(canClear);
  }

  private static ListItem searchValueOf (TGFoundMessage message) {
    return new ListItem(ListItem.TYPE_CHAT_BETTER, R.id.search_message).setData(message).setLongId(message.getId());
  }

  protected TdApi.ChatList getChatMessagesSearchChatList () {
    return null;
  }

  protected int getChatSearchFlags () {
    return SearchManager.FLAG_NEED_TOP_CHATS;
  }

  protected boolean filterChatSearchResult (TdApi.Chat chat) {
    return true;
  }

  protected boolean filterChatMessageSearchResult (TdApi.Chat chat) {
    return true;
  }

  protected void modifyFoundChat (TGFoundChat chat) { }

  protected void modifyFoundChatView (ListItem item, int position, BetterChatView chatView) { }

  protected boolean needChatSearchManagerPreparation () {
    // Disable if it's not needed
    return true;
  }

  private static final boolean DEBUG_CHATS_SEARCH_ADAPTER = false;

  private void removeSuggestedChat (final TGFoundChat chat) {
    showOptions(Lang.getStringBold(R.string.ChatHintsDelete, chat.getTitle()), new int[]{R.id.btn_delete, R.id.btn_cancel}, new String[]{Lang.getString(R.string.Delete), Lang.getString(R.string.Cancel)}, new int[]{OptionColor.RED, OptionColor.NORMAL}, new int[]{R.drawable.baseline_delete_sweep_24, R.drawable.baseline_cancel_24}, (itemView, id) -> {
      if (id == R.id.btn_delete) {
        chatSearchManager.removeTopChat(chat.getId());
      }
      return true;
    });
  }

  private void removeRecentChat (final TGFoundChat chat) {
    showOptions(Lang.getStringBold(R.string.DeleteXFromRecents, chat.getTitle()), new int[]{R.id.btn_delete, R.id.btn_cancel}, new String[]{Lang.getString(R.string.Delete), Lang.getString(R.string.Cancel)}, new int[]{OptionColor.RED, OptionColor.NORMAL}, new int[]{R.drawable.baseline_delete_24, R.drawable.baseline_cancel_24}, (itemView, id) -> {
      if (id == R.id.btn_delete) {
        chatSearchManager.removeRecentlyFoundChat(chat);
      }
      return true;
    });
  }

  @Override
  public void dispatchSystemInsets (View parentView, ViewGroup.MarginLayoutParams originalParams, Rect legacyInsets, Rect insets, Rect insetsWithoutIme, Rect systemInsets, Rect systemInsetsWithoutIme, boolean fitsSystemWindows) {
    super.dispatchSystemInsets(parentView, originalParams, legacyInsets, insets, insetsWithoutIme, systemInsets, systemInsetsWithoutIme, fitsSystemWindows);
    setBottomInset(insets.bottom, insetsWithoutIme.bottom);
  }

  protected final CustomRecyclerView generateChatSearchView (@Nullable ViewGroup parent) {
    final boolean noChatSearch = (getChatSearchFlags() & SearchManager.FLAG_NO_CHATS) != 0;
    chatSearchViewport = tdlib.messageViewer().createViewport(new TdApi.MessageSourceSearch(), this);
    chatSearchViewport.addIgnoreLock(() -> !this.isSearchContentVisible);
    chatSearchView = (CustomRecyclerView) Views.inflate(context(), R.layout.recycler_custom, parent);
    chatSearchView.setClipToPadding(false);
    chatSearchView.setPadding(0, 0, 0, systemInsets.bottom);
    Views.setScrollBarPosition(chatSearchView);
    tdlib.ui().attachViewportToRecyclerView(chatSearchViewport, chatSearchView);
    chatSearchView.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrollStateChanged (@NonNull RecyclerView recyclerView, int newState) {
        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
          hideSoftwareKeyboard();
        }
      }

      private int firstVisiblePosition = -1, lastVisiblePosition = -1;

      @Override
      public void onScrolled (@NonNull RecyclerView recyclerView, int dx, int dy) {
        if (dy != 0) {
          /*
           * (((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition() > 0) || ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition() < recyclerView.getAdapter().getItemCount() - 1
           * */
          hideSoftwareKeyboard();
        }
        int first = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        int last = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
        if (this.firstVisiblePosition != first || this.lastVisiblePosition != last) {
          this.firstVisiblePosition = first;
          this.lastVisiblePosition = last;

          boolean shouldDisallowScreenshots = false;
          for (int i = first; i <= last; i++) {
            ListItem item = chatSearchAdapter.getItem(i);
            if (item != null && item.getData() instanceof TGFoundMessage) {
              TdApi.Message message = ((TGFoundMessage) item.getData()).getMessage();
              if (!message.canBeSaved) {
                shouldDisallowScreenshots = true;
                break;
              }
            }
          }
          if (chatSearchDisallowScreenshots != shouldDisallowScreenshots) {
            chatSearchDisallowScreenshots = shouldDisallowScreenshots;
            context().checkDisallowScreenshots();
          }
        }
        if (last + 5 >= chatSearchAdapter.getItems().size()) {
          chatSearchManager.loadMoreMessages();
        }
      }
    });
    chatSearchView.setBackgroundColor(Theme.backgroundColor());
    addThemeBackgroundColorListener(chatSearchView, ColorId.background);
    chatSearchView.setLayoutManager(new LinearLayoutManager(context(), RecyclerView.VERTICAL, false));
    if (parent != null) {
      chatSearchView.setAlpha(0f);
      chatSearchView.setScrollDisabled(true);
    } else {
      isSearchContentVisible = true;
    }
    chatSearchView.setLayoutParams(FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    final BaseView.ActionListProvider previewProvider = (v, context, ids, icons, strings, target) -> {
      final ListItem item = (ListItem) v.getTag();

      final Object data = item.getData();
      final TGFoundChat chat;
      if (data instanceof TGFoundMessage) {
        chat = ((TGFoundMessage) data).getChat();
      } else {
        chat = (TGFoundChat) data;
      }
      boolean canInteractWithFoundChat = canInteractWithFoundChat(chat) && navigationController != null;
      if (navigationController == null) {
        context.setExcludeHeader(true);
      }

      final int itemId = item.getId();
      if (itemId == R.id.search_chat_local || itemId == R.id.search_chat_top) {
        if (Config.CALL_FROM_PREVIEW && canInteractWithFoundChat && tdlib.cache().userGeneral(chat.getUserId())) {
          ids.append(R.id.btn_phone_call);
          strings.append(R.string.Call);
          icons.append(R.drawable.baseline_call_24);
        }
        boolean chatAvailable = tdlib.chatAvailable(chat.getChat());
        if (chatAvailable) {
          ids.append(R.id.btn_notifications);
          boolean hasNotifications = tdlib.chatNotificationsEnabled(chat.getId());
          strings.append(hasNotifications ? R.string.Mute : R.string.Unmute);
          icons.append(hasNotifications ? R.drawable.baseline_notifications_off_24 : R.drawable.baseline_notifications_24);
          if (canInteractWithFoundChat) {
            if (chat.getList() != null) {
              boolean isPinned = tdlib.chatPinned(chat.getList(), chat.getId());
              ids.append(R.id.btn_pinUnpinChat);
              strings.append(isPinned ? R.string.Unpin : R.string.Pin);
              icons.append(isPinned ? R.drawable.deproko_baseline_pin_undo_24 : R.drawable.deproko_baseline_pin_24);
              if (tdlib.canArchiveOrUnarchiveChat(chat.getChat())) {
                boolean isArchived = tdlib.chatArchived(chat.getId());
                ids.append(R.id.btn_archiveUnarchiveChat);
                strings.append(isArchived ? R.string.Unarchive : R.string.Archive);
                icons.append(isArchived ? R.drawable.baseline_unarchive_24 : R.drawable.baseline_archive_24);
              }
            }
            boolean canRead = tdlib.canMarkAsRead(chat.getChat());
            ids.append(canRead ? R.id.btn_markChatAsRead : R.id.btn_markChatAsUnread);
            strings.append(canRead ? R.string.MarkAsRead : R.string.MarkAsUnread);
            icons.append(canRead ? Config.ICON_MARK_AS_READ : Config.ICON_MARK_AS_UNREAD);
            if (chat.hasHighlight()) {
              ids.append(R.id.btn_removeChatFromListOrClearHistory);
              strings.append(R.string.Delete);
              icons.append(R.drawable.baseline_delete_24);
            }
          }
        }
        if (!chat.hasHighlight()) {
          ids.append(R.id.btn_delete);
          strings.append(R.string.Remove);
          icons.append(R.drawable.baseline_delete_sweep_24);
        }
      } else if (itemId == R.id.search_chat_global) {// Nothing?
      }

      if (canSelectFoundChat(chat)) {
        ids.append(R.id.btn_selectChat);
        strings.append(R.string.Select);
        icons.append(R.drawable.baseline_playlist_add_check_24);
      }

      return new ForceTouchView.ActionListener() {
        @Override
        public void onForceTouchAction (ForceTouchView.ForceTouchContext context, int actionId, Object arg) {
          if (actionId == R.id.btn_phone_call) {
            tdlib.context().calls().makeCallDelayed(MainViewPagerController.this, chat.getUserId(), null, true);
          } else if (actionId == R.id.btn_pinUnpinChat || actionId == R.id.btn_archiveUnarchiveChat || actionId == R.id.btn_notifications || actionId == R.id.btn_markChatAsRead || actionId == R.id.btn_markChatAsUnread || actionId == R.id.btn_removeChatFromListOrClearHistory || actionId == R.id.btn_removePsaChatFromList) {
            tdlib.ui().processChatAction(MainViewPagerController.this, chat.getList(), chat.getChatId(), chat.getMessageThread(), new TdApi.MessageSourceSearch(), actionId, null);
          } else if (actionId == R.id.btn_selectChat) {
            onFoundChatClick(v, chat);
          } else if (actionId == R.id.btn_delete) {
            final int itemId = item.getId();
            if (itemId == R.id.search_chat_top) {
              removeSuggestedChat(chat);
            } else if (itemId == R.id.search_chat_local) {
              removeRecentChat(chat);
            }
          }
        }

        @Override
        public void onAfterForceTouchAction (ForceTouchView.ForceTouchContext context, int actionId, Object arg) {

        }
      };
    };

    final SettingsAdapter topChatsAdapter = new SettingsAdapter(this, v -> {
      ListItem item = (ListItem) v.getTag();
      final int itemId = item.getId();
      if (itemId == R.id.search_chat_top) {
        final TGFoundChat chat = (TGFoundChat) item.getData();
        if (chat.getId() != 0 && !onFoundChatClick(v, chat)) {
          tdlib.ui().openChat(MainViewPagerController.this, chat.getId(), null);
        }
      }
    }, this) {
      @Override
      protected void setChatData (ListItem item, VerticalChatView chatView) {
        chatView.setPreviewActionListProvider(previewProvider);
        chatView.setChat((TGFoundChat) item.getData());
      }
    };
    topChatsAdapter.setOnLongClickListener(v -> {
      final ListItem item = (ListItem) v.getTag();
      int itemId = item.getId();
      if (itemId == R.id.search_chat_top) {
        final TGFoundChat chat = (TGFoundChat) item.getData();
        removeSuggestedChat(chat);
        return true;
      }
      return false;
    });

    chatSearchAdapter = new SettingsAdapter(this, v -> {
      ListItem listItem = (ListItem) v.getTag();
      int listItemId = listItem.getId();
      if (listItemId == R.id.search_section_local) {
        if (!chatSearchManager.areLocalChatsRecent()) {
          return;
        }
        showOptions(Lang.getString(R.string.ClearRecentsHint), new int[] {R.id.btn_delete, R.id.btn_cancel}, new String[] {Lang.getString(R.string.Clear), Lang.getString(R.string.Cancel)}, new int[] {OptionColor.RED, OptionColor.NORMAL}, new int[] {R.drawable.baseline_delete_24, R.drawable.baseline_cancel_24}, (itemView, id) -> {
          if (id == R.id.btn_delete) {
            chatSearchManager.clearRecentlyFoundChats();
          }
          return true;
        });
      } else if (listItemId == R.id.search_chat_local || listItemId == R.id.search_chat_global) {
        final TGFoundChat chat = (TGFoundChat) listItem.getData();
        if (listItem.getId() == R.id.search_chat_global) {
          preventLeavingSearchMode();
        }
        if (chat.getId() != 0) {
          chatSearchManager.addRecentlyFoundChat(chat);
          if (!onFoundChatClick(v, chat)) {
            tdlib.ui().openChat(MainViewPagerController.this, chat.getId(), null);
          }
        } else if (chat.getUserId() != 0) {
          tdlib.ui().openPrivateChat(MainViewPagerController.this, chat.getUserId(), new TdlibUi.ChatOpenParameters().noOpen().after(createdChatId -> {
            chat.setCreatedChatId(createdChatId);
            chatSearchManager.addRecentlyFoundChat(chat);
            if (!onFoundChatClick(v, chat)) {
              tdlib.ui().openChat(MainViewPagerController.this, createdChatId, null);
            }
          }));
        }
      } else if (listItemId == R.id.search_message) {
        TGFoundMessage message = (TGFoundMessage) listItem.getData();
        TdApi.Message rawMessage = message.getMessage();
        preventLeavingSearchMode();
        String query = getLastSearchInput();
        tdlib.ui().openChat(MainViewPagerController.this, rawMessage.chatId, new TdlibUi.ChatOpenParameters().foundMessage(query, rawMessage).keepStack());
      }
    }, this) {
      @Override
      public void onEmojiUpdated (boolean isPackSwitch) {
        for (RecyclerView parentView : parentViews) {
          LinearLayoutManager manager = (LinearLayoutManager) parentView.getLayoutManager();
          final int first = manager.findFirstVisibleItemPosition();
          final int last = manager.findLastVisibleItemPosition();
          for (int i = first; i <= last; i++) {
            View view = manager.findViewByPosition(i);
            if (view instanceof BetterChatView) {
              view.invalidate();
            }
          }
          if (first > 0) {
            notifyItemRangeChanged(0, first);
          }
          if (last < getItemCount() - 1) {
            notifyItemRangeChanged(last, getItemCount() - last);
          }
        }
      }

      @Override
      protected void setRecyclerViewData(ListItem item, RecyclerView recyclerView, boolean isInitialization) {
        int itemId = item.getId();
        if (itemId == R.id.search_top) {
          if (recyclerView.getAdapter() != topChatsAdapter) {
            recyclerView.setItemAnimator(new CustomItemAnimator(AnimatorUtils.DECELERATE_INTERPOLATOR, 180l));
            recyclerView.setAdapter(topChatsAdapter);
          }
        }
      }

      @Override
      protected void setChatData (ListItem item, int position, BetterChatView chatView) {
        chatView.setPreviewActionListProvider(previewProvider);
        int itemId = item.getId();
        if (itemId == R.id.search_chat_local || itemId == R.id.search_chat_global) {
          chatView.setChat((TGFoundChat) item.getData());
        } else if (itemId == R.id.search_message) {
          chatView.setMessage((TGFoundMessage) item.getData());
        }
        MainViewPagerController.this.modifyFoundChatView(item, position, chatView);
      }

      @Override
      protected void setInfo (ListItem item, int position, ListInfoView infoView) {
        if (chatSearchManager.isEndReached()) {
          infoView.showInfo(Lang.pluralBold(R.string.xMessages, chatSearchManager.getFoundMessagesCount()));
        } else {
          infoView.showProgress();
        }
      }
    };
    TGLegacyManager.instance().addEmojiListener(chatSearchAdapter);
    chatSearchAdapter.setOnLongClickListener(v -> {
      final int chatViewId = v.getId();
      if (chatViewId == R.id.search_chat_local) {
        ListItem item = (ListItem) v.getTag();
        if (!item.getBoolValue()) {
          return false;
        }
        final TGFoundChat chat = (TGFoundChat) item.getData();
        showOptions(Lang.getStringBold(R.string.DeleteXFromRecents, chat.getTitle()), new int[] {R.id.btn_delete, R.id.btn_cancel}, new String[] {Lang.getString(R.string.Delete), Lang.getString(R.string.Cancel)}, new int[] {OptionColor.RED, OptionColor.NORMAL}, new int[] {R.drawable.baseline_delete_24, R.drawable.baseline_cancel_24}, (itemView, id) -> {
          if (id == R.id.btn_delete) {
            chatSearchManager.removeRecentlyFoundChat(chat);
          }
          return true;
        });
        return true;
      }
      return false;
    });
    if (!noChatSearch) {
      chatSearchAdapter.setItems(new ListItem[]{new ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL)}, false);
    }

    final RecyclerView.ItemAnimator chatSearchViewAnimator = chatSearchView.getItemAnimator();
    if (!DEBUG_CHATS_SEARCH_ADAPTER) {
      if (!noChatSearch) {
        chatSearchView.setItemAnimator(null);
      }
    }
    if (parent == null) {
      chatSearchView.setAdapter(chatSearchAdapter);
    }

    chatSearchManager = new SearchManager(tdlib, new SearchManager.Listener() {
      @Override
      public void onOpen () {
        setNeedPreventKeyboardLag(true);
        if (chatSearchView.getAdapter() != null) {
          chatSearchAdapter.resetRecyclerScrollById(R.id.search_top);
          ((LinearLayoutManager) chatSearchView.getLayoutManager()).scrollToPositionWithOffset(0, 0);
        }
      }

      @Override
      public boolean customFilter (TdApi.Chat chat) {
        return filterChatSearchResult(chat);
      }

      @Override
      public boolean filterMessageSearchResultSource (TdApi.Chat chat) {
        return MainViewPagerController.this.filterChatMessageSearchResult(chat);
      }

      @Override
      public void modifyFoundChat (TGFoundChat foundChat) {
        MainViewPagerController.this.modifyFoundChat(foundChat);
      }

      @Override
      public void onClose () {
        setNeedPreventKeyboardLag(false);
        if (chatSearchView.getAdapter() != null) {
          Views.destroyRecyclerView(chatSearchView);
          chatSearchView.setAdapter(null);
        }
      }

      @Override
      public void onPerformNewSearch (boolean isDefault) {
        if (!DEBUG_CHATS_SEARCH_ADAPTER) {
          if (!noChatSearch) {
            chatSearchView.setItemAnimator(null);
          }
        }
      }

      private void updateTopChatsAdapter (ArrayList<TGFoundChat> topChats) {
        ArrayList<ListItem> chatItems = new ArrayList<>(topChats.size());
        for (TGFoundChat chat : topChats) {
          chatItems.add(new ListItem(ListItem.TYPE_CHAT_VERTICAL, R.id.search_chat_top).setData(chat).setLongId(chat.getId()));
        }
        topChatsAdapter.replaceItems(chatItems);
      }

      @Override
      public boolean onAddTopChats (ArrayList<TGFoundChat> topChats, boolean updateData, boolean isSilent) {
        if (updateData) {
          updateTopChatsAdapter(topChats);
        }
        if (!isSilent && chatSearchAdapter.indexOfViewById(R.id.search_section_top) == -1) {
          int startIndex = 1; // chatSearchAdapter.getItems().size();
          int addedCount = generateTopCells(chatSearchAdapter.getItems(), startIndex);
          chatSearchAdapter.notifyItemRangeInserted(startIndex, addedCount);
          return true;
        }
        return false;
      }

      @Override
      public void onRemoveTopChats (boolean updateData, boolean isSilent) {
        if (updateData) {
          topChatsAdapter.replaceItems(null);
        }
        if (!isSilent) {
          int i = chatSearchAdapter.indexOfViewById(R.id.search_section_top);
          if (i != -1) {
            chatSearchAdapter.removeRange(i, 4);
          }
        }
      }

      @Override
      public void onRemoveTopChat (long chatId) {
        topChatsAdapter.removeItemByLongId(chatId);
      }

      @Override
      public void onUpdateTopChats (long[] oldChatIds, long[] newChatIds) {
        updateTopChatsAdapter(chatSearchManager.getTopChats());
      }

      @Override
      public void onAddLocalChats (ArrayList<TGFoundChat> localChats) {
        if (chatSearchAdapter.indexOfViewById(R.id.search_section_local) == -1) {
          int startIndex = chatSearchAdapter.indexOfViewById(R.id.search_section_messages);
          if (startIndex == -1) {
            startIndex = chatSearchAdapter.indexOfViewById(R.id.search_section_global);
            if (startIndex == -1) {
              startIndex = chatSearchAdapter.getItems().size();
            }
          }
          int addedCount = generateChatsCells(startIndex, chatSearchAdapter.getItems(), localChats, R.id.search_section_local, R.id.search_chat_local, chatSearchManager.areLocalChatsRecent() ? R.string.Recent : R.string.ChatsAndContacts, chatSearchManager.areLocalChatsRecent());
          chatSearchAdapter.notifyItemRangeInserted(startIndex, addedCount);
        }
      }

      @Override
      public void onAddMoreLocalChats (ArrayList<TGFoundChat> addedLocalChats, int oldChatCount) {
        int startIndex = chatSearchAdapter.indexOfViewById(R.id.search_section_local);
        if (startIndex != -1) {
          startIndex = startIndex + oldChatCount * 2 + 1;
          int addedCount = 0;
          List<ListItem> items = chatSearchAdapter.getItems();
          ArrayUtils.ensureCapacity(items, items.size() + addedLocalChats.size() * 2);
          for (TGFoundChat chat : addedLocalChats) {
            items.add(startIndex + addedCount++, new ListItem(ListItem.TYPE_SEPARATOR));
            items.add(startIndex + addedCount++, searchValueOf(R.id.search_chat_local, chat, chatSearchManager.areLocalChatsRecent()));
          }
          chatSearchAdapter.notifyItemRangeInserted(startIndex, addedCount);
        }
      }

      @Override
      public void onRemoveLocalChats (int oldChatCount) {
        int i = chatSearchAdapter.indexOfViewById(R.id.search_section_local);
        if (i != -1) {
          chatSearchAdapter.removeRange(i, 3 + (oldChatCount - 1) * 2 + 1);
        }
      }

      @Override
      public void onUpdateLocalChats (final int oldChatCount, ArrayList<TGFoundChat> localChats) {
        int startIndex = chatSearchAdapter.indexOfViewById(R.id.search_section_local);
        if (startIndex == -1) {
          return;
        }

        ListItem headerItem = chatSearchAdapter.getItems().get(startIndex);
        final boolean canClear = chatSearchManager.areLocalChatsRecent();
        if (canClear != (headerItem.getViewType() == ListItem.TYPE_HEADER_WITH_ACTION)) {
          headerItem.setViewType(canClear ? ListItem.TYPE_HEADER_WITH_ACTION : ListItem.TYPE_HEADER);
          headerItem.setString(canClear ? R.string.Recent : R.string.ChatsAndContacts);
          chatSearchAdapter.notifyItemChanged(startIndex);
        }

        startIndex += 2;

        final int newChatCount = localChats.size();
        final int changedChatCount = Math.min(oldChatCount, newChatCount);
        for (int i = 0; i < changedChatCount; i++) {
          final int position = startIndex + i * 2;
          ListItem item = chatSearchAdapter.getItems().get(position);
          if (item.getViewType() != ListItem.TYPE_CHAT_BETTER) {
            throw new IllegalStateException("Bug, viewType: " + item.getViewType());
          }
          TGFoundChat chat = localChats.get(i);
          item.setData(chat).setLongId(chat.getId()).setBoolValue(canClear);
        }
        chatSearchAdapter.notifyItemRangeChanged(startIndex, 1 + (changedChatCount - 1) * 2);

        startIndex += 1 + (changedChatCount - 1) * 2;

        if (newChatCount > oldChatCount) {
          int index = startIndex;
          final int addedChatCount = newChatCount - oldChatCount;
          for (int i = 0; i < addedChatCount; i++) {
            TGFoundChat chat = localChats.get(i + changedChatCount);
            chatSearchAdapter.getItems().add(index++, new ListItem(ListItem.TYPE_SEPARATOR));
            chatSearchAdapter.getItems().add(index++, searchValueOf(R.id.search_chat_local, chat, canClear));
          }
          chatSearchAdapter.notifyItemRangeInserted(startIndex, index - startIndex);
        } else if (newChatCount < oldChatCount) {
          final int removedChatCount = oldChatCount - newChatCount;
          chatSearchAdapter.removeRange(startIndex, removedChatCount * 2);
        }
      }

      @Override
      public void onRemoveLocalChat (long chatId, int position, int totalCount) {
        int i = chatSearchAdapter.indexOfViewById(R.id.search_section_local);
        if (i != -1) {
          i += 2;
          if (position == 0) {
            chatSearchAdapter.removeRange(i, 2);
          } else {
            chatSearchAdapter.removeRange(i + position * 2 - 1, 2);
          }
        }
      }

      @Override
      public void onAddLocalChat (TGFoundChat chat) {
        int i = chatSearchAdapter.indexOfViewById(R.id.search_section_local);
        if (i != -1) {
          i += 2;
          chatSearchAdapter.getItems().add(i, new ListItem(ListItem.TYPE_SEPARATOR));
          chatSearchAdapter.getItems().add(i, searchValueOf(R.id.search_chat_local, chat, true));
          chatSearchAdapter.notifyItemRangeInserted(i, 2);
        }
      }

      @Override
      public void onMoveLocalChat (TGFoundChat chat, int fromPosition, int totalCount) {
        int i = chatSearchAdapter.indexOfViewById(R.id.search_section_local);
        if (i != -1) {
          i += 2;
          int adapterPosition = i + fromPosition * 2;
          ListItem chatItem = chatSearchAdapter.getItems().remove(adapterPosition);
          ListItem shadowItem;
          if (fromPosition != totalCount - 1) {
            shadowItem = chatSearchAdapter.getItems().remove(adapterPosition);
            chatSearchAdapter.notifyItemRangeRemoved(adapterPosition, 2);
          } else {
            shadowItem = chatSearchAdapter.getItems().remove(adapterPosition - 1);
            chatSearchAdapter.notifyItemRangeRemoved(adapterPosition - 1, 2);
          }
          chatSearchAdapter.getItems().add(i, shadowItem);
          chatSearchAdapter.getItems().add(i, chatItem);
          chatSearchAdapter.notifyItemRangeInserted(i, 2);
        }
      }

      @Override
      public void onAddGlobalChats (ArrayList<TGFoundChat> globalChats) {
        if (chatSearchAdapter.indexOfViewById(R.id.search_section_global) == -1) {
          int startIndex = chatSearchAdapter.indexOfViewById(R.id.search_section_messages);
          if (startIndex == -1) {
            startIndex = chatSearchAdapter.getItems().size();
          }
          int addedCount = generateChatsCells(startIndex, chatSearchAdapter.getItems(), globalChats, R.id.search_section_global, R.id.search_chat_global, R.string.GlobalSearch, false);
          chatSearchAdapter.notifyItemRangeInserted(startIndex, addedCount);
        }
      }

      @Override
      public void onRemoveGlobalChats (int oldChatCount) {
        int startPosition = chatSearchAdapter.indexOfViewById(R.id.search_section_global);
        if (startPosition != -1) {
          chatSearchAdapter.removeRange(startPosition, 3 + (oldChatCount - 1) * 2 + 1);
        }
      }

      @Override
      public void onUpdateGlobalChats (int oldChatCount, ArrayList<TGFoundChat> globalChats) {
        int startIndex = chatSearchAdapter.indexOfViewById(R.id.search_section_global);
        if (startIndex == -1) {
          return;
        }

        startIndex += 2;

        final int newChatCount = globalChats.size();
        final int changedChatCount = Math.min(oldChatCount, newChatCount);
        for (int i = 0; i < changedChatCount; i++) {
          final int position = startIndex + i * 2;
          ListItem item = chatSearchAdapter.getItems().get(position);
          if (item.getViewType() != ListItem.TYPE_CHAT_BETTER) {
            throw new IllegalStateException("Bug, viewType: " + item.getViewType());
          }
          TGFoundChat chat = globalChats.get(i);
          item.setData(chat).setLongId(chat.getId()).setBoolValue(false);
        }
        chatSearchAdapter.notifyItemRangeChanged(startIndex, 1 + (changedChatCount - 1) * 2);

        startIndex += 1 + (changedChatCount - 1) * 2;

        if (newChatCount > oldChatCount) {
          int index = startIndex;
          final int addedChatCount = newChatCount - oldChatCount;
          for (int i = 0; i < addedChatCount; i++) {
            TGFoundChat chat = globalChats.get(i + changedChatCount);
            chatSearchAdapter.getItems().add(index++, new ListItem(ListItem.TYPE_SEPARATOR));
            chatSearchAdapter.getItems().add(index++, new ListItem(ListItem.TYPE_CHAT_BETTER, R.id.search_chat_global).setData(chat).setLongId(chat.getId()));
          }
          chatSearchAdapter.notifyItemRangeInserted(startIndex, index - startIndex);
        } else if (newChatCount < oldChatCount) {
          final int removedChatCount = oldChatCount - newChatCount;
          chatSearchAdapter.removeRange(startIndex, removedChatCount * 2);
        }
      }

      @Override
      public void onAddMessages (ArrayList<TGFoundMessage> messages) {
        if (chatSearchAdapter.indexOfViewById(R.id.search_section_messages) == -1) {
          generateMessagesCells(chatSearchAdapter.getItems(), messages, 0, messages.size());
        }
      }

      @Override
      public void onUpdateMessages (int oldMessageCount, ArrayList<TGFoundMessage> messages) {
        int startIndex = chatSearchAdapter.indexOfViewById(R.id.search_section_messages);
        if (startIndex == -1) {
          return;
        }

        startIndex += 2;

        final int newMessageCount = messages.size();
        final int changedChatCount = Math.min(oldMessageCount, newMessageCount);
        for (int i = 0; i < changedChatCount; i++) {
          final int position = startIndex + i * 2;
          ListItem item = chatSearchAdapter.getItems().get(position);
          if (item.getViewType() != ListItem.TYPE_CHAT_BETTER) {
            throw new IllegalStateException("Bug, viewType: " + item.getViewType());
          }
          TGFoundMessage message = messages.get(i);
          item.setData(message).setLongId(message.getId()).setBoolValue(false);
        }
        chatSearchAdapter.notifyItemRangeChanged(startIndex, 1 + (changedChatCount - 1) * 2);

        startIndex += 1 + (changedChatCount - 1) * 2;

        if (newMessageCount > oldMessageCount) {
          int index = startIndex;
          final int addedChatCount = newMessageCount - oldMessageCount;
          for (int i = 0; i < addedChatCount; i++) {
            TGFoundMessage message = messages.get(i + changedChatCount);
            chatSearchAdapter.getItems().add(index++, new ListItem(ListItem.TYPE_SEPARATOR));
            chatSearchAdapter.getItems().add(index++, searchValueOf(message));
          }
          chatSearchAdapter.notifyItemRangeInserted(startIndex, index - startIndex);
        } else if (newMessageCount < oldMessageCount) {
          final int removedChatCount = oldMessageCount - newMessageCount;
          chatSearchAdapter.removeRange(startIndex, removedChatCount * 2);
        }
      }

      @Override
      public void onRemoveMessages (int oldMessageCount) {
        int startPosition = chatSearchAdapter.indexOfViewById(R.id.search_section_messages);
        if (startPosition != -1) {
          chatSearchAdapter.removeRange(startPosition, 4 + (oldMessageCount - 1) * 2 + 1);
        }
      }

      @Override
      public void onAddMoreMessages (int oldMessageCount, ArrayList<TGFoundMessage> messages) {
        int i = chatSearchAdapter.indexOfViewById(R.id.search_section_messages);
        if (i != -1) {
          final int startIndex = i + 2 + (oldMessageCount - 1) * 2 + 1;
          i = startIndex;
          for (int j = oldMessageCount; j < messages.size(); j++) {
            chatSearchAdapter.getItems().add(i++, new ListItem(ListItem.TYPE_SEPARATOR));
            chatSearchAdapter.getItems().add(i++, searchValueOf(messages.get(j)));
          }
          chatSearchAdapter.notifyItemRangeInserted(startIndex, i - startIndex);
        }
      }

      @Override
      public int getMessagesHeightOffset () {
        int totalHeight = 0;
        for (ListItem item : chatSearchAdapter.getItems()) {
          if (item.getId() == R.id.search_section_messages) {
            break;
          }
          totalHeight += SettingHolder.measureHeightForType(item.getViewType());
        }
        return totalHeight;
      }

      @Override
      public void onHeavyPartReached (final int currentContextId) {
        if (!DEBUG_CHATS_SEARCH_ADAPTER) {
          if (!noChatSearch) {
            tdlib.ui().post(() -> chatSearchView.setItemAnimator(chatSearchViewAnimator));
          }
        }
      }

      @Override
      public void onHeavyPartFinished (final int currentContextId) {
        /*TGDataManager.runOnUiThread(new Runnable() {
          @Override
          public void run () {
            if (currentContextId == chatSearchManager.getContextId()) {
              chatSearchView.setItemAnimator(null);
            }
          }
        }, 360);*/
      }

      @Override
      public void onEndReached () {
        chatSearchAdapter.updateValuedSettingById(R.id.search_counter);
        if (chatSearchAdapter.getItems().isEmpty()) {
          chatSearchAdapter.setItems(new ListItem[] {
                  new ListItem(ListItem.TYPE_EMPTY, 0, 0, R.string.NothingFound)
          }, false);
        }
      }
    });
    chatSearchManager.setSearchFlags(getChatSearchFlags());

    RemoveHelper.attach(chatSearchView, new RemoveHelper.Callback() {
      @Override
      public boolean canRemove (RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, int position) {
        ListItem item = (ListItem) viewHolder.itemView.getTag();
        return item != null && item.getViewType() == ListItem.TYPE_CHAT_BETTER && item.getBoolValue();
      }

      @Override
      public void onRemove (RecyclerView.ViewHolder viewHolder) {
        ListItem item = (ListItem) viewHolder.itemView.getTag();
        final TGFoundChat chat = (TGFoundChat) item.getData();
        removeRecentChat(chat);
      }
    });

    if (parent != null) {
      parent.addView(chatSearchView);
    }
    if (needChatSearchManagerPreparation()) {
      chatSearchManager.onPrepare(getChatMessagesSearchChatList(), getChatSearchInitialQuery());
    }
    return chatSearchView;
  }

  protected final RecyclerView getChatSearchView () {
    return chatSearchView;
  }

  protected String getChatSearchInitialQuery () {
    return "";
  }

  private boolean isSearchAntagonistHidden;

  protected View getSearchAntagonistView () {
    throw new RuntimeException("Stub!");
  }

  private void setSearchAntagonistHidden (boolean isHidden) {
    if (this.isSearchAntagonistHidden != isHidden) {
      this.isSearchAntagonistHidden = isHidden;
      getSearchAntagonistView().setVisibility(isHidden ? View.INVISIBLE : View.VISIBLE);
    }
  }

  protected final boolean isSearchAntagonistHidden () {
    return isSearchAntagonistHidden;
  }

  private boolean isSearchContentVisible;

  private void setSearchContentVisible (boolean isVisible) {
    if (this.isSearchContentVisible != isVisible) {
      this.isSearchContentVisible = isVisible;
      chatSearchView.setScrollDisabled(!isVisible);
      chatSearchViewport.notifyLockValueChanged();
      context().checkDisallowScreenshots();
    }
  }

  private int generateTopCells (List<ListItem> items, int index) {
    final int startCount = items.size();
    ArrayList<TGFoundChat> topChats = chatSearchManager.getTopChats();
    if (topChats != null && topChats.size() > 0) {
      ArrayUtils.ensureCapacity(items, items.size() + 5);
      if (items.isEmpty()) {
        items.add(index++, new ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL));
      }
      items.add(index++, new ListItem(ListItem.TYPE_HEADER, R.id.search_section_top, 0, (getChatSearchFlags() & SearchManager.FLAG_TOP_SEARCH_CATEGORY_GROUPS) != 0 ? R.string.Groups : R.string.People));
      items.add(index++, new ListItem(ListItem.TYPE_SHADOW_TOP));
      items.add(index++, new ListItem(ListItem.TYPE_RECYCLER_HORIZONTAL, R.id.search_top));
      items.add(index++, new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
    }
    return items.size() - startCount;
  }

  private int generateChatsCells (int index, List<ListItem> items, ArrayList<TGFoundChat> chats, @IdRes int headerId, @IdRes int itemId, @StringRes int headerRes, boolean canClear) {
    final int startSize = items.size();
    if (chats != null && chats.size() > 0) {
      ArrayUtils.ensureCapacity(items, items.size() + (chats.size() - 1) * 2 + 5);
      if (items.isEmpty()) {
        items.add(index++, new ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL));
      }
      items.add(index++, new ListItem(canClear ? ListItem.TYPE_HEADER_WITH_ACTION : ListItem.TYPE_HEADER, headerId, R.drawable.baseline_clear_all_24, headerRes));
      items.add(index++, new ListItem(ListItem.TYPE_SHADOW_TOP));
      boolean isFirst = true;
      for (TGFoundChat chat : chats) {
        if (isFirst) {
          isFirst = false;
        } else {
          items.add(index++, new ListItem(ListItem.TYPE_SEPARATOR));
        }
        items.add(index++, searchValueOf(itemId, chat, canClear));
      }
      items.add(index++, new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
    }
    return items.size() - startSize;
  }

  private void generateMessagesCells (List<ListItem> items, ArrayList<TGFoundMessage> messages, int dataStartIndex, int dataCount) {
    final boolean isInitialChunk = dataStartIndex == 0;
    final int startIndex = isInitialChunk ? items.size() : items.size() - 2;
    int index = startIndex;
    boolean addedInitialCell = false;

    if (messages != null && messages.size() > 0) {
      if (isInitialChunk) {
        ArrayUtils.ensureCapacity(items, items.size() + (messages.size() - 1) * 2 + 6);
        if (items.isEmpty()) {
          items.add(index++, new ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL));
          addedInitialCell = true;
        }
      } else {
        ArrayUtils.ensureCapacity(items, items.size() + (messages.size() - 1) + 1);
      }
      if (isInitialChunk) {
        items.add(index++, new ListItem(ListItem.TYPE_HEADER, R.id.search_section_messages, 0, getChatMessagesSearchTitle()));
        items.add(index++, new ListItem(ListItem.TYPE_SHADOW_TOP));
      }
      boolean isFirst = true;
      for (int i = dataStartIndex; i < dataStartIndex + dataCount; i++) {
        TGFoundMessage message = messages.get(i);
        if (isFirst) {
          isFirst = false;
        } else {
          items.add(index++, new ListItem(ListItem.TYPE_SEPARATOR));
        }
        items.add(index++, new ListItem(ListItem.TYPE_CHAT_BETTER, R.id.search_message).setData(message));
      }
    }

    if (isInitialChunk) {
      items.add(index++, new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
      items.add(index++, new ListItem(ListItem.TYPE_LIST_INFO_VIEW, R.id.search_counter));
    }

    final int addedCount = index - startIndex;

    if (addedInitialCell) {
      chatSearchAdapter.notifyItemRemoved(0);
    }
    chatSearchAdapter.notifyItemRangeInserted(startIndex, addedCount);
  }

  protected @StringRes int getChatMessagesSearchTitle () {
    return R.string.general_Messages;
  }

  protected final void forceOpenChatSearch (String query) {
    // enterSearchMode();
    if (chatSearchView.getAdapter() == null)
      chatSearchView.setAdapter(chatSearchAdapter);
    setSearchTransformFactor(1f, true);
    chatSearchManager.onOpen(getChatMessagesSearchChatList());
    // onEnterSearchMode();
    // updateSearchMode(true);
    forceSearchChats(query);
  }

  protected final boolean isChatSearchOpen () {
    return chatSearchManager != null && chatSearchManager.isOpen();
  }

  protected final void forceCloseChatSearch () {
    // updateSearchMode(false);
    // onLeaveSearchMode();
    setSearchTransformFactor(0f, false);
    // leaveSearchMode();
    chatSearchManager.onClose(getChatMessagesSearchChatList());
    chatSearchView.setAdapter(null);
  }

  protected final void forceSearchChats (String query) {
    chatSearchManager.onQueryChanged(getChatMessagesSearchChatList(), query);
  }

  private boolean needPreventKeyboardLag;

  private void setNeedPreventKeyboardLag (boolean needPreventKeyboardLag) {
    if (this.needPreventKeyboardLag != needPreventKeyboardLag) {
      this.needPreventKeyboardLag = needPreventKeyboardLag;
      UI.setSoftInputMode(context, needPreventKeyboardLag ? WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN : Config.DEFAULT_WINDOW_PARAMS);
    }
  }

  @Override
  @CallSuper
  public void onLanguagePackEvent (int event, int arg1) {
    super.onLanguagePackEvent(event, arg1);
    if (chatSearchAdapter != null) {
      chatSearchAdapter.onLanguagePackEvent(event, arg1);
    }
  }

  @Override
  @CallSuper
  protected void onEnterSearchMode () {
    super.onEnterSearchMode();
    if (chatSearchManager != null) {
      chatSearchManager.onOpen(getChatMessagesSearchChatList());
    }
  }

  @Override
  protected void startHeaderTransformAnimator (ValueAnimator animator, int mode, boolean open) {
    if (chatSearchView != null && chatSearchView.getAdapter() == null && mode == HeaderView.TRANSFORM_MODE_SEARCH && open) {
      animator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationStart (Animator animation) {
          onChatSearchOpenStarted();
        }
      });
      AnimatorUtils.startAnimator(chatSearchView, animator, true);
      chatSearchView.setAdapter(chatSearchAdapter);
      return;
    }
    super.startHeaderTransformAnimator(animator, mode, open);
  }

  protected void onChatSearchOpenStarted () { }

  @Override
  @CallSuper
  protected void applySearchTransformFactor (float factor, boolean isOpening) {
    super.applySearchTransformFactor(factor, isOpening);
    if (chatSearchManager != null) {
      chatSearchView.setAlpha(factor);
      setSearchAntagonistHidden(factor == 1f);
      setSearchContentVisible(factor != 0f);
      // setNeedPreventKeyboardLag(factor != 0f && factor != 1f);
    }
  }

  @Override
  public int getRootColorId () {
    return (getSearchTransformFactor() != 0f && chatSearchManager != null) ? ColorId.background : super.getRootColorId();
  }

  protected final void invalidateChatSearchResults () {
    if (chatSearchManager != null) {
      chatSearchManager.reloadSearchResults(getChatMessagesSearchChatList());
    }
  }

  protected final boolean inChatSearchMode () {
    return chatSearchManager != null;
  }

  @Override
  protected boolean useGraySearchHeader () {
    return chatSearchManager != null || super.useGraySearchHeader();
  }

  @Override
  @CallSuper
  protected void onSearchInputChanged (String input) {
    super.onSearchInputChanged(input);
    if (chatSearchManager != null) {
      chatSearchManager.onQueryChanged(getChatMessagesSearchChatList(), input);
    }
  }

  @Override
  @CallSuper
  protected final boolean needPreventiveKeyboardHide () {
    return inSearchMode() && inChatSearchMode();
  }

  @Override
  protected void onAfterLeaveSearchMode () {
    super.onAfterLeaveSearchMode();
    if (chatSearchManager != null) {
      chatSearchManager.onClose(getChatMessagesSearchChatList());
      clearSearchInput();
    }
  }
}
