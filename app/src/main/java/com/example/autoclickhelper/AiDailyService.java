package com.example.autoclickhelper;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.ArrayList;
import java.util.List;
public class AiDailyService {
    private static final String TAG = "AiDaily";
    private static final String BASE_URL = "https://aidaily.modao.xyz";
    // 线程池处理网络请求
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * 完整流程：先生成日报，再下载图片。使用coze改版后：每日在8：25分固定链接下载一张图
     */
    public void downloadPicAndTxt(Context context, String url, OnCompleteListener listener) {
        if (url.equals(MainActivity.mySummary)) {
            downloadTextAndCopy(context, url, new OnTextCompleteListener() {
                @Override
                public void onSuccess(String text) {
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(context, "复制失败", Toast.LENGTH_LONG).show();
                }
            });
            downloadLatestOnePic(context,listener);//梦核版本
        } else {
            executor.execute(() -> {
                try {
                    String html = httpGet(url);
                    RundownPage page = parseRundownHtml(html, url);
                    int index = 1;
                    for (RundownImage image : page.images) {
                        Bitmap bitmap = downloadBitmap(image.url);

                        String filename = String.format(Locale.CHINA,
                                "rundown_ai_%02d.png",
                                index++
                        );

                        saveToGallery(context, bitmap, filename);
                        bitmap.recycle();
                    }
                    String publishTitle = page.heading;
                    context.getSharedPreferences(
                                    "AutoClickHelper", Context.MODE_PRIVATE).edit()
                            .putString("rundown_title", publishTitle)
                            .apply();
                    mainHandler.post(() -> {
                        String sdfStr = getCurrentDay();
                        copyToClipboard(context, sdfStr + " 玩偶 Agents 晚报：\n" + page.summary);
                        listener.onSuccess(page.images.size() +","+ page.summary,
                                null
                        );
                    });
                } catch (Exception e) {
                    mainHandler.post(() ->
                            listener.onError(e.getMessage())
                    );
                }
            });
        }

    }

