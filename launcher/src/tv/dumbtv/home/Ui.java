package tv.dumbtv.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.widget.TextView;

final class Ui {
    static final int TEXT = 0xFFF2F2F3;
    static final int MUTED = 0x99FFFFFF;
    static final int FAINT = 0x66FFFFFF;
    static final int SURFACE = 0xFF1C1D21;
    static final int DIALOG = android.R.style.Theme_DeviceDefault_Dialog_Alert;
    static final Typeface LIGHT = Typeface.create("sans-serif-light", Typeface.NORMAL);
    static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);

    private Ui() {}

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** Black on light accents, white on dark ones. */
    static int ink(int color) {
        return Color.luminance(color) > 0.5f ? 0xFF000000 : 0xFFFFFFFF;
    }

    static TextView text(Context c, float sp, int color, Typeface face) {
        TextView v = new TextView(c);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTextColor(color);
        v.setTypeface(face);
        v.setIncludeFontPadding(false);
        return v;
    }

    static GradientDrawable round(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    static GradientDrawable round(int color, float radius, int strokeColor, int strokeWidth) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (strokeWidth > 0) d.setStroke(strokeWidth, strokeColor);
        return d;
    }

    /** Accent fill or border when focused, a faint glass fill otherwise. */
    static void focusStyle(TextView v, int accent, int idle, float radius) {
        StateListDrawable bg = new StateListDrawable();
        int focusedBorder = 0xFF00C8FF;
        int focusedBg = 0x3300C8FF;
        int idleBg = 0xB0101B32;
        int idleBorder = 0x25FFFFFF;
        int strokeW = dp(v.getContext(), 2.2f);
        int idleStrokeW = dp(v.getContext(), 1f);

        bg.addState(new int[] {android.R.attr.state_focused}, round(focusedBg, radius, focusedBorder, strokeW));
        bg.addState(new int[0], round(idleBg, radius, idleBorder, idleStrokeW));
        v.setBackground(bg);

        ColorStateList colors = new ColorStateList(
                new int[][] {{android.R.attr.state_focused}, {}},
                new int[] {focusedBorder, 0xD9FFFFFF});
        v.setTextColor(colors);
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            v.setCompoundDrawableTintList(colors);
        }
    }
}
