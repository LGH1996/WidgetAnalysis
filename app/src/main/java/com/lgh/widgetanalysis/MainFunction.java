package com.lgh.widgetanalysis;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.lgh.widgetanalysis.databinding.ViewMessageBinding;
import com.lgh.widgetanalysis.databinding.ViewWidgetSelectBinding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * adb shell pm  grant com.lgh.widgetanalysis android.permission.WRITE_SECURE_SETTINGS
 * adb shell settings put secure enabled_accessibility_services com.lgh.widgetanalysis/com.lgh.widgetanalysis.MyAccessibilityService
 * adb shell settings put secure accessibility_enabled 1
 * <p>
 * Settings.Secure.putString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, getPackageName()+"/"+MyAccessibilityService.class.getName());
 * Settings.Secure.putString(getContentResolver(),Settings.Secure.ACCESSIBILITY_ENABLED, "1");
 */

public class MainFunction {

    private WindowManager windowManager;
    private final AccessibilityService service;

    private WindowManager.LayoutParams aParams, bParams;
    private ViewWidgetSelectBinding widgetSelectBinding;
    private ViewMessageBinding viewMessageBinding;

    private String currentPackage;
    private String currentActivity;

    private int mParamWidth, mParamHeight;

    public MainFunction(AccessibilityService service) {
        this.service = service;
    }

