package com.github.yukagram.ui.widget;

import android.content.Context;
import android.graphics.Outline;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.yukagram.ui.main_controller.DialogsController;

import org.thunderdog.challegram.FillingDrawable;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.support.ViewSupport;
import org.thunderdog.challegram.theme.ColorId;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.tool.Fonts;
import org.thunderdog.challegram.tool.Screen;
import org.thunderdog.challegram.widget.NoScrollTextView;
import org.thunderdog.challegram.widget.ShadowView;

import java.util.ArrayList;
import java.util.List;

import me.vkryl.android.AnimatorUtils;
import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.FactorAnimator;
import me.vkryl.android.widget.FrameLayoutFix;

/*
* Pill-alike bottom bar for emoji keyboard
*/

public class FoldersHeaderView extends FrameLayoutFix {
  private int currentSelectedIndex = 0;
  private List<Button> buttons;
  private LinearLayout linearLayout;
  private DialogsController parent;

  public FoldersHeaderView(@NonNull Context context) {
    super(context);
  }

  public void fillButtons(ViewController<?> themeProvider, int mainChatListPosition, List<String> folders) {
    int i = 0;
    for (String folder : folders) {
      if (i == mainChatListPosition) {
        addButton(themeProvider, i++, Lang.getString(R.string.yu_All));
      }
      addButton(themeProvider, i++, folder);
    }

    if (!buttons.isEmpty()) {
      setVisibility(View.VISIBLE);
    }
  }

  public void addButton(ViewController<?> themeProvider, int index, String folder) {
    LayoutParams params = FrameLayoutFix.newParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT,
      Gravity.LEFT | Gravity.CENTER_HORIZONTAL
    );
    params.setMargins(index != 0 ? Screen.dp(2f) : 0, 0, 0, 0);

    Button title = new Button(getContext());
    title.init(themeProvider, currentSelectedIndex == index);
    title.setId(index);
    title.setText(folder);
    title.setLayoutParams(params);
    title.setOnClickListener(onClickListener);
    title.setOnLongClickListener(onLongClickListener);

    linearLayout.addView(title);
    buttons.add(title);

    if (buttons.size() == 1) {
      setVisibility(View.VISIBLE);
    }
  }

  public void clear() {
    buttons.clear();
    linearLayout.removeAllViews();
  }

  public void removeButton(int index) {
    buttons.remove(index);
    linearLayout.removeViewAt(index);

    if (buttons.isEmpty()) {
      setVisibility(View.GONE);
    }
  }

  public void init(ViewController<?> themeProvider, int currentSelectedIndex, DialogsController parent) {
    this.currentSelectedIndex = currentSelectedIndex;
    this.parent = parent;

    linearLayout = new LinearLayout(getContext());

    buttons = new ArrayList<>();

    float linearLayoutCornerRadius = 15 + 5; // innerRadius + padding = outerRadius

    ViewSupport.setThemedBackground(linearLayout, ColorId.background, themeProvider)
            .setCornerRadius(linearLayoutCornerRadius);
//    themeProvider.addThemeBackgroundColorListener(linearLayout, ColorId.headerBackground);

    linearLayout.setPadding(Screen.dp(5f), Screen.dp(5f), Screen.dp(5f), Screen.dp(5f));

    LayoutParams layoutParams = FrameLayoutFix.newParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT,
      Gravity.CENTER_HORIZONTAL | Gravity.FILL_VERTICAL
    );
    layoutParams.setMargins(0, 0, 0, Screen.dp(5));

    linearLayout.setOutlineProvider(new ViewOutlineProvider() {
      @Override
      public void getOutline(View view, Outline outline) {
        outline.setRoundRect(
          0, 0, view.getWidth(), view.getHeight(),
          Screen.dp(linearLayoutCornerRadius)
        );
      }
    });

    linearLayout.setElevation(3);

    addView(linearLayout, layoutParams);

    setLayoutParams(FrameLayoutFix.newParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT,
      Gravity.TOP | Gravity.FILL_HORIZONTAL
    ));

    setVisibility(View.GONE);
  }

  public void setSelectedIndex (int index) {
    if (index == currentSelectedIndex) {
      return;
    }

    buttons.get(currentSelectedIndex).getAnimator().setValue(false, true);
    buttons.get(index).getAnimator().setValue(true, true);

    currentSelectedIndex = index;
  }

  OnClickListener onClickListener;
  OnLongClickListener onLongClickListener;

  public void setOnClickListener (OnClickListener onClickListener) {
    this.onClickListener = onClickListener;
    for (TextView element : buttons) {
      element.setOnClickListener(onClickListener);
    }
    linearLayout.setOnClickListener(onClickListener);
  }

  public void setOnLongClickListener (OnLongClickListener onLongClickListener) {
    this.onLongClickListener = onLongClickListener;
    for (TextView element : buttons) {
      element.setOnLongClickListener(onLongClickListener);
    }
  }

  public static class Button extends NoScrollTextView implements FactorAnimator.Target {
    public Button(Context context) {
      super(context);
    }

    private BoolAnimator animator;

    private FillingDrawable background;

    public void init(ViewController<?> themeProvider, boolean selected) {
      setTypeface(Fonts.getRobotoMedium());
      setSingleLine();
      setGravity(Gravity.LEFT);
      setEllipsize(TextUtils.TruncateAt.END);
      setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f);
      setPadding(Screen.dp(10), Screen.dp(3), Screen.dp(10), Screen.dp(3));

      setTextColor(Theme.getColor(ColorId.circleButtonOverlayIcon));
      themeProvider.addOrUpdateThemeTextColorListener(this, ColorId.circleButtonOverlayIcon);

      background = ViewSupport.setThemedBackground(this, ColorId.circleButtonOverlay, themeProvider);
      background.setCornerRadius(15f);
      if (!selected)
        background.setAlphaFactor(0);

      animator = new BoolAnimator(0, this, AnimatorUtils.DECELERATE_INTERPOLATOR, 200l, selected);
    }

    public BoolAnimator getAnimator() {
      return animator;
    }

    @Override
    public void onFactorChanged(int id, float factor, float fraction, FactorAnimator callee) {
      background.setAlphaFactor(factor);
      background.setCornerRadius(15f * factor);
    }
  }
}
