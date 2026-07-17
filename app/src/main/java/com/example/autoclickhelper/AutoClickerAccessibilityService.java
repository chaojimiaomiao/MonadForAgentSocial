package com.example.autoclickhelper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AutoClickerAccessibilityService extends AccessibilityService {

    private static AutoClickerAccessibilityService instance;
    private static boolean isRunning = false;
    private static boolean isExecuting = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int currentStep = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        isRunning = true;
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
            processCurrentStep();
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
        if (isExecuting)
            return;
        isExecuting = true;
        currentStep = 0;
        openPublicAccountHelper();
    }

    private void openPublicAccountHelper() {
        handler.postDelayed(() -> {
            try {
                Intent intent = getPackageManager().getLaunchIntentForPackage("com.example.publicaccounthelper");
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
            handler.postDelayed(() -> {
                currentStep = 1;
                processCurrentStep();
            }, 3000);
        }, 500);
    }

    private void processCurrentStep() {
        handler.postDelayed(() -> {
            try {
                switch (currentStep) {
                    case 1:
                        clickText("贴图");
                        break;
                    case 2:
                        selectFirstImage();
                        break;
                    case 3:
                        clickText("下一步");
                        break;
                    case 4:
                        clickText("完成");
                        break;
                    case 5:
                        fillTitle();
                        break;
                    case 6:
                        clickText("更多设置");
                        break;
                    case 7:
                        toggleGroupNotification();
                        break;
                    case 8:
                        clickText("发布");
                        break;
                    default:
                        isExecuting = false;
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1000);
    }

    private void clickText(String text) {
        
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
            currentStep++;
        }

        rootNode.recycle();
    }

    private void selectFirstImage() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null)
            return;

        List<AccessibilityNodeInfo> imageNodes = rootNode.findAccessibilityNodeInfosByViewId("android:id/icon");
        if (!imageNodes.isEmpty()) {
            AccessibilityNodeInfo firstImage = imageNodes.get(0);
            if (firstImage.isClickable()) {
                performClick(firstImage);
            } else {
                AccessibilityNodeInfo parent = findClickableParent(firstImage);
                if (parent != null) {
                    performClick(parent);
                }
            }
        } else {
            List<AccessibilityNodeInfo> allNodes = rootNode.findAccessibilityNodeInfosByText("");
            for (AccessibilityNodeInfo node : allNodes) {
                if (node.getClassName().toString().contains("ImageView") && node.isClickable()) {
                    performClick(node);
                    break;
                }
            }
        }

        currentStep++;
        rootNode.recycle();

        handler.postDelayed(() -> {
            currentStep++;
            processCurrentStep();
        }, 2000);
    }

    private void fillTitle() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null)
            return;

        String title = "梦核科技AI日报|" + getCurrentDate();

        List<AccessibilityNodeInfo> editTexts = rootNode.findAccessibilityNodeInfosByViewId("android:id/edit");
        if (editTexts.isEmpty()) {
            editTexts = rootNode.findAccessibilityNodeInfosByViewId("com.example.publicaccounthelper:id/title");
        }
        if (editTexts.isEmpty()) {
            editTexts = findEditableNodes(rootNode);
        }

        if (!editTexts.isEmpty()) {
            AccessibilityNodeInfo editText = editTexts.get(0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Bundle arguments = new Bundle();
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, title);
                editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            }
        }

        currentStep++;
        rootNode.recycle();

        handler.postDelayed(() -> processCurrentStep(), 1000);
    }

    private void toggleGroupNotification() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null)
            return;

        List<AccessibilityNodeInfo> toggleNodes = rootNode
                .findAccessibilityNodeInfosByViewId("android:id/switch_widget");
        if (!toggleNodes.isEmpty()) {
            for (AccessibilityNodeInfo toggle : toggleNodes) {
                if (toggle.isChecked()) {
                    performClick(toggle);
                    break;
                }
            }
        }

        currentStep++;
        rootNode.recycle();

        handler.postDelayed(() -> {
            clickText("返回");
        }, 1000);
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
}