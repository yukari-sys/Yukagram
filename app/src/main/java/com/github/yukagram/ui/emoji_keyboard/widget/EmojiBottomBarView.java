package com.github.yukagram.ui.emoji_keyboard.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.RectKt;

import com.github.yukagram.ui.Yutils;

import org.jetbrains.annotations.NotNull;
import org.thunderdog.challegram.FillingDrawable;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.support.ViewSupport;
import org.thunderdog.challegram.theme.ColorId;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.tool.Fonts;
import org.thunderdog.challegram.tool.Paints;
import org.thunderdog.challegram.tool.Screen;
import org.thunderdog.challegram.widget.NoScrollTextView;

import java.util.ArrayList;
import java.util.List;

import me.vkryl.android.AnimatorUtils;
import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.FactorAnimator;
import me.vkryl.android.animatorx.AnimatorListener;
import me.vkryl.android.animatorx.FloatAnimator;
import me.vkryl.android.widget.FrameLayoutFix;

/*
* Pill-alike bottom bar for emoji keyboard
*/

public class EmojiBottomBarView extends FrameLayoutFix implements FactorAnimator.Target, AnimatorListener<@NotNull Float> {
  private int currentSelectedIndex = -1;
  private List<Button> buttons;
  private LinearLayout linearLayout;
  private BoolAnimator hidden;
  public FloatAnimator animator;

  public EmojiBottomBarView(@NonNull Context context) {
    super(context);
  }

  public void init(ViewController<?> themeProvider, int currentSelectedIndex, boolean allowMedia, boolean hasGifs) {
    this.currentSelectedIndex = currentSelectedIndex;

    hidden = new BoolAnimator(0, this, AnimatorUtils.DECELERATE_INTERPOLATOR, 200L);
    animator = new FloatAnimator(200L, AnimatorUtils.DECELERATE_INTERPOLATOR, currentSelectedIndex, this);

    linearLayout = new LinearLayout(getContext()) {
      @Override
      protected void dispatchDraw(Canvas canvas) {
        drawPill(canvas);
        super.dispatchDraw(canvas);
      }
    };

    buttons = new ArrayList<>();

    FrameLayoutFix.LayoutParams elementParams = FrameLayoutFix.newParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT,
      Gravity.LEFT | Gravity.CENTER_HORIZONTAL
    );

    // what a schizo 🔥🔥🔥🙏🙏
    for (int[] ids : new int[][] {
      {R.id.btn_tabEmojis,   R.string.yu_TabEmojis,   0,                  Screen.dp(2f), },
      {R.id.btn_tabGifs,     R.string.yu_TabGifs,     0,                  0,                  },
      {R.id.btn_tabStickers, R.string.yu_TabStickers, Screen.dp(2f), 0,                  },
    }) {
      Button title = new Button(getContext());

      title.init(themeProvider);

      title.setId(ids[0]);
      title.setText(ids[1]);
      elementParams.setMargins(ids[2], 0, ids[3], 0);
      title.setLayoutParams(elementParams);

      linearLayout.addView(title);
      buttons.add(title);
    }

    float linearLayoutCornerRadius = 15 + 5; // innerRadius + padding = outerRadius

    ViewSupport.setThemedBackground(linearLayout, ColorId.background, themeProvider)
      .setCornerRadius(linearLayoutCornerRadius);
//    themeProvider.addThemeBackgroundColorListener(linearLayout, ColorId.headerBackground);

    linearLayout.setPadding(Screen.dp(5f), Screen.dp(5f), Screen.dp(5f), Screen.dp(5f));

    FrameLayoutFix.LayoutParams layoutParams = FrameLayoutFix.newParams(
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
      Gravity.BOTTOM | Gravity.FILL_HORIZONTAL
    ));

    setAllowMedia(allowMedia);
  }

  public void setAllowMedia(boolean value) {
    hidden.setValue(!value, true);
  }

  public void setSelectedIndex (int index) {
    if (index == currentSelectedIndex) {
      return;
    }

    animator.setAnimatedValue((float) index);

    currentSelectedIndex = index;
  }

  public void setOnClickListener (OnClickListener onClickListener) {
    for (TextView element : buttons) {
      element.setOnClickListener(onClickListener);
    }
    linearLayout.setOnClickListener(onClickListener);
  }

  public void setOnLongClickListener (OnLongClickListener onLongClickListener) {
    for (TextView element : buttons) {
      element.setOnLongClickListener(onLongClickListener);
    }
  }

  public void setHidden(boolean value) {
    hidden.setValue(value, true);
  }

  RectF pillRect;

  public void drawPill(Canvas canvas) {
    if (pillRect == null) pillRect = getButtonRect(currentSelectedIndex);
    Paint p = new Paint();
    p.setColor(Theme.getColor(ColorId.circleButtonOverlay));
//    canvas.drawRoundRect(0, 0, canvas.getWidth(), canvas.getHeight(), 15, 15, );
    canvas.drawRoundRect(pillRect, Screen.dp(15), Screen.dp(15), p);
  }

  public RectF getButtonRect(int index) {
    Rect rect = new Rect();
    buttons.get(index).getHitRect(rect);
    return RectKt.toRectF(rect);
  }

  @Override
  public void onFactorChanged(int id, float factor, float fraction, FactorAnimator callee) {
    setTranslationY(factor * (getMeasuredHeight() + Screen.dp(10)));
  }

  @Override
  public void onAnimationUpdate(@NotNull Float value) {
    int floor = Math.clamp((int) Math.floor(value), 0, buttons.size() - 1);
    int ceiling = Math.clamp((int) Math.ceil(value), 0, buttons.size() - 1);
    float factor = Math.clamp(value - floor, 0, 1);

    pillRect = Yutils.rectFLerp(getButtonRect(floor), getButtonRect(ceiling), factor);
    linearLayout.postInvalidateOnAnimation();
  }

  public static class Button extends NoScrollTextView {
    public Button(Context context) {
      super(context);
    }

    public void init(ViewController<?> themeProvider) {
      setTypeface(Fonts.getRobotoMedium());
      setSingleLine();
      setGravity(Gravity.LEFT);
      setEllipsize(TextUtils.TruncateAt.END);
      setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f);
      setPadding(Screen.dp(10), Screen.dp(3), Screen.dp(10), Screen.dp(3));

      setTextColor(Theme.getColor(ColorId.circleButtonOverlayIcon));
      themeProvider.addOrUpdateThemeTextColorListener(this, ColorId.circleButtonOverlayIcon);
    }
  }
}
