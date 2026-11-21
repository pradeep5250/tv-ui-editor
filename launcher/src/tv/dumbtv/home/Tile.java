package tv.dumbtv.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.media.tv.TvInputManager;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

/** A card drawn in one pass: banner, or icon plus name, or an input name; focus ring on top. */
final class Tile extends View {
    private static final float FOCUS_SCALE = 1.08f;

    final Item item;
    final String kind;
    private final CharSequence title;
    private final int cardW, cardH, labelH;
    private final boolean labelAlways, labelOnFocus;
    private final float radius, stroke;
    private final RectF card = new RectF();
    private final Path clip = new Path();
    private final Path arrow = new Path();
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint inner = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint under = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private boolean moving;
    private final AfterglowTile afterglow;
    private final Drawable inputIcon;

    Tile(Context c, Item item, String kind, CharSequence title, int cardW, int cardH,
            String labels, int accent) {
        super(c);
        this.item = item;
        this.kind = kind;
        this.title = title;
        this.cardW = cardW;
        this.cardH = cardH;
        boolean app = Catalog.APPS.equals(kind);
        afterglow = null;
        labelH = 0;
        labelAlways = "always".equals(labels);
        labelOnFocus = "focus".equals(labels);
        radius = Ui.dp(c, 14);
        stroke = Ui.dp(c, 2.8f);

        Drawable inIcon = null;
        if (!app && title != null) {
            String lower = title.toString().toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("hdmi")) {
                inIcon = c.getDrawable(R.drawable.ic_input_hdmi);
            } else if (lower.contains("av")) {
                inIcon = c.getDrawable(R.drawable.ic_input_av);
            } else if (lower.contains("usb")) {
                inIcon = c.getDrawable(R.drawable.ic_input_usb);
            }
        }
        inputIcon = inIcon;

        fill.setColor(app ? 0xCC101B32 : 0xCC0D1525);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(stroke);
        ring.setColor(0xFF00C8FF);
        dot.setColor(0xFF00C8FF);
        inner.setColor(0xFFF5F8FF);
        inner.setTypeface(Ui.MEDIUM);
        inner.setTextAlign(Paint.Align.CENTER);
        inner.setTextSize(Ui.dp(c, app ? 13 : 15));
        under.setColor(Ui.TEXT);
        under.setTypeface(Ui.MEDIUM);
        under.setTextAlign(Paint.Align.CENTER);
        under.setTextSize(Ui.dp(c, 13));

