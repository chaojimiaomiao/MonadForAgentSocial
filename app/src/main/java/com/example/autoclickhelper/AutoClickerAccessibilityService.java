package com.example.autoclickhelper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AutoClickerAccessibilityService extends AccessibilityService {

    private static AutoClickerAccessibilityService instance;
    private static boolean isRunning = false;
    private static boolean isExecuting = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int screenHeight, screenWidth;
    private int imageCount = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        isRunning = true;
        screenHeight = getResources().getDisplayMetrics().heightPixels;
        screenWidth = getResources().getDisplayMetrics().widthPixels;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        isRunning = false;
        isExecuting = false;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isExecuting)
            return;

        int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            //Toast.makeText(this, "onAccessibilityEvent: ", Toast.LENGTH_SHORT).show();
            //processCurrentStep();
        }
    }

    @Override
    public void onInterrupt() {
    }

    public static boolean isRunning() {
        return isRunning && instance != null;
    }

    public static void executeAutoPostTask(int imgCount) {
        if (instance == null)
            return;

        instance.startAutoPostTask(imgCount);
    }

    private void startAutoPostTask(int imgCount) {
        Toast.makeText(this, "startAutoPostTask: ", Toast.LENGTH_SHORT).show();
        /*if (isExecuting)
            return;*/
        isExecuting = true;
        openPublicAccountHelper(imgCount);
    }

    private void openPublicAccountHelper(int imgCount) {//step1
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.tencent.mp");
            if (intent == null) {
                intent = getPackageManager().getLaunchIntentForPackage("com.tencent.mm");
            }
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        imageCount = imgCount;
        handler.postDelayed(() -> clickText("贴图"), 3000);
    }

    private void clickText(String text) {//step 2,5,6,8,10
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null)
            return;

        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(text);
        if (!nodes.isEmpty()) {
            AccessibilityNodeInfo targetNode = nodes.get(0);
            if (targetNode.isClickable()) {
                performClick(targetNode);
            } else {
                AccessibilityNodeInfo parent = findClickableParent(targetNode);
                if (parent != null) {
                    performClick(parent);
                }
            }
        }
        rootNode.recycle();

        handler.postDelayed(() -> {
            Toast.makeText(this, "switch: " + text, Toast.LENGTH_SHORT).show();
            switch (text) {
                case "贴图":
                    clickSecondImage();
                    break;
                case "下一步":
                    clickYouXia();
                    break;
                case "更多设置":
                    toggleGroupNotification();
                    break;
                case "赞赏设置":
                    handler.postDelayed(() -> clickText("小猫姬"), 2000);
                    break;
                case "小猫姬":
                    handler.postDelayed(() -> clickText("确定"), 1000);
                    break;
                case "确定":
                    handler.postDelayed(() -> clickText("链接或合集"), 2000);
                    break;
                case "链接或合集":
                    handler.postDelayed(() -> clickText("添加合集"), 2000);
                    break;
                case "添加合集":
                    handler.postDelayed(() -> clickText("玩偶rundown晚报"), 1500);
                    break;
                case "玩偶rundown晚报":
                    handler.postDelayed(() -> S9fillTitle(), 1500);
                    break;
                case "发表":
                    //currentStep = 11;
                    Toast.makeText(this, "完毕。", Toast.LENGTH_SHORT).show();
                    break;
            }
        },4000);
    }

    private void clickYouXia() {
        Toast.makeText(this, "右下角", Toast.LENGTH_SHORT).show();
        //clickAt(screenWidth * 0.9f, screenHeight * 0.9f);
        clickAt(950, 2250);
        handler.postDelayed(() -> {
            if (imageCount == 5) {
                clickText("赞赏设置");
            } else {
                clickText("更多设置");
            }
        }, 4000);
    }

    private void clickSecondImage() {
        Toast.makeText(this, "选中图片", Toast.LENGTH_SHORT).show();
        if (imageCount == 1) {
            clickAt(50, 800);
            //currentStep = 4;
            handler.postDelayed(() -> {
                checkDotImage();
            }, 3000);

        } else if (imageCount == 5) {
            clickFiveImageDot();
        }
    }

    private void checkDotImage() {
        Toast.makeText(this, "checkDotImage", Toast.LENGTH_SHORT).show();
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null)
            return;

        clickAlbumImageCheckbox(rootNode);
        rootNode.recycle();
        handler.postDelayed(() -> {
            clickText("下一步");
        }, 4000);
    }

    private void clickAlbumImageCheckbox(AccessibilityNodeInfo root) {
        Toast.makeText(this, "点击圆点", Toast.LENGTH_SHORT).show();
        List<AccessibilityNodeInfo> checkboxes = root.findAccessibilityNodeInfosByViewId("com.tencent.mp:id/check_view");
        performClick(checkboxes.get(0));
    }

    private void collectAllNodes(AccessibilityNodeInfo root, List<AccessibilityNodeInfo> result) {
        if (root == null)
            return;

        result.add(root);
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            collectAllNodes(child, result);
        }
    }

    private void S9fillTitle() {
        Toast.makeText(this, "填写标题", Toast.LENGTH_SHORT).show();
        // 步骤1: 点击"填写标题"输入框位置激活它
        clickAt(220f, 970f);  // 输入框大约在屏幕上方45%位置
        handler.postDelayed(() -> {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) {
                Toast.makeText(this, "root空", Toast.LENGTH_SHORT).show();
                return;
            }
            String title = getSharedPreferences("AutoClickHelper", Context.MODE_PRIVATE
            ).getString("daily_title", "梦核科技AI早报 | " +getCurrentDate());
            
            if (imageCount == 5) {
                title = getSharedPreferences("AutoClickHelper", Context.MODE_PRIVATE
                ).getString("rundown_title", "玩偶Agents晚报: " + getCurrentDate()
                );
            }
            // 步骤2: 查找当前聚焦的输入框
            List<AccessibilityNodeInfo> allNodes = new java.util.ArrayList<>();
            collectAllNodes(rootNode, allNodes);

            for (AccessibilityNodeInfo node : allNodes) {
                // 查找可编辑且已聚焦的节点
                if (node.isEditable() && node.isFocused()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Bundle arguments = new Bundle();
                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, title);
                        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                    } else {
                        node.setText(title);
                    }
                    Toast.makeText(this, "标题填写成功", Toast.LENGTH_SHORT).show();
                    //
                    node.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS);
                    node.recycle();
                    break;
                }
            }

            rootNode.recycle();
            // 找到编辑框后，清除焦点
            //editText.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            Toast.makeText(this, "收起键盘", Toast.LENGTH_SHORT).show();

            handler.postDelayed(() -> s10fillContent(), 3000);

        }, 3000);
    }

    private void s10fillContent() {
        Toast.makeText(this, "填写内容", Toast.LENGTH_SHORT).show();
        clickAt(220f, 970f);
        handler.postDelayed(() -> {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) {
                Toast.makeText(this, "root空", Toast.LENGTH_SHORT).show();
                return;
            }
            // 步骤2: 查找当前聚焦的输入框
            List<AccessibilityNodeInfo> allNodes = new java.util.ArrayList<>();
            collectAllNodes(rootNode, allNodes);

            for (AccessibilityNodeInfo node : allNodes) {
                // 查找可编辑且已聚焦的节点
                if (node.isEditable() && node.isFocused()) {
                    node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                    node.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                    Toast.makeText(this, "内容填写成功", Toast.LENGTH_SHORT).show();
                    //
                    node.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS);
                    node.recycle();
                    break;
                }
            }

            rootNode.recycle();

            // 步骤4: 直接点击右上角发表
            handler.postDelayed(() -> clickYoushangFabiao(), 3000);

        }, 3000);
    }

    private void clickYoushangFabiao() {
        clickText("发表");
        handler.postDelayed(() -> makeSurePopup(), 4000);
    }

    private void makeSurePopup() {
        clickAt(880, 1420);
    }

    private void toggleGroupNotification() {
        clickAt(930, 450);
        handler.postDelayed(() -> {
            clickBack();
        }, 2500);
    }

    private void clickBack() {
        Toast.makeText(this, "返回", Toast.LENGTH_SHORT).show();
        clickAt(88, 220);
        handler.postDelayed(() -> S9fillTitle(), 4000);
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("M.d", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void performClick(AccessibilityNodeInfo node) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Rect rect = new Rect();
            node.getBoundsInScreen(rect);
            int x = rect.centerX();
            int y = rect.centerY();
            GestureDescription.Builder builder = new GestureDescription.Builder();
            Path path = new android.graphics.Path();
            path.moveTo(x, y);
            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));
            dispatchGesture(builder.build(), null, null);
        } else {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    private AccessibilityNodeInfo findClickableParent(AccessibilityNodeInfo node) {
        if (node == null)
            return null;
        if (node.isClickable())
            return node;
        return findClickableParent(node.getParent());
    }

    private List<AccessibilityNodeInfo> findEditableNodes(AccessibilityNodeInfo root) {
        return root.findAccessibilityNodeInfosByText("填写标题");
    }

    public void clickAt(float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 100))
                .build();

        dispatchGesture(gesture, new AccessibilityService.GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                // 点击完成
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                // 被打断（来电、弹窗等）
            }
        }, null);
    }

    //获取方格位置并排序
    private void clickFiveImageDot() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            Toast.makeText(this, "相册页面未加载", Toast.LENGTH_SHORT).show();
            return;
        }
        List<AccessibilityNodeInfo> dots =
                root.findAccessibilityNodeInfosByViewId(
                        "com.tencent.mp:id/check_view"
                );
        if (dots.size() < 5) {
            root.recycle();
            Toast.makeText(
                    this,
                    "圆点数量不足: " + dots.size(),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }
        // 按从上到下、从左到右排序
        dots.sort((a, b) -> {
            Rect rectA = new Rect();
            Rect rectB = new Rect();
            a.getBoundsInScreen(rectA);
            b.getBoundsInScreen(rectB);
            if (Math.abs(rectA.top - rectB.top) < 20) {
                return Integer.compare(rectA.left, rectB.left);
            }
            return Integer.compare(rectA.top, rectB.top);
        });
        List<Rect> targets = new ArrayList<>();
        // 只检查相册前五张图片对应的圆点
        for (int i = 0; i < 5; i++) {
            AccessibilityNodeInfo dot = dots.get(i);

            // 已选中的圆点不要再次点击，否则会取消
            if (!dot.isChecked()) {
                Rect bounds = new Rect();
                dot.getBoundsInScreen(bounds);
                targets.add(bounds);
            }
        }
        root.recycle();
        // 点击的是圆点中心，而不是图片格子中心
        for (int i = 0; i < targets.size(); i++) {
            Rect target = new Rect(targets.get(i));

            handler.postDelayed(
                    () -> clickAt(
                            target.centerX(),
                            target.centerY()
                    ),
                    i * 600L
            );
        }
        long nextDelay = targets.size() * 600L + 1500L;
        handler.postDelayed(
                () -> clickText("下一步"),
                nextDelay
        );
    }
}