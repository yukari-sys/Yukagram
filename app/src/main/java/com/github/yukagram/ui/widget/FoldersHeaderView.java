package com.github.yukagram.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
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

import com.github.yukagram.ui.main_controller.DialogsController;

import org.jetbrains.annotations.NotNull;
import org.thunderdog.challegram.FillingDrawable;
import org.thunderdog.challegram.Log;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.support.ViewSupport;
import org.thunderdog.challegram.theme.ColorId;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.theme.ThemedColorAnimator;
import org.thunderdog.challegram.tool.Fonts;
import org.thunderdog.challegram.tool.Paints;
import org.thunderdog.challegram.tool.Screen;
import org.thunderdog.challegram.widget.NoScrollTextView;
import org.thunderdog.challegram.widget.ShadowView;

import java.util.ArrayList;
import java.util.List;

import me.vkryl.android.AnimatorUtils;
import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.FactorAnimator;
import me.vkryl.android.animatorx.AnimatorListener;
import me.vkryl.android.animatorx.FloatAnimator;
import me.vkryl.android.widget.FrameLayoutFix;
import me.vkryl.core.ColorUtils;
import me.vkryl.core.MathUtils;

/*
* Pill-alike bottom bar for emoji keyboard
*/

public class FoldersHeaderView extends FrameLayoutFix implements AnimatorListener<@NotNull Float> {
  private int currentSelectedIndex = 0;
  private List<Button> buttons;
  private LinearLayout linearLayout;
  private DialogsController parent;
  private FloatAnimator pillAnimator;

  public FoldersHeaderView(@NonNull Context context) {
    super(context);
  }

  public void init(ViewController<?> themeProvider, int currentSelectedIndex, DialogsController parent) {
    this.currentSelectedIndex = currentSelectedIndex;
    this.parent = parent;

    linearLayout = new LinearLayout(getContext()) {
      @Override
      protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        onPillDraw(canvas);
      }
    };

    buttons = new ArrayList<>();

    ViewSupport.setThemedBackground(linearLayout, ColorId.background, themeProvider);
//    themeProvider.addThemeBackgroundColorListener(linearLayout, ColorId.headerBackground);

    linearLayout.setPadding(Screen.dp(10f), Screen.dp(5f), Screen.dp(10f), Screen.dp(5f));

    LayoutParams layoutParams = FrameLayoutFix.newParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    );
    layoutParams.setMargins(0, 0, 0, Screen.dp(5));

    linearLayout.setOutlineProvider(new ViewOutlineProvider() {
      @Override
      public void getOutline(View view, Outline outline) {
        outline.setRect(
          0, 0, view.getWidth(), view.getHeight()
        );
      }
    });

    linearLayout.setElevation(3);

    addView(linearLayout, layoutParams);

    pillAnimator = new FloatAnimator(200, AnimatorUtils.DECELERATE_INTERPOLATOR, currentSelectedIndex, this);

    setLayoutParams(FrameLayoutFix.newParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.FILL_HORIZONTAL
    ));

    setVisibility(View.GONE);
  }

  public RectF getButtonPill(int index) {
    Button b = buttons.get(index);
    return new RectF(
      b.getLeft() + b.getPaddingLeft() - Screen.dp(3),
      b.getBottom() + Screen.dp(2),
      b.getRight() - b.getPaddingRight() + Screen.dp(3),
      b.getBottom() + Screen.dp(15)
    );
  }

  public RectF rectFLerp(RectF a, RectF b, float factor) {
    return new RectF(
      MathUtils.fromTo(a.left, b.left, factor),
      MathUtils.fromTo(a.top, b.top, factor),
      MathUtils.fromTo(a.right, b.right, factor),
      MathUtils.fromTo(a.bottom, b.bottom, factor)
    );
  }

  @Override
  public void onAnimationUpdate(@NotNull Float newValue) {
    int floor = (int) Math.floor(newValue);
    int ceiling = (int) Math.ceil(newValue);

    if (floor == ceiling) {
      pillRect = getButtonPill(floor);
    } else {
      float factor = newValue - floor;
      pillRect = rectFLerp(getButtonPill(floor), getButtonPill(ceiling), factor);
    }
    linearLayout.postInvalidateOnAnimation();
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

      buttons.get(currentSelectedIndex).post(() -> {
        pillRect = getButtonPill(currentSelectedIndex);
        linearLayout.postInvalidateOnAnimation();
      });
    }
  }

  public void addButton(ViewController<?> themeProvider, int index, String folder) {
    LayoutParams params = FrameLayoutFix.newParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.LEFT | Gravity.CENTER_HORIZONTAL
    );
    params.setMargins(index != 0 ? Screen.dp(10f) : 0, 0, 0, 0);

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

  public void setSelectedIndex (int index) {
    if (index == currentSelectedIndex) {
      return;
    }

    buttons.get(currentSelectedIndex).getAnimator().setValue(false, true);
    buttons.get(index).getAnimator().setValue(true, true);

    currentSelectedIndex = index;
    pillAnimator.setAnimatedValue((float) index);
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

  static final int inactiveColor = ColorId.fillingActiveContent;
  static final int activeColor = ColorId.textNeutral;

  private RectF pillRect = new RectF();

  protected void onPillDraw(Canvas canvas) {
    Paint p = new Paint();
    p.setColor(Theme.getColor(activeColor));
    canvas.drawRoundRect(pillRect, Screen.dp(5f), Screen.dp(5f), p);
  }

  public static class Button extends NoScrollTextView implements FactorAnimator.Target {
    public Button(Context context) {
      super(context);
    }

    private BoolAnimator animator;

    private ViewController<?> themeProvider;

    public void init(ViewController<?> themeProvider, boolean selected) {
      this.themeProvider = themeProvider;

      setTypeface(Fonts.getRobotoMedium());
      setSingleLine();
      setGravity(Gravity.LEFT);
      setEllipsize(TextUtils.TruncateAt.END);
      setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f);
      setPadding(Screen.dp(12), Screen.dp(3), Screen.dp(12), Screen.dp(3));

      int textColor = selected ? activeColor : inactiveColor;

      setTextColor(Theme.getColor(textColor));
      themeProvider.addOrUpdateThemeTextColorListener(this, textColor);

      animator = new BoolAnimator(0, this, AnimatorUtils.DECELERATE_INTERPOLATOR, 200l, selected);
    }

    public BoolAnimator getAnimator() {
      return animator;
    }

    @Override
    public void onFactorChanged(int id, float factor, float fraction, FactorAnimator callee) {
      setTextColor(ColorUtils.fromToArgb(
        Theme.getColor(inactiveColor),
        Theme.getColor(activeColor),
        factor
      ));
    }

    @Override
    public void onFactorChangeFinished(int id, float finalFactor, FactorAnimator callee) {
      themeProvider.addOrUpdateThemeTextColorListener(this, finalFactor == 1 ? activeColor : inactiveColor);
    }
  }
}
