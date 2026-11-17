package com.github.yukagram.ui.widget;

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
  private int currentSelectedIndex = 0;
  private List<Button> buttons;
  private LinearLayout linearLayout;
  private HorizontalScrollView scrollView;
  private DialogsController parent;
  private FloatAnimator pillAnimator;

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

    scrollView.setOutlineProvider(new ViewOutlineProvider() {
      @Override
      public void getOutline(View view, Outline outline) {
        outline.setRect(
                0, 0, view.getWidth(), view.getHeight()
        );
      }
    });

    scrollView.setElevation(3);

    ViewSupport.setThemedBackground(scrollView, ColorId.background, themeProvider);

    linearLayout = new LinearLayout(getContext());

    buttons = new ArrayList<>();

    linearLayout.setPadding(Screen.dp(10f), Screen.dp(5f), Screen.dp(10f), Screen.dp(5f));

    LayoutParams layoutParams = FrameLayoutFix.newParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    );

    scrollView.addView(linearLayout, layoutParams);
    addView(scrollView);

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
    scrollView.postInvalidateOnAnimation();
  }

  private RectF pillRect = new RectF();

  protected void onPillDraw(Canvas canvas) {
    Paint p = new Paint();
    p.setColor(Theme.getColor(activeColor));
    canvas.drawRoundRect(pillRect, Screen.dp(5f), Screen.dp(5f), p);
  }

  public void fillButtons(ViewController<?> themeProvider, int mainChatListPosition, TdApi.ChatFolderInfo[] folders) {
    int i = 0;
    for (TdApi.ChatFolderInfo folder : folders) {
      if (i == mainChatListPosition) {
        addButton(themeProvider, i++, Lang.getString(R.string.yu_All), R.drawable.baseline_forum_24);
      }
      addButton(themeProvider, i++, folder.name.text.text, TD.findFolderIcon(folder.icon, R.drawable.baseline_folder_24));
    }

    if (!buttons.isEmpty()) {
      setVisibility(View.VISIBLE);

      buttons.get(currentSelectedIndex).post(() -> {
        pillRect = getButtonPill(currentSelectedIndex);
        scrollView.postInvalidateOnAnimation();
      });
    }
  }

  public void addButton(ViewController<?> themeProvider, int index, String folderName, int iconId) {
    LayoutParams params = FrameLayoutFix.newParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.LEFT | Gravity.CENTER_HORIZONTAL
    );
    params.setMargins(index != 0 ? Screen.dp(3f) : 0, 0, 0, 0);

    Button title = new Button(getContext());
    title.init(themeProvider, currentSelectedIndex == index);
    title.setId(index);
    title.textView.setText(folderName);
    title.iconView.setImageResource(iconId);
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

    Button b = buttons.get(index);

    buttons.get(currentSelectedIndex).getAnimator().setValue(false, true);
    b.getAnimator().setValue(true, true);

    currentSelectedIndex = index;
    pillAnimator.setAnimatedValue((float) index);

    int itemLeft = b.getLeft();
    int itemRight = b.getRight();

    int scrollLeft = scrollView.getScrollX();
    int scrollRight = scrollLeft + scrollView.getWidth();

    if (itemLeft < scrollLeft) {
      scrollView.smoothScrollTo(itemLeft, 0);
    } else if (itemRight > scrollRight) {
      scrollView.smoothScrollTo(
        itemRight - scrollView.getWidth(),
        0
      );
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
      themeProvider.addOrUpdateThemeTextColorListener(this, finalFactor == 1 ? activeColor : inactiveColor);
    }
  }
}