    protected void onServiceConnected() {
        try {
            windowManager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
        } catch (Throwable throwable) {
//            throwable.printStackTrace();
        }
    }

    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            switch (event.getEventType()) {
                case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                    AccessibilityNodeInfo root = service.getRootInActiveWindow();
                    CharSequence temPackage = event.getPackageName();
                    CharSequence temClass = event.getClassName();
                    String packageName = root != null ? root.getPackageName().toString() : temPackage != null ? temPackage.toString() : null;
                    String activityName = temClass != null ? temClass.toString() : null;
                    if (packageName != null) {
                        currentPackage = packageName;
                    }
                    if (activityName != null && !activityName.startsWith("android.widget.") && !activityName.startsWith("android.view.")) {
                        currentActivity = activityName;
                    }
                    break;
            }
        } catch (Throwable throwable) {
//            throwable.printStackTrace();
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        try {
            if (viewMessageBinding != null && widgetSelectBinding != null) {
                DisplayMetrics metrics = new DisplayMetrics();
                windowManager.getDefaultDisplay().getRealMetrics(metrics);
                aParams.width = metrics.widthPixels;
                aParams.height = metrics.heightPixels / 4;
                aParams.x = (metrics.widthPixels - aParams.width) / 2;
                aParams.y = metrics.heightPixels - aParams.height;
                bParams.width = metrics.widthPixels;
                bParams.height = metrics.heightPixels;
                windowManager.updateViewLayout(viewMessageBinding.getRoot(), aParams);
                widgetSelectBinding.frame.removeAllViews();
                TextView text = new TextView(service);
                text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
                text.setGravity(Gravity.CENTER);
                text.setTextColor(0xffff0000);
                text.setText("请重新刷新布局");
                windowManager.updateViewLayout(widgetSelectBinding.frame, bParams);
                widgetSelectBinding.frame.addView(text, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER));
            }
        } catch (Throwable e) {
//            e.printStackTrace();
        }
    }

    /**
     * 查找所有
     * 的控件
     */
    private void findAllNode(List<AccessibilityNodeInfo> roots, List<AccessibilityNodeInfo> list) {
        try {
            ArrayList<AccessibilityNodeInfo> temList = new ArrayList<>();
            for (AccessibilityNodeInfo e : roots) {
                if (e == null) continue;
                list.add(e);
                for (int n = 0; n < e.getChildCount(); n++) {
                    temList.add(e.getChild(n));
                }
            }
            if (!temList.isEmpty()) {
                findAllNode(temList, list);
            }
        } catch (Throwable e) {
//            e.printStackTrace();
        }
    }

    /**
     * 创建规则时调用
     */
    @SuppressLint("ClickableViewAccessibility")
    public void showAnalysisFloatWindow() {
        try {
            if (viewMessageBinding != null || widgetSelectBinding != null) {
                return;
            }
            final LayoutInflater inflater = LayoutInflater.from(service);

            widgetSelectBinding = ViewWidgetSelectBinding.inflate(inflater);
            viewMessageBinding = ViewMessageBinding.inflate(inflater);

            Typeface iconFont = Typeface.createFromAsset(service.getAssets(), "iconfont.ttf");
            viewMessageBinding.onOff.setTypeface(iconFont);
            viewMessageBinding.min.setTypeface(iconFont);
            viewMessageBinding.close.setTypeface(iconFont);

            final DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getRealMetrics(metrics);
            int width = Math.min(metrics.heightPixels, metrics.widthPixels);
            int height = Math.max(metrics.heightPixels, metrics.widthPixels);

            aParams = new WindowManager.LayoutParams();
            aParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
            aParams.format = PixelFormat.TRANSPARENT;
            aParams.gravity = Gravity.START | Gravity.TOP;
            aParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            aParams.width = width;
            aParams.height = height / 4;
            aParams.x = (metrics.widthPixels - aParams.width) / 2;
            aParams.y = metrics.heightPixels - aParams.height;
            aParams.alpha = 0.9f;

            bParams = new WindowManager.LayoutParams();
            bParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
            bParams.format = PixelFormat.TRANSPARENT;
            bParams.gravity = Gravity.START | Gravity.TOP;
            bParams.width = metrics.widthPixels;
            bParams.height = metrics.heightPixels;
            bParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            bParams.alpha = 0f;

            viewMessageBinding.getRoot().setOnTouchListener(new View.OnTouchListener() {

                int startX = 0, startY = 0, x = 0, y = 0;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = x = Math.round(event.getRawX());
                            startY = y = Math.round(event.getRawY());
                            break;
                        case MotionEvent.ACTION_MOVE:
                            aParams.x = Math.round(aParams.x + (event.getRawX() - x));
                            aParams.y = Math.round(aParams.y + (event.getRawY() - y));
                            x = Math.round(event.getRawX());
                            y = Math.round(event.getRawY());
                            windowManager.updateViewLayout(viewMessageBinding.getRoot(), aParams);
                            break;
                        case MotionEvent.ACTION_UP:
                            windowManager.getDefaultDisplay().getRealMetrics(metrics);
                            aParams.x = aParams.x < 0 ? 0 : aParams.x;
                            aParams.x = aParams.x > metrics.widthPixels - aParams.width ? metrics.widthPixels - aParams.width : aParams.x;
                            aParams.y = aParams.y < 0 ? 0 : aParams.y;
                            aParams.y = aParams.y > metrics.heightPixels - aParams.height ? metrics.heightPixels - aParams.height : aParams.y;
                            if (Math.abs(event.getRawX() - startX) < 10 && Math.abs(event.getRawY() - startY) < 10) {
                                if (viewMessageBinding.topView.getVisibility() == View.GONE) {
                                    viewMessageBinding.topView.setVisibility(View.VISIBLE);
                                    aParams.width = mParamWidth;
                                    aParams.height = mParamHeight;
                                    windowManager.updateViewLayout(viewMessageBinding.getRoot(), aParams);
                                }
                            }
                            break;
                    }
                    return true;
                }
            });
            viewMessageBinding.onOff.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (bParams.alpha == 0) {
                        AccessibilityNodeInfo root = service.getRootInActiveWindow();
                        if (root == null) return;

                        ArrayList<AccessibilityNodeInfo> roots = new ArrayList<>();
                        roots.add(root);
                        ArrayList<AccessibilityNodeInfo> nodeList = new ArrayList<>();
                        findAllNode(roots, nodeList);
                        nodeList.sort(new Comparator<AccessibilityNodeInfo>() {
                            @Override
                            public int compare(AccessibilityNodeInfo a, AccessibilityNodeInfo b) {
                                Rect rectA = new Rect();
                                Rect rectB = new Rect();
                                a.getBoundsInScreen(rectA);
                                b.getBoundsInScreen(rectB);
                                return rectB.width() * rectB.height() - rectA.width() * rectA.height();
                            }
                        });
                        for (AccessibilityNodeInfo e : nodeList) {
                            final Rect temRect = new Rect();
                            e.getBoundsInScreen(temRect);
                            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(temRect.width(), temRect.height());
                            params.leftMargin = temRect.left;
                            params.topMargin = temRect.top;
                            final ImageView img = new ImageView(service);
                            img.setBackgroundResource(R.drawable.node);
                            img.setFocusableInTouchMode(true);
                            img.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    v.requestFocus();
                                }
                            });
                            img.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                                @Override
                                public void onFocusChange(View v, boolean hasFocus) {
                                    if (hasFocus) {
                                        String msg = e.toString();
                                        msg = msg.substring(msg.indexOf("boundsInParent:"), msg.lastIndexOf("actions:"));
                                        StringBuilder str = new StringBuilder();
                                        for (String e : msg.split(";")) {
                                            str.append(e.trim()).append("\n");
                                        }
                                        viewMessageBinding.message.setText("package:" + currentPackage + "\n" + "activity:" + currentActivity + "\n" + str.toString());
                                        v.setBackgroundResource(R.drawable.node_focus);
                                    } else {
                                        v.setBackgroundResource(R.drawable.node);
                                    }
                                }
                            });
                            widgetSelectBinding.frame.addView(img, params);
                        }
                        bParams.alpha = 0.8f;
                        bParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                        viewMessageBinding.onOff.setText(R.string.visible);
                    } else {
                        bParams.alpha = 0f;
                        bParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                        widgetSelectBinding.frame.removeAllViews();
                        viewMessageBinding.onOff.setText(R.string.invisible);
                    }
                    viewMessageBinding.message.setText("package:" + currentPackage + "\n" + "activity:" + currentActivity);
                    windowManager.updateViewLayout(widgetSelectBinding.getRoot(), bParams);
                }
            });

            viewMessageBinding.drag.setOnTouchListener(new View.OnTouchListener() {

                int x = 0, y = 0;

                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            x = Math.round(event.getRawX());
                            y = Math.round(event.getRawY());
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if (Math.abs(event.getRawX() - x) > 5 || Math.abs(event.getRawY() - y) > 5) {
                                int w = aParams.width + Math.round(event.getRawX() - x);
                                int h = aParams.height + Math.round(event.getRawY() - y);
                                aParams.width = w > 500 && w + aParams.x < metrics.widthPixels ? w : aParams.width;
                                aParams.height = h > 500 && h + aParams.y < metrics.heightPixels ? h : aParams.height;
                                x = Math.round(event.getRawX());
                                y = Math.round(event.getRawY());
                                windowManager.updateViewLayout(viewMessageBinding.getRoot(), aParams);
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            windowManager.getDefaultDisplay().getRealMetrics(metrics);
                            aParams.x = aParams.x < 0 ? 0 : aParams.x;
                            aParams.x = aParams.x > metrics.widthPixels - aParams.width ? metrics.widthPixels - aParams.width : aParams.x;
                            aParams.y = aParams.y < 0 ? 0 : aParams.y;
                            aParams.y = aParams.y > metrics.heightPixels - aParams.height ? metrics.heightPixels - aParams.height : aParams.y;
                            break;
                    }

                    return true;
                }
            });

            viewMessageBinding.min.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mParamWidth = aParams.width;
                    mParamHeight = aParams.height;
                    aParams.width = 50;
                    aParams.height = 50;
                    viewMessageBinding.topView.setVisibility(View.GONE);
                    windowManager.updateViewLayout(viewMessageBinding.getRoot(), aParams);
                }
            });

            viewMessageBinding.close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    windowManager.removeViewImmediate(widgetSelectBinding.getRoot());
                    windowManager.removeViewImmediate(viewMessageBinding.getRoot());
                    widgetSelectBinding = null;
                    viewMessageBinding = null;
                    aParams = null;
                    bParams = null;
                }
            });
            windowManager.addView(widgetSelectBinding.getRoot(), bParams);
            windowManager.addView(viewMessageBinding.getRoot(), aParams);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
