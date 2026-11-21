package com.github.yukagram.ui;

import android.graphics.RectF;

import me.vkryl.core.MathUtils;

public class Yutils {
    public static RectF rectFLerp(RectF a, RectF b, float factor) {
        return new RectF(
            MathUtils.fromTo(a.left, b.left, factor),
            MathUtils.fromTo(a.top, b.top, factor),
            MathUtils.fromTo(a.right, b.right, factor),
            MathUtils.fromTo(a.bottom, b.bottom, factor)
        );
    }
}
