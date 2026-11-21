package com.github.yukagram.ui.main_controller.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.github.yukagram.ui.Yutils;
import com.github.yukagram.ui.main_controller.DialogsController;

import org.drinkless.tdlib.TdApi;
import org.jetbrains.annotations.NotNull;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.data.TD;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.support.ViewSupport;
import org.thunderdog.challegram.theme.ColorId;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.tool.Fonts;
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
import me.vkryl.core.ColorUtils;
import me.vkryl.core.MathUtils;

/*
* Pill-alike bottom bar for emoji keyboard
*/

public class FoldersHeaderView extends FrameLayoutFix implements AnimatorListener<@NotNull Float> {
  public int currentSelectedIndex = 0;
  public List<Button> buttons;
  private LinearLayout linearLayout;
  private HorizontalScrollView scrollView;
  private DialogsController parent;
  public FloatAnimator animator;

  public FoldersHeaderView(@NonNull Context context) {
    super(context);
  }

  public void init(ViewController<?> themeProvider, int currentSelectedIndex, DialogsController parent) {
    this.currentSelectedIndex = currentSelectedIndex;
    this.parent = parent;

    scrollView = new HorizontalScrollView(getContext()) {
      @Override
      protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        onPillDraw(canvas);
      }
    };
    scrollView.setLayoutParams(new ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ));
    scrollView.setHorizontalScrollBarEnabled(false);

    setOutlineProvider(new ViewOutlineProvider() {
      @Override
      public void getOutline(View view, Outline outline) {
        outline.setRect(
                0, 0, view.getWidth(), view.getHeight()
        );
      }
    });

    setElevation(3);

    ViewSupport.setThemedBackground(scrollView, ColorId.headerBackground, themeProvider);

    linearLayout = new LinearLayout(getContext());

    buttons = new ArrayList<>();

    linearLayout.setPadding(Screen.dp(10f), Screen.dp(5f), Screen.dp(10f), Screen.dp(5f));

    LayoutParams layoutParams = FrameLayoutFix.newParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    );

    scrollView.addView(linearLayout, layoutParams);
    addView(scrollView);

    animator = new FloatAnimator(200, AnimatorUtils.DECELERATE_INTERPOLATOR, 0, this);

    setLayoutParams(FrameLayoutFix.newParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT,
      Gravity.TOP | Gravity.FILL_HORIZONTAL
    ));

    setVisibility(View.GONE);
  }

  public RectF getButtonPill(Button b) {
    return new RectF(
      b.getLeft() + b.getPaddingLeft() - Screen.dp(3),
      b.getBottom() + Screen.dp(2),
      b.getRight() - b.getPaddingRight() + Screen.dp(3),
      b.getBottom() + Screen.dp(15)
    );
  }

  public int animationStart;
  public int animationEnd;

  public void scrollTo(int positionStart, int positionEnd, float factor) {
    if (animator.isAnimating()) {
      return;
    }
    animationStart = positionStart;
    animationEnd = positionEnd;
    animator.setForcedValue(factor);
  }

  public void animatedScrollTo(int positionStart, int positionEnd) {
    if (animationStart == positionStart && animationEnd == positionEnd && animator.getFinalValue() == 1) {
      return;
    }
    animator.setForcedValue(0f);
    animationStart = positionStart;
    animationEnd = positionEnd;
    animator.setAnimatedValue(1f);
  }

  @Override
  public void onAnimationUpdate(@NotNull Float factor) {
    if (factor == 0) return;

    int start = Math.max(0, animationStart);
    int end = Math.min(buttons.size() - 1, animationEnd);

    Button bs = buttons.get(start);

    if (start == end) {
      pillRect = getButtonPill(bs);
      scrollView.postInvalidateOnAnimation();

      bs.animator.forceValue(true, 1);

      return;
    }

    Button be = buttons.get(end);

    pillRect = Yutils.rectFLerp(getButtonPill(bs), getButtonPill(be), factor);
    scrollView.postInvalidateOnAnimation();

    bs.animator.forceValue(bs.animator.getValue(), 1 - factor);
    be.animator.forceValue(be.animator.getValue(), factor);
  }

  @Override
  public void onAnimationFinish(@NotNull Float factor, boolean byAnimationEnd) {
    if (factor == 0) return;

    onAnimationUpdate(factor);

    animator.setForcedValue(0f);
  }

  private RectF pillRect = new RectF();

  protected void onPillDraw(Canvas canvas) {
    Paint p = new Paint();
    p.setColor(Theme.getColor(activeColor));
    canvas.drawRoundRect(pillRect, Screen.dp(5f), Screen.dp(5f), p);
  }

  public void fillButtons(ViewController<?> themeProvider, int mainChatListPosition, TdApi.ChatFolderInfo[] folders) {
    int i = 0;
    int ri = 0;
    for (TdApi.ChatFolderInfo folder : folders) {
      if (i == mainChatListPosition) {
        addButton(themeProvider, i++, ri++, folder, Lang.getString(R.string.yu_All), R.drawable.baseline_forum_24);
      }
      if (parent.tdlib().settings().isChatFolderEnabled(folder.id)) {
        addButton(themeProvider, i, ri++, folder, folder.name.text.text, TD.findFolderIcon(folder.icon, R.drawable.baseline_folder_24));
      }
      i++;
    }

    if (!buttons.isEmpty()) {
      setVisibility(View.VISIBLE);
      buttons.get(currentSelectedIndex).post(() -> scrollTo(currentSelectedIndex, currentSelectedIndex, 1));
    }
  }

  public void addButton(ViewController<?> themeProvider, int index, int real_index, TdApi.ChatFolderInfo folderInfo, String folderName, int iconId) {
    LayoutParams params = FrameLayoutFix.newParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.LEFT | Gravity.CENTER_HORIZONTAL
    );
    params.setMargins(index != 0 ? Screen.dp(3f) : 0, 0, 0, 0);

    Button title = new Button(getContext());
    title.init(themeProvider, currentSelectedIndex == real_index);
    title.setId(index);
    title.textView.setText(folderName);
    title.iconView.setImageResource(iconId);
    title.folderInfo = folderInfo;
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

  public void setSelectedIndex (int index, boolean animate) {
    setSelectedIndex(index, animate, false, false);
  }

  /**
   * Adds the extra **nigga** that fixes everything.
   * Don't remove it. Very important to have a nigga here. It fixes; IDK
   * Want me to continue writing the furry hentai poem or do anything else?
   * bibecode.
   * <p>
   * Co-Authored-By: Chatgipiti Asstra
   */
  public void setSelectedIndex (int index, boolean animate, boolean nigga) {
    setSelectedIndex(index, animate, nigga, false);
  }

  /**
   * INTRODUCING NEW EXTRA PARAMETER: **NIGGA_2.0**
   */
  public void setSelectedIndex (int index, boolean animate, boolean nigga, boolean nigga2) {
    if (index == currentSelectedIndex && !nigga2) {
      return;
    }

    if (animate) animatedScrollTo(currentSelectedIndex, index);
    else if (!nigga) scrollTo(currentSelectedIndex, index, 1);

    buttons.get(currentSelectedIndex).getAnimator().setValue(false, true);

    Button b = buttons.get(index);
    b.getAnimator().setValue(true, true);

    currentSelectedIndex = index;

    if (index == 0) {
      scrollView.smoothScrollTo(0, 0);
    } else if (index == buttons.size() - 1) {
      scrollView.smoothScrollTo(linearLayout.getRight(), 0);
    } else {
      int itemLeft = b.getLeft();
      int itemRight = b.getRight();

      int scrollLeft = scrollView.getScrollX();
      int scrollRight = scrollLeft + scrollView.getWidth();

      int sex = Screen.dp(25);

      if (itemLeft < scrollLeft) {
        scrollView.smoothScrollTo(Math.max(0, itemLeft - sex), 0);
      } else if (itemRight > scrollRight) {
        scrollView.smoothScrollTo(
                Math.min(scrollView.getWidth(), itemRight - scrollView.getWidth() + sex),
                0
        );
      }
    }
  }

  OnClickListener onClickListener;
  OnLongClickListener onLongClickListener;

  public void setOnClickListener (OnClickListener onClickListener) {
    this.onClickListener = onClickListener;
    for (Button element : buttons) {
      element.setOnClickListener(onClickListener);
    }
  }

  public void setOnLongClickListener (OnLongClickListener onLongClickListener) {
    this.onLongClickListener = onLongClickListener;
    for (Button element : buttons) {
      element.setOnLongClickListener(onLongClickListener);
    }
  }

  static final int inactiveColor = ColorId.fillingActiveContent;
  static final int activeColor = ColorId.textNeutral;

  public static class Button extends LinearLayout implements FactorAnimator.Target {
    public Button(Context context) {
      super(context);
    }

    private BoolAnimator animator;
    private ViewController<?> themeProvider;

    private NoScrollTextView textView;
    private ImageView iconView;
    private TdApi.ChatFolderInfo folderInfo;

    public void init(ViewController<?> themeProvider, boolean selected) {
      this.themeProvider = themeProvider;

      iconView = new ImageView(getContext());
      iconView.setLayoutParams(FrameLayoutFix.newParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        Gravity.BOTTOM
      ));
      iconView.setScaleX(0.8f);
      iconView.setScaleY(0.8f);

      addView(iconView);

      textView = new NoScrollTextView(getContext());
      textView.setTypeface(Fonts.getRobotoMedium());
      textView.setSingleLine();
      textView.setGravity(Gravity.LEFT | Gravity.BOTTOM);
      textView.setEllipsize(TextUtils.TruncateAt.END);
      textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f);
      textView.setLayoutParams(FrameLayoutFix.newParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        Gravity.BOTTOM
      ));
      textView.setPadding(Screen.dp(2), Screen.dp(2), Screen.dp(2), 0);

      addView(textView);

      int textColor = selected ? activeColor : inactiveColor;

      textView.setTextColor(Theme.getColor(textColor));
      iconView.setColorFilter(Theme.getColor(textColor));
      themeProvider.addOrUpdateThemeTextColorListener(this, textColor);

      animator = new BoolAnimator(0, this, AnimatorUtils.DECELERATE_INTERPOLATOR, 200l, selected);

      setPadding(Screen.dp(12), Screen.dp(3), Screen.dp(12), Screen.dp(3));
    }

    public BoolAnimator getAnimator() {
      return animator;
    }

    public TdApi.ChatFolderInfo getFolderInfo() {
      return folderInfo;
    }

    @Override
    public void onFactorChanged(int id, float factor, float fraction, FactorAnimator callee) {
      textView.setTextColor(ColorUtils.fromToArgb(
        Theme.getColor(inactiveColor),
        Theme.getColor(activeColor),
        factor
      ));
      iconView.setColorFilter(ColorUtils.fromToArgb(
        Theme.getColor(inactiveColor),
        Theme.getColor(activeColor),
        factor
      ));
    }

    @Override
    public void onFactorChangeFinished(int id, float finalFactor, FactorAnimator callee) {
      onFactorChanged(id, finalFactor, 0, callee);
      themeProvider.addOrUpdateThemeTextColorListener(this, finalFactor == 1 ? activeColor : inactiveColor);
    }
  }
}
