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
import android.graphics.Region;
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
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.lgh.widgetanalysis.databinding.ViewMessageBinding;
import com.lgh.widgetanalysis.databinding.ViewWidgetSelectBinding;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * adb shell pm grant com.lgh.widgetanalysis android.permission.WRITE_SECURE_SETTINGS
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

    private AccessibilityServiceInfo serviceInfo;

    private SimpleKeyValue<ImageView, AccessibilityNodeInfo> imgAndNodes;
    private SimpleKeyValue.Entry<ImageView, AccessibilityNodeInfo> currentImgAndNode;

    private AbsoluteLayout.LayoutParams mainParams;

    private Bitmap bitmap;

    public MainFunction(AccessibilityService service) {
        this.service = service;
    }

    protected void onServiceConnected() {
        try {
            windowManager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
            serviceInfo = service.getServiceInfo();
            @SuppressLint("SoonBlockedPrivateApi") Field field = AccessibilityNodeInfo.class.getDeclaredField("DEBUG");
            field.setAccessible(true);
            field.setBoolean(null, true);
        } catch (Throwable throwable) {
//            throwable.printStackTrace();
        }
    }

    public void onUnbind() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
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
            viewMessageBinding.up.setTypeface(iconFont);

            final DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getRealMetrics(metrics);

            aParams = new WindowManager.LayoutParams();
            aParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
            aParams.format = PixelFormat.TRANSPARENT;
            aParams.gravity = Gravity.START | Gravity.TOP;
            aParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            aParams.width = metrics.widthPixels;
            aParams.height = metrics.heightPixels;

            bParams = new WindowManager.LayoutParams();
            bParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
            bParams.format = PixelFormat.TRANSPARENT;
            bParams.gravity = Gravity.START | Gravity.TOP;
            bParams.width = metrics.widthPixels;
            bParams.height = metrics.heightPixels;
            bParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            bParams.alpha = 0f;

            mainParams = (AbsoluteLayout.LayoutParams) viewMessageBinding.main.getLayoutParams();
            mainParams.width = dp2px(service, 100);
            mainParams.height = dp2px(service, 100);
            mainParams.x = 0;
            mainParams.y = 0;
            viewMessageBinding.main.setLayoutParams(mainParams);

            /*viewMessageBinding.main.getViewTreeObserver().addOnComputeInternalInsetsListener(new ViewTreeObserver.OnComputeInternalInsetsListener() {
                @Override
                public void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo inoutInfo) {
                    inoutInfo.setTouchableInsets(ViewTreeObserver.InternalInsetsInfo.TOUCHABLE_INSETS_REGION);
                    inoutInfo.touchableRegion.set(mainParams.x, mainParams.y, mainParams.x + mainParams.width, mainParams.y + mainParams.height);
                }
            });*/

            @SuppressLint("PrivateApi") Class<?> onComputeInternalInsetsListenerClass = Class.forName("android.view.ViewTreeObserver$OnComputeInternalInsetsListener");
            @SuppressLint("PrivateApi") Class<?> internalInsetsInfoClass = Class.forName("android.view.ViewTreeObserver$InternalInsetsInfo");
            @SuppressLint("DiscouragedPrivateApi") Method setTouchableInsetsMethod = internalInsetsInfoClass.getDeclaredMethod("setTouchableInsets", int.class);
            @SuppressLint("DiscouragedPrivateApi") Field touchableRegionField = internalInsetsInfoClass.getDeclaredField("touchableRegion");
            @SuppressLint("DiscouragedPrivateApi") Field touchTypeField = internalInsetsInfoClass.getDeclaredField("TOUCHABLE_INSETS_REGION");
            setTouchableInsetsMethod.setAccessible(true);
            touchableRegionField.setAccessible(true);
            touchTypeField.setAccessible(true);
            int TOUCHABLE_INSETS_REGION = touchTypeField.getInt(internalInsetsInfoClass);
            Object proxyInstance = Proxy.newProxyInstance(ViewTreeObserver.class.getClassLoader(), new Class<?>[]{onComputeInternalInsetsListenerClass}, new InvocationHandler() {
                @Override
                public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                    setTouchableInsetsMethod.invoke(objects[0], TOUCHABLE_INSETS_REGION);
                    Region region = (Region) touchableRegionField.get(objects[0]);
                    region.set(mainParams.x, mainParams.y, mainParams.x + mainParams.width, mainParams.y + mainParams.height);
                    return null;
                }
            });
            Method addOnComputeInternalInsetsListenerMethod = ViewTreeObserver.class.getDeclaredMethod("addOnComputeInternalInsetsListener", onComputeInternalInsetsListenerClass);
            addOnComputeInternalInsetsListenerMethod.setAccessible(true);
            addOnComputeInternalInsetsListenerMethod.invoke(viewMessageBinding.main.getViewTreeObserver(), proxyInstance);

            viewMessageBinding.main.setOnTouchListener(new View.OnTouchListener() {

                int startRowX = 0, startRowY = 0;
                int startLpX, startLpY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startRowX = Math.round(event.getRawX());
                            startRowY = Math.round(event.getRawY());
                            startLpX = mainParams.x;
                            startLpY = mainParams.y;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            mainParams.x = startLpX + Math.round(event.getRawX()) - startRowX;
                            mainParams.y = startLpY + Math.round(event.getRawY()) - startRowY;
                            viewMessageBinding.main.setLayoutParams(mainParams);
                            break;
                        case MotionEvent.ACTION_UP:
                            windowManager.getDefaultDisplay().getRealMetrics(metrics);
                            mainParams.x = Math.max(mainParams.x, 0);
                            mainParams.x = Math.min(mainParams.x, metrics.widthPixels - mainParams.width);
                            mainParams.y = Math.max(mainParams.y, 0);
                            mainParams.y = Math.min(mainParams.y, metrics.heightPixels - mainParams.height);
                            if (Math.abs(event.getRawX() - startRowX) < 10 && Math.abs(event.getRawY() - startRowY) < 10) {
                                if (viewMessageBinding.topView.getVisibility() == View.GONE) {
                                    viewMessageBinding.topView.setVisibility(View.VISIBLE);
                                    mainParams.width = mParamWidth;
                                    mainParams.height = mParamHeight;
                                }
                            }
                            viewMessageBinding.main.setLayoutParams(mainParams);
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
                        refreshLayout(nodeInfoList.get(currentPosition = 0));
                        bParams.alpha = 1f;
                        bParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                        viewMessageBinding.onOff.setText(R.string.visible);
                        viewMessageBinding.left.setClickable(true);
                        viewMessageBinding.right.setClickable(true);
                        viewMessageBinding.up.setClickable(true);

                        aParams.alpha = 0f;
                        windowManager.updateViewLayout(viewMessageBinding.getRoot(), aParams);
                        viewMessageBinding.getRoot().post(new Runnable() {
                            @Override
                            public void run() {
                                bitmap = getCapture();
                                widgetSelectBinding.background.setImageBitmap(bitmap);
                                aParams.alpha = 1f;
                                windowManager.updateViewLayout(viewMessageBinding.getRoot(), aParams);
                            }
                        });
                    } else {
                        bParams.alpha = 0f;
                        bParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                        widgetSelectBinding.background.setImageBitmap(null);
                        widgetSelectBinding.widgets.removeAllViews();
                        viewMessageBinding.onOff.setText(R.string.invisible);
                        viewMessageBinding.left.setClickable(false);
                        viewMessageBinding.right.setClickable(false);
                        viewMessageBinding.up.setClickable(false);
                    }
                    windowManager.updateViewLayout(widgetSelectBinding.getRoot(), bParams);
                }
            });

            viewMessageBinding.up.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (imgAndNodes == null || imgAndNodes.isEmpty() || currentImgAndNode == null) {
                        return;
                    }
                    AccessibilityNodeInfo parentNode = currentImgAndNode.getValue().getParent();
                    if (parentNode == null) {
                        return;
                    }
                    ImageView parentImg = imgAndNodes.getKeyByValue(parentNode);
                    if (parentImg == null) {
                        return;
                    }
                    parentImg.requestFocus();
                }
            });
            viewMessageBinding.up.setClickable(false);

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

                int startRowX = 0, startRowY = 0;
                int startLpWidth, startLpHeight;

                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startRowX = Math.round(event.getRawX());
                            startRowY = Math.round(event.getRawY());
                            startLpWidth = mainParams.width;
                            startLpHeight = mainParams.height;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            mainParams.width = startLpWidth + Math.round(event.getRawX()) - startRowX;
                            mainParams.height = startLpHeight + Math.round(event.getRawY()) - startRowY;
                            mainParams.width = Math.max(mainParams.width, dp2px(service, 100));
                            mainParams.height = Math.max(mainParams.height, dp2px(service, 100));
                            viewMessageBinding.main.setLayoutParams(mainParams);
                            if (mainParams.width < dp2px(service, 250)) {
                                setViewVisibility(View.GONE);
                            } else {
                                setViewVisibility(View.VISIBLE);
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            windowManager.getDefaultDisplay().getRealMetrics(metrics);
                            mainParams.width = Math.min(mainParams.width, metrics.widthPixels);
                            mainParams.height = Math.min(mainParams.height, metrics.heightPixels);
                            viewMessageBinding.main.setLayoutParams(mainParams);
                            break;
                    }
                    return true;
                }
            });

            viewMessageBinding.min.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mParamWidth = mainParams.width;
                    mParamHeight = mainParams.height;
                    mainParams.width = dp2px(service, 20);
                    mainParams.height = dp2px(service, 20);
                    viewMessageBinding.topView.setVisibility(View.GONE);
                    viewMessageBinding.main.setLayoutParams(mainParams);
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
        widgetSelectBinding.widgets.removeAllViews();
        imgAndNodes = new SimpleKeyValue<>();
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
                        StringBuilder str = new StringBuilder();
                        for (String e : msg.split(";")) {
                            str.append(e.trim()).append("\n");
                        }
                        str.append("boundsInParent: " + "Rect(").append(px2dp(service, parentRect.left)).append(", ").append(px2dp(service, parentRect.top)).append(" - ").append(px2dp(service, parentRect.right)).append(", ").append(px2dp(service, parentRect.bottom)).append(") " + "dp").append("\n");
                        str.append("boundsInScreen: " + "Rect(").append(px2dp(service, screenRect.left)).append(", ").append(px2dp(service, screenRect.top)).append(" - ").append(px2dp(service, screenRect.right)).append(", ").append(px2dp(service, screenRect.bottom)).append(") " + "dp").append("\n");
                        viewMessageBinding.message.setText("package: " + currentPackage + "\n" + "activity: " + currentActivity + "\n" + str.toString().trim());
                        v.setBackgroundResource(R.drawable.node_focus);
                        currentImgAndNode = new SimpleKeyValue.Entry<>(img, e);
                    } else {
                        v.setBackgroundResource(R.drawable.node);
                    }
                }
            });
            if (e.isFocused()) {
                img.requestFocus();
                ImageView imgFocus = new ImageView(service);
                imgFocus.setBackgroundResource(R.drawable.is_focus);
                widgetSelectBinding.widgets.addView(imgFocus, params);
            }
            imgAndNodes.put(img, e);
            widgetSelectBinding.widgets.addView(img, params);
        }
    }

    @SuppressLint("WrongConstant")
    public void initCapture(int resultCode, Intent data) {
        WindowManager mWindowManager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(metrics);
        mImageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2);
        MediaProjection mediaProjection = ((MediaProjectionManager) service.getSystemService(Context.MEDIA_PROJECTION_SERVICE)).getMediaProjection(resultCode, data);
        mVirtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mImageReader.getSurface(), null, null);
    }

    private Bitmap getCapture() {
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
        viewMessageBinding.group.setVisibility(visibility);
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

    public boolean canCapture() {
        return mVirtualDisplay != null && mImageReader != null;
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