        setFocusable(true);
        setClickable(true);
        setLongClickable(true);
        setContentDescription(title);
    }

    void setMoving(boolean on) {
        moving = on;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(cardW, cardH + labelH);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        card.set(0, 0, cardW, cardH);
        clip.reset();
        clip.addRoundRect(card, radius, radius, Path.Direction.CW);
        if (afterglow != null) afterglow.resize(cardW, cardH);
        setPivotX(cardW / 2f);
        setPivotY(cardH / 2f);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previous) {
        super.onFocusChanged(focused, direction, previous);
        animate().cancel();
        if (focused) {
            animate().scaleX(FOCUS_SCALE).scaleY(FOCUS_SCALE)
                    .withLayer()
                    .setInterpolator(new android.view.animation.DecelerateInterpolator(1.8f))
                    .setDuration(160).start();
        } else {
            animate().scaleX(1f).scaleY(1f)
                    .withLayer()
                    .setInterpolator(new android.view.animation.DecelerateInterpolator(1.8f))
                    .setDuration(160).start();
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable banner = item.banner, icon = item.icon;
        canvas.save();
        canvas.clipPath(clip);
        canvas.drawRect(card, fill);
        if (afterglow != null) {
            afterglow.draw(canvas, icon, cardW, cardH);
        } else if (Catalog.APPS.equals(kind) && icon != null) {
            int size = Math.round(cardH * 0.52f);
            int left = (cardW - size) / 2, top = Math.round(cardH * 0.12f);
            icon.setBounds(left, top, left + size, top + size);
            icon.draw(canvas);
            drawLine(canvas, inner, cardW / 2f, cardH - cardH * 0.14f);
        } else if (banner != null) {
            banner.setBounds(0, 0, cardW, cardH);
            banner.draw(canvas);
        } else if (icon != null) {
            int size = cardH * 46 / 100;
            int left = (cardW - size) / 2, top = cardH * 12 / 100;
            icon.setBounds(left, top, left + size, top + size);
            icon.draw(canvas);
            drawLine(canvas, inner, cardW / 2f, cardH - cardH * 13 / 100f);
        } else if (!Catalog.APPS.equals(kind)) {
            if (inputIcon != null) {
                int iw = Math.max(1, inputIcon.getIntrinsicWidth());
                int ih = Math.max(1, inputIcon.getIntrinsicHeight());
                float scale = (float) cardH * 0.40f / (float) ih;
                int iconW = Math.round(iw * scale);
                int iconH = Math.round(ih * scale);
                int gap = Ui.dp(getContext(), 8);
                float textW = inner.measureText(title, 0, title.length());
                float totalW = iconW + gap + textW;
                float startX = (cardW - totalW) / 2f;
                int iconTop = Math.round((cardH - iconH) / 2f);
                inputIcon.setBounds(Math.round(startX), iconTop, Math.round(startX + iconW), iconTop + iconH);
                inputIcon.draw(canvas);

                float baseline = cardH / 2f - (inner.ascent() + inner.descent()) / 2f;
                canvas.drawText(title, 0, title.length(), startX + iconW + gap + textW / 2f, baseline, inner);
            } else {
                drawLine(canvas, inner, cardW / 2f, cardH / 2f - (inner.ascent() + inner.descent()) / 2);
            }
        }
        if (moving) canvas.drawColor(0x99000000);
        canvas.restore();

        if (afterglow != null) afterglow.edge(canvas, cardW, cardH, radius);

        if (item.state == TvInputManager.INPUT_STATE_CONNECTED) {
            float r = Ui.dp(getContext(), 3.5f);
            canvas.drawCircle(cardW - r * 3, r * 3, r, dot);
        }
        if (isFocused() || moving) {
            float half = stroke / 2;
            ring.setColor(0xFF00C8FF);
            ring.setStrokeWidth(Ui.dp(getContext(), 2.8f));
            ring.setPathEffect(moving ? new DashPathEffect(new float[] {stroke * 3, stroke * 2}, 0) : null);
            canvas.drawRoundRect(half, half, cardW - half, cardH - half, radius - half, radius - half, ring);
        }
        if (moving) drawArrows(canvas);
        boolean label = banner != null && (labelAlways || labelOnFocus && isFocused());
        if (labelH > 0 && label) {
            drawLine(canvas, under, cardW / 2f, cardH + labelH - Ui.dp(getContext(), 6));
        }
    }

    private void drawLine(Canvas canvas, TextPaint paint, float x, float baseline) {
        float room = cardW - Ui.dp(getContext(), 16);
        CharSequence s = TextUtils.ellipsize(title, paint, room, TextUtils.TruncateAt.END);
        canvas.drawText(s, 0, s.length(), x, baseline, paint);
    }

    private void drawArrows(Canvas canvas) {
        float h = cardH / 6f, w = h * 0.7f, mid = cardH / 2f, pad = h * 0.8f;
        inner.setStyle(Paint.Style.FILL);
        arrow.reset();
        arrow.moveTo(pad, mid);
        arrow.lineTo(pad + w, mid - h / 2);
        arrow.lineTo(pad + w, mid + h / 2);
        arrow.close();
        arrow.moveTo(cardW - pad, mid);
        arrow.lineTo(cardW - pad - w, mid - h / 2);
        arrow.lineTo(cardW - pad - w, mid + h / 2);
        arrow.close();
        canvas.drawPath(arrow, inner);
    }
}
