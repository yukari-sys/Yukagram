package com.github.yukagram.ui.main_controller;

import static org.thunderdog.challegram.telegram.TdlibManager.getTdlib;

import android.content.Context;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.navigation.HeaderView;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.telegram.ConnectionState;
import org.thunderdog.challegram.telegram.GlobalAccountListener;
import org.thunderdog.challegram.telegram.GlobalConnectionListener;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.telegram.TdlibAccount;
import org.thunderdog.challegram.telegram.TdlibManager;
import org.thunderdog.challegram.telegram.TdlibUi;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.tool.Fonts;
import org.thunderdog.challegram.tool.Screen;
import org.thunderdog.challegram.unsorted.Size;
import org.thunderdog.challegram.widget.EmojiTextView;

import me.vkryl.android.AnimatorUtils;
import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.FactorAnimator;
import me.vkryl.android.widget.FrameLayoutFix;

public class ConnectionTitleController extends ViewController<MainControllerV2> implements
        FactorAnimator.Target, GlobalConnectionListener, GlobalAccountListener {

    private FrameLayout wrapper;
    private TextView defaultTitle; // 0
    private TextView connectionTitle; // 1
    private BoolAnimator animator; // is connection title

    private String connectionTitleText;
    private String defaultTitleText;

    public ConnectionTitleController(@NonNull Context context, Tdlib tdlib) {
        super(context, tdlib);
    }

    public void init() {
        animator = new BoolAnimator(0, this, AnimatorUtils.DECELERATE_INTERPOLATOR, 200, false) {
            @Override
            public void onFactorChangeFinished(int id, float finalFactor, FactorAnimator callee) {
                super.onFactorChangeFinished(id, finalFactor, callee);
            }
        };

        defaultTitleText = getArguments().getDefaultTitle();
        connectionTitleText = Lang.getString(R.string.Connection);

        TdlibManager.instance().global().addAccountListener(this);
        TdlibManager.instance().global().addConnectionListener(this);
    }

    public TextView genTextView() {
        FrameLayoutFix.LayoutParams params = FrameLayoutFix.newParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | (Lang.rtl() ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        params.topMargin = Screen.dp(15);
        EmojiTextView text = new EmojiTextView(context);
        text.setScrollDisabled(true);
        text.setTag(this);
        text.setMovementMethod(LinkMovementMethod.getInstance());
        text.setHighlightColor(Theme.textLinkHighlightColor());
        text.setTypeface(Fonts.getRobotoMedium());
        text.setGravity(Gravity.LEFT);
        text.setSingleLine();
        text.setEllipsize(TextUtils.TruncateAt.END);
        text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19f);
        text.setTextColor(0xffffffff);
        text.setLayoutParams(params);
        return text;
    }

    public int getHeaderHeight() {
        return Size.getHeaderDrawerSize() + Screen.dp(10);
    }

    @Override
    protected View onCreateView(Context context) {
        wrapper = new FrameLayout(context);

        FrameLayoutFix.LayoutParams params = FrameLayoutFix.newParams(ViewGroup.LayoutParams.WRAP_CONTENT, getHeaderHeight());
        params.leftMargin = Screen.dp(68f);

        wrapper.setLayoutParams(params);

        defaultTitle = genTextView();
        defaultTitle.setText(defaultTitleText);
        defaultTitle.setTranslationY(animator.getValue() ? -getHeaderHeight() : 0);

        wrapper.addView(defaultTitle);

        connectionTitle = genTextView();
        connectionTitle.setText(connectionTitleText);
        connectionTitle.setTranslationY(animator.getValue() ? 0 : getHeaderHeight());
        wrapper.addView(connectionTitle);

        return wrapper;
    }

    @Override
    public int getId() {
        return 0;
    }

    public void setConnectionTitleEnabled(boolean enabled) {
        if (!enabled) {
            getArguments().tdlib().ui().postDelayed(() -> {
                animator.setValue(false, true);
            }, 500);
        } else {
            animator.setValue(true, true);
        }
    }

    public void setConnectionTitle(String title) {
        connectionTitleText = title;
        if (connectionTitle != null)
            connectionTitle.setText(title);
    }

    public void setDefaultTitle(String title) {
        defaultTitleText = title;
        if (defaultTitle != null)
            defaultTitle.setText(title);
    }

    @Override
    public void onFactorChanged(int id, float factor, float fraction, FactorAnimator callee) {
        if (wrapper == null) {
            return;
        }
        defaultTitle.setTranslationY(-getHeaderHeight() * factor);
        connectionTitle.setTranslationY(getHeaderHeight() + -getHeaderHeight() * factor);
    }

    @Override
    public void onFactorChangeFinished(int id, float finalFactor, FactorAnimator callee) {
        onFactorChanged(id, finalFactor, 0, callee);
    }

    public void onAccountProfileChanged (TdlibAccount account, TdApi.User profile, boolean isCurrent, boolean isLoaded) {
        // TODO: not supported yet
    }

    public void onAccountProfileEmojiStatusChanged (TdlibAccount account, boolean isCurrent) {
        // TODO: not supported yet
    }

    public void onAccountSwitched (TdlibAccount newAccount, TdApi.User profile, int reason, TdlibAccount oldAccount) {
        // TODO: not supported yet
    }

    public void onConnectionStateChanged (Tdlib tdlib, @ConnectionState int newState, boolean isCurrent) {
        setConnectionTitleEnabled(newState != ConnectionState.CONNECTED);
        setConnectionTitle(Lang.getString(TdlibUi.stringForConnectionState(newState)));
    }
}
