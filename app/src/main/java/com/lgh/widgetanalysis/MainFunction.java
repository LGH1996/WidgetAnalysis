package com.lgh.widgetanalysis;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.lgh.widgetanalysis.databinding.ViewMessageBinding;
import com.lgh.widgetanalysis.databinding.ViewWidgetSelectBinding;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
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
    private int currentPosition;

    private ArrayList<ArrayList<AccessibilityNodeInfo>> nodeInfoList;

    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;
    private Bitmap mBitmap;

    private AccessibilityServiceInfo serviceInfo;

    public MainFunction(AccessibilityService service) {
        this.service = service;
    }

    protected void onServiceConnected() {
        try {
            windowManager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
            serviceInfo = service.getServiceInfo();
        } catch (Throwable throwable) {
//            throwable.printStackTrace();
        }
    }

    public void onUnbind() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
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
                case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                    List<CharSequence> msgList = event.getText();
                    StringBuilder builder = new StringBuilder();
                    for (CharSequence s : msgList) {
                        builder.append(s.toString().replaceAll("\\s", ""));
                    }
                    viewMessageBinding.message.setText("receive a new notify -->" + "\neventTime：" + SimpleDateFormat.getDateTimeInstance().format(Calendar.getInstance()) + "\npackage：" + event.getPackageName() + "\nmessage：" + builder.toString());
                    break;
            }
        } catch (Throwable throwable) {
//            throwable.printStackTrace();
        }
    }

    public boolean onKeyEvent(KeyEvent event) {
        viewMessageBinding.message.setText("receive a new keyEvent -->" + "\neventTime：" + SimpleDateFormat.getDateTimeInstance().format(Calendar.getInstance()) + "\naction：" + (event.getAction() == KeyEvent.ACTION_DOWN ? "ACTION_DOWN" : "ACTION_UP") + "\nkeyName：" + KeyEvent.keyCodeToString(event.getKeyCode()) + "\nkeyCode：" + event.getKeyCode());
        return false;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        try {
            if (viewMessageBinding != null && widgetSelectBinding != null) {
                windowManager.removeView(viewMessageBinding.getRoot());
                windowManager.removeView(widgetSelectBinding.getRoot());
                viewMessageBinding = null;
                widgetSelectBinding = null;
                showAnalysisFloatWindow();
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
            ArrayList<AccessibilityNodeInfo> tem = new ArrayList<>();
            for (AccessibilityNodeInfo e : roots) {
                if (e == null) continue;
                Rect rect = new Rect();
                e.getBoundsInScreen(rect);
                if (rect.width() <= 0 || rect.height() <= 0) continue;
                list.add(e);
                for (int n = 0; n < e.getChildCount(); n++) {
                    tem.add(e.getChild(n));
                }
            }
            if (!tem.isEmpty()) {
                findAllNode(tem, list);
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

            serviceInfo.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            service.setServiceInfo(serviceInfo);

            final LayoutInflater inflater = LayoutInflater.from(service);

            widgetSelectBinding = ViewWidgetSelectBinding.inflate(inflater);
            viewMessageBinding = ViewMessageBinding.inflate(inflater);

            Typeface iconFont = Typeface.createFromAsset(service.getAssets(), "iconfont.ttf");
            viewMessageBinding.onOff.setTypeface(iconFont);
            viewMessageBinding.min.setTypeface(iconFont);
            viewMessageBinding.close.setTypeface(iconFont);
            viewMessageBinding.left.setTypeface(iconFont);
            viewMessageBinding.right.setTypeface(iconFont);

            final DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getRealMetrics(metrics);

            aParams = new WindowManager.LayoutParams();
            aParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
            aParams.format = PixelFormat.TRANSPARENT;
            aParams.gravity = Gravity.START | Gravity.TOP;
            aParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            aParams.width = dp2px(service, 100);
            aParams.height = dp2px(service, 100);

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

                    viewMessageBinding.message.setText("package: " + currentPackage + "\n" + "activity: " + currentActivity);
                    if (bParams.alpha == 0) {
                        nodeInfoList = new ArrayList<>();
                        List<AccessibilityWindowInfo> windowInfoList = service.getWindows();
                        for (AccessibilityWindowInfo e : windowInfoList) {
                            AccessibilityNodeInfo node = e.getRoot();
                            if (node != null && !node.getPackageName().equals(service.getPackageName())) {
                                ArrayList<AccessibilityNodeInfo> nodeList = new ArrayList<>();
                                findAllNode(Collections.singletonList(node), nodeList);
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
                                nodeInfoList.add(nodeList);
                            }
                        }
                        nodeInfoList.sort(new Comparator<ArrayList<AccessibilityNodeInfo>>() {
                            @Override
                            public int compare(ArrayList<AccessibilityNodeInfo> o1, ArrayList<AccessibilityNodeInfo> o2) {
                                return o2.size() - o1.size();
                            }
                        });
                        mBitmap = getCapture();
                        refreshLayout(nodeInfoList.get(currentPosition = 0));
                        bParams.alpha = 1f;
                        bParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                        viewMessageBinding.onOff.setText(R.string.visible);
                        viewMessageBinding.left.setClickable(true);
                        viewMessageBinding.right.setClickable(true);
                    } else {
                        bParams.alpha = 0f;
                        bParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                        widgetSelectBinding.frame.removeAllViews();
                        viewMessageBinding.onOff.setText(R.string.invisible);
                        viewMessageBinding.left.setClickable(false);
                        viewMessageBinding.right.setClickable(false);
                    }
                    windowManager.updateViewLayout(widgetSelectBinding.getRoot(), bParams);
                }
            });

            viewMessageBinding.left.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (nodeInfoList == null || nodeInfoList.isEmpty()) {
                        return;
                    }
                    viewMessageBinding.message.setText("package: " + currentPackage + "\n" + "activity: " + currentActivity);
                    if (--currentPosition < 0) {
                        currentPosition = nodeInfoList.size() - 1;
                    }
                    refreshLayout(nodeInfoList.get(currentPosition));
                }
            });
            viewMessageBinding.left.setClickable(false);

            viewMessageBinding.right.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (nodeInfoList == null || nodeInfoList.isEmpty()) {
                        return;
                    }
                    viewMessageBinding.message.setText("package: " + currentPackage + "\n" + "activity: " + currentActivity);
                    if (++currentPosition > nodeInfoList.size() - 1) {
                        currentPosition = 0;
                    }
                    refreshLayout(nodeInfoList.get(currentPosition));
                }
            });
            viewMessageBinding.right.setClickable(false);

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
                            int w = aParams.width + Math.round(event.getRawX() - x);
                            int h = aParams.height + Math.round(event.getRawY() - y);
                            aParams.width = w > dp2px(service, 100) && w + aParams.x < metrics.widthPixels ? w : aParams.width;
                            aParams.height = h > dp2px(service, 100) && h + aParams.y < metrics.heightPixels ? h : aParams.height;
                            x = Math.round(event.getRawX());
                            y = Math.round(event.getRawY());
                            windowManager.updateViewLayout(viewMessageBinding.getRoot(), aParams);
                            if (aParams.width < dp2px(service, 200)) {
                                setViewVisibility(View.GONE);
                            } else {
                                setViewVisibility(View.VISIBLE);
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
                    aParams.width = dp2px(service, 20);
                    aParams.height = dp2px(service, 20);
                    viewMessageBinding.topView.setVisibility(View.GONE);
                    windowManager.updateViewLayout(viewMessageBinding.getRoot(), aParams);
                }
            });

            viewMessageBinding.close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    serviceInfo.flags &= ~AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
                    service.setServiceInfo(serviceInfo);
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

    private void refreshLayout(ArrayList<AccessibilityNodeInfo> nodeList) {
        widgetSelectBinding.frame.removeAllViews();
        ImageView bg = new ImageView(service);
        bg.setImageBitmap(mBitmap);
        widgetSelectBinding.frame.addView(bg, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        for (AccessibilityNodeInfo e : nodeList) {
            Rect parentRect = new Rect();
            e.getBoundsInParent(parentRect);
            Rect screenRect = new Rect();
            e.getBoundsInScreen(screenRect);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(screenRect.width(), screenRect.height());
            params.leftMargin = screenRect.left;
            params.topMargin = screenRect.top;
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
                        str.append("boundsInParent: " + "Rect(").append(px2dp(service, parentRect.left)).append(", ").append(px2dp(service, parentRect.top)).append(" - ").append(px2dp(service, parentRect.right)).append(", ").append(px2dp(service, parentRect.bottom)).append(") " + "dp").append("\n");
                        str.append("boundsInScreen: " + "Rect(").append(px2dp(service, screenRect.left)).append(", ").append(px2dp(service, screenRect.top)).append(" - ").append(px2dp(service, screenRect.right)).append(", ").append(px2dp(service, screenRect.bottom)).append(") " + "dp").append("\n");
                        viewMessageBinding.message.setText("package: " + currentPackage + "\n" + "activity: " + currentActivity + "\n" + str.toString().trim());
                        v.setBackgroundResource(R.drawable.node_focus);
                    } else {
                        v.setBackgroundResource(R.drawable.node);
                    }
                }
            });
            if (e.isFocused()) {
                img.requestFocus();
                ImageView imgFocus = new ImageView(service);
                imgFocus.setBackgroundResource(R.drawable.is_focus);
                widgetSelectBinding.frame.addView(imgFocus, params);
            }
            widgetSelectBinding.frame.addView(img, params);
        }
    }

    @SuppressLint("WrongConstant")
    public void initCapture(int resultCode, Intent data) {
        if (mVirtualDisplay != null) return;
        WindowManager mWindowManager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(metrics);
        mImageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2);
        MediaProjection mediaProjection = ((MediaProjectionManager) service.getSystemService(Context.MEDIA_PROJECTION_SERVICE)).getMediaProjection(resultCode, data);
        mVirtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mImageReader.getSurface(), null, null);
    }

    public Bitmap getCapture() {
        while (true) {
            Image image = mImageReader.acquireLatestImage();
            if (image == null) continue;
            int width = image.getWidth();
            int height = image.getHeight();
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;
            Bitmap mBitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
            mBitmap.copyPixelsFromBuffer(buffer);
            image.close();
            return Bitmap.createBitmap(mBitmap, 0, 0, width, height);
        }
    }

    private void setViewVisibility(int visibility) {
        viewMessageBinding.left.setVisibility(visibility);
        viewMessageBinding.right.setVisibility(visibility);
        viewMessageBinding.min.setVisibility(visibility);
    }

    public void createForegroundNotification() {
        Notification.Builder builder = new Notification.Builder(service)
                .setContentIntent(PendingIntent.getActivity(service, 0x01, new Intent(service, MainActivity.class), PendingIntent.FLAG_ONE_SHOT))
                .setLargeIcon(BitmapFactory.decodeResource(service.getResources(), R.mipmap.ic_launcher))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("running......");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(service.getPackageName());
            NotificationManager notificationManager = service.getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(service.getPackageName(), service.getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
        service.startForeground(0x01, builder.build());
    }

    public static int px2dp(Context context, int pxValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }


    public static int dp2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}