package io.github.dmxystudio.grokassist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * Simple swipe detector. onDown() returns true to ensure subsequent callbacks
 * (required for onFling/onScroll to fire reliably).
 */
@SuppressWarnings({"WeakerAccess"})
public class SwipeTouchListener implements OnTouchListener {

    private final GestureDetector gestureDetector;

    public SwipeTouchListener(Context context) {
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // 先把事件喂给手势识别器，但不拦截，
        // 让 WebView 继续收到点击/滚动等默认处理。
        gestureDetector.onTouchEvent(event);
        return false; // 关键：不消费事件，保证按钮/链接可点
    }

    public void onSwipeRight() {}
    public void onSwipeLeft() {}
    public void onSwipeTop() {}
    public void onSwipeBottom() {}

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD_PX = 100; // keep original semantics
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            // Must return true to receive subsequent events.
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 != null && e2 != null) {
                try {
                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD_PX && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) onSwipeRight(); else onSwipeLeft();
                        }
                    } else if (Math.abs(diffY) > SWIPE_THRESHOLD_PX && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) onSwipeBottom(); else onSwipeTop();
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }
}