    private String getCurrentDay() {
        SimpleDateFormat sdf = new SimpleDateFormat("M.d日", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * 只下载最新图片（不重新生成）
     */
    public void downloadLatestOnePic(Context context, OnCompleteListener listener) {
        executor.execute(() -> {
            try {
                Bitmap bitmap = downloadBitmap(MainActivity.myDailyPic);
                if (bitmap != null) {
                    String savedPath = saveToGallery(context, bitmap, "ai_daily_latest.png");
                    mainHandler.post(() -> {
                        if (listener != null)
                            listener.onSuccess(savedPath, bitmap);
                    });
                } else {
                    mainHandler.post(() -> {
                        if (listener != null)
                            listener.onError("下载失败");
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error", e);
                mainHandler.post(() -> {
                    if (listener != null)
                        listener.onError(e.getMessage());
                });
            }
        });
    }

    public void downloadTextAndCopy(Context context, String url, OnTextCompleteListener listener) {
        executor.execute(() -> {
            try {
                String html = httpGet(url);
                String text = getCurrentDay() + extractTextFromHtml(html);
                String title = Jsoup.parse(html).title();
                context.getSharedPreferences("AutoClickHelper", Context.MODE_PRIVATE)
                        .edit().putString("daily_title", title).apply();
                mainHandler.post(() -> {
                    copyToClipboard(context, text);
                    if (listener != null) {
                        listener.onSuccess(text);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onError(e.getMessage());
                    }
                });
            }
        });
    }
    public interface OnTextCompleteListener {
        void onSuccess(String text);
        void onError(String error);
    }

    private void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("贴图号正文", text);
        clipboard.setPrimaryClip(clip);
    }

    // === 网络请求 ===

    private String httpGet(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(30000); // 30秒超时
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Accept", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append('\n');
            }
            reader.close();
            return response.toString();
        } else {
            throw new IOException("HTTP " + responseCode);
        }
    }

    private Bitmap downloadBitmap(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            InputStream input = conn.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();
            return bitmap;
        }
        return null;
    }

    // === 解析 ===

    private String parseImageUrl(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            if (json.has("image_url")) {
                String path = json.getString("image_url");
                // 如果返回的是相对路径，拼接完整URL
                if (path.startsWith("/")) {
                    return BASE_URL + path;
                }
                return path;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Parse error", e);
        }
        return null;
    }

    private String getTodayDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new java.util.Date());
    }

    // === 保存到相册 ===

    private String saveToGallery(Context context, Bitmap bitmap, String filename) throws IOException {
        // Android 10+ 用 MediaStore
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/梦核科技AI日报");

            android.net.Uri uri = context.getContentResolver().insert(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values);

            if (uri != null) {
                OutputStream out = context.getContentResolver().openOutputStream(uri);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                if (out != null)
                    out.close();
                return uri.toString();
            }
        } else {
            // Android 9 及以下，直接写文件
            File dir = new File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_PICTURES),
                    "梦核科技AI日报");
            if (!dir.exists())
                dir.mkdirs();

            File file = new File(dir, filename);
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();

            // 通知相册扫描
            context.sendBroadcast(new android.content.Intent(
                    android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    android.net.Uri.fromFile(file)));

            return file.getAbsolutePath();
        }
        return null;
    }

    private String extractTextFromHtml(String html) throws IOException {
        Pattern pattern = Pattern.compile(
                "<pre\\b[^>]*>(.*?)</pre>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(html);
        if (!matcher.find()) {
            throw new IOException("没有找到正文内容");
        }
        /*String preHtml = matcher.group(1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(
                    preHtml,
                    Html.FROM_HTML_MODE_LEGACY
            ).toString();
        }*/
        return matcher.group(1).trim();
    }

    // === 回调接口 ===

    public interface OnCompleteListener {
        void onSuccess(String savedPath, Bitmap bitmap);

        void onError(String error);
    }

    // 清理资源
    public void shutdown() {
        executor.shutdown();
    }


    private static class RundownImage {
        final String url;
        final String alt;

        RundownImage(String url, String alt) {
            this.url = url;
            this.alt = alt;
        }
    }

    private static class RundownPage {
        final String title;
        final String heading;
        final String summary;
        final String footer;
        final List<RundownImage> images;

        RundownPage(
                String title,
                String heading,
                String summary,
                String footer,
                List<RundownImage> images) {
            this.title = title;
            this.heading = heading;
            this.summary = summary;
            this.footer = footer;
            this.images = images;
        }
    }

    private RundownPage parseRundownHtml(
            String html,
            String pageUrl) throws IOException {

        Document document = Jsoup.parse(html, pageUrl);

        Element container = document.selectFirst("body > div.container");
        if (container == null) {
            throw new IOException("缺少 div.container");
        }

        Element headingNode = container.selectFirst("div.header > h1");
        Element summaryNode = container.selectFirst("div.header > div.sub");
        Element footerNode = container.selectFirst("div.footer");

        if (headingNode == null || summaryNode == null || footerNode == null) {
            throw new IOException("页面标题、摘要或页脚结构不完整");
        }

        Elements imageNodes =
                container.select("div.card-img > img[src]");

        if (imageNodes.isEmpty()) {
            throw new IOException("没有找到日报图片");
        }

        List<RundownImage> images = new ArrayList<>();

        for (Element imageNode : imageNodes) {
            String imageUrl = imageNode.absUrl("src");
            String alt = imageNode.attr("alt").trim();

            if (imageUrl.isEmpty()) {
                throw new IOException("存在空的图片地址");
            }

            images.add(new RundownImage(imageUrl, alt));
        }
        String summary = summaryNode != null
                ? summaryNode.wholeText().trim()
                : "";
        return new RundownPage(
                document.title(),
                headingNode.text(),
                summary,
                footerNode.text(),
                images
        );
    }




}
