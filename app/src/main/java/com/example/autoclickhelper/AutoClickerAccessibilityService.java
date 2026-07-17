package com.example.autoclickhelper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AutoClickerAccessibilityService extends AccessibilityService {

    private static AutoClickerAccessibilityService instance;
    private static boolean isRunning = false;
    private static boolean isExecuting = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int screenHeight, screenWidth;

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

    public static void executeAutoPostTask() {
        if (instance == null)
            return;
        instance.startAutoPostTask();
    }

    private void startAutoPostTask() {
        Toast.makeText(this, "startAutoPostTask: ", Toast.LENGTH_SHORT).show();
        if (isExecuting)
            return;
        isExecuting = true;
        openPublicAccountHelper();
    }

    private void openPublicAccountHelper() {//step1
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
                    //clickText("完成");
                    clickYouXia();
                    break;
                /*case "完成":
                    fillTitle();
                    break;*/
                case "更多设置":
                    toggleGroupNotification();
                    break;
                case "发布":
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
            fillTitle();
        }, 3000);
    }

    private void clickSecondImage() {
        Toast.makeText(this, "选中图片", Toast.LENGTH_SHORT).show();
//        GestureDescription.Builder builder = new GestureDescription.Builder();
//        Path path = new android.graphics.Path();
//        path.moveTo(20, 200);
//        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));
//        dispatchGesture(builder.build(), null, null);
        clickAt(50, 800);
        //currentStep = 4;
        handler.postDelayed(() -> {
            checkDotImage();
        }, 3000);
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
        List<AccessibilityNodeInfo> checkboxes = root.findAccessibilityNodeInfosByViewId("android:id/checkbox");
        if (!checkboxes.isEmpty()) {
            int targetIndex = checkboxes.size() > 3 ? 3 : 0;
            AccessibilityNodeInfo checkbox = checkboxes.get(targetIndex);
            performClick(checkbox);
            checkbox.recycle();
            return;
        }
        List<AccessibilityNodeInfo> allNodes = new java.util.ArrayList<>();
        collectAllNodes(root, allNodes);
        for (AccessibilityNodeInfo node : allNodes) {
            String className = node.getClassName().toString();
            if ((className.contains("CheckBox") || className.contains("check")) && node.isClickable()) {
                performClick(node);
                node.recycle();
                return;
            }
        }
        for (AccessibilityNodeInfo node : allNodes) {
            if (node.isClickable() && node.getContentDescription() != null) {
                String desc = node.getContentDescription().toString();
                if (desc.contains("选择") || desc.contains("check")) {
                    performClick(node);
                    node.recycle();
                    return;
                }
            }
        }
        for (AccessibilityNodeInfo node : allNodes) {
            if (node.isClickable()) {
                Rect bounds = new Rect();
                node.getBoundsInScreen(bounds);
                if (bounds.right > screenWidth * 0.8 && bounds.top < screenHeight * 0.3) {
                    performClick(node);
                    node.recycle();
                    return;
                }
            }
        }
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

    private void fillTitle() {
        Toast.makeText(this, "填写标题", Toast.LENGTH_SHORT).show();

        // 步骤1: 点击"填写标题"输入框位置激活它
        clickAt(220f, 970f);  // 输入框大约在屏幕上方45%位置

        handler.postDelayed(() -> {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) {
                Toast.makeText(this, "root空", Toast.LENGTH_SHORT).show();
                return;
            }

            String title = "梦核科技AI日报|" + getCurrentDate();

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
                    node.recycle();
                    break;
                }
            }

            rootNode.recycle();

            // 步骤3: 延迟后收起键盘
            handler.postDelayed(() -> {
                // 点击屏幕顶部空白区域收起键盘
                clickAt(screenWidth / 2f, 100f);
                Toast.makeText(this, "收起键盘", Toast.LENGTH_SHORT).show();

                // 步骤4: 延迟后点击"更多设置"
                handler.postDelayed(() -> clickText("更多设置"), 1000);
            }, 500);
        }, 1000);
    }

    private void fillTitle1() {
        Toast.makeText(this, "填写标题", Toast.LENGTH_SHORT).show();
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Toast.makeText(this, "root空", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = "梦核科技AI日报| 今日";// + getCurrentDate();

        List<AccessibilityNodeInfo> editTexts = rootNode.findAccessibilityNodeInfosByViewId("android:id/edit");
        if (editTexts.isEmpty()) {
            editTexts = rootNode.findAccessibilityNodeInfosByViewId("com.example.publicaccounthelper:id/title");
        }
        if (editTexts.isEmpty()) {
            editTexts = findEditableNodes(rootNode);
        }

        if (!editTexts.isEmpty()) {
            Toast.makeText(this, "不为空", Toast.LENGTH_SHORT).show();
            AccessibilityNodeInfo editText = editTexts.get(0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Bundle arguments = new Bundle();
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, title);
                editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            } else {
                editText.setText(title);
            }
        }
        rootNode.recycle();
        handler.postDelayed(() -> clickText("更多设置"), 3000);
    }

    private void toggleGroupNotification() {
        clickAt(930, 450);
        handler.postDelayed(() -> {
            clickAt(50, 200);
            Toast.makeText(this, "返回", Toast.LENGTH_SHORT).show();
            clickText("发布");
        }, 4000);
    }

    private void toggleGroupNotification1() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Toast.makeText(this, "root空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 方式1: 查找"群发通知"文字，然后点击它右边的区域
        List<AccessibilityNodeInfo> textNodes = rootNode.findAccessibilityNodeInfosByText("群发通知");
        if (!textNodes.isEmpty()) {
            AccessibilityNodeInfo textNode = textNodes.get(0);
            Rect textBounds = new Rect();
            textNode.getBoundsInScreen(textBounds);

            // 点击文字右侧（toggle位置）
            int toggleX = textBounds.right + 100;
            int toggleY = textBounds.centerY();
            clickAt(toggleX, toggleY);
        }

        // 方式2: 直接点击屏幕右侧位置（兜底）
        else {
            //clickAt(screenWidth * 0.85f, screenHeight * 0.15f);
            clickAt(930, 450);
        }

        rootNode.recycle();

        handler.postDelayed(() -> {
            clickAt(50, 200);  // 返回
        }, 3500);
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMdd", Locale.getDefault());
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
}