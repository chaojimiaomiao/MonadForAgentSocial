package com.example.autoclickhelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AiDailyService {

    private static final String TAG = "AiDaily";
    private static final String BASE_URL = "https://aidaily.modao.xyz";

    // 线程池处理网络请求
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * 完整流程：先生成日报，再下载图片
     */
    public void generateAndDownload(Context context, OnCompleteListener listener) {
        executor.execute(() -> {
            try {
                // 第一步：触发生成（GET）
                String generateUrl = BASE_URL + "/generate?date=" + getTodayDate();
                Log.d(TAG, "Generating: " + generateUrl);

                String generateResponse = httpGet(generateUrl);
                Log.d(TAG, "Generate response: " + generateResponse);

                // 解析JSON，获取图片URL
                /*String imageUrl = parseImageUrl(generateResponse);
                if (imageUrl == null) {
                    // 如果JSON解析失败，用默认路径
                    imageUrl = BASE_URL + "/latestimg";
                }*/

                // 等待2秒确保生成完成（服务器需要时间截图+裁剪）
                Thread.sleep(2000);

                // 第二步：下载图片
                Log.d(TAG, "Downloading: " + BASE_URL + "/latestimg");
                Bitmap bitmap = downloadBitmap(BASE_URL + "/latestimg");

                // 保存到本地相册
                if (bitmap != null) {
                    String savedPath = saveToGallery(context, bitmap, "daily_post_" + getTodayDate() + ".png");
                    mainHandler.post(() -> {
                        if (listener != null)
                            listener.onSuccess(savedPath, bitmap);
                    });
                } else {
                    mainHandler.post(() -> {
                        if (listener != null)
                            listener.onError("图片下载失败");
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

    /**
     * 只下载最新图片（不重新生成）
     */
    public void downloadLatest(Context context, OnCompleteListener listener) {
        executor.execute(() -> {
            try {
                String imageUrl = BASE_URL + "/latestimg";
                Log.d(TAG, "Downloading latest: " + imageUrl);

                Bitmap bitmap = downloadBitmap(imageUrl);

                if (bitmap != null) {
                    String savedPath = saveToGallery(context, bitmap, "daily_post_latest.png");
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
                response.append(line);
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

    // === 回调接口 ===

    public interface OnCompleteListener {
        void onSuccess(String savedPath, Bitmap bitmap);

        void onError(String error);
    }

    // 清理资源
    public void shutdown() {
        executor.shutdown();
    }
}

// === 使用示例（Activity中） ===

/*
 * public class MainActivity extends AppCompatActivity {
 * 
 * private AiDailyService aiDailyService;
 * 
 * @Override
 * protected void onCreate(Bundle savedInstanceState) {
 * super.onCreate(savedInstanceState);
 * setContentView(R.layout.activity_main);
 * 
 * aiDailyService = new AiDailyService();
 * 
 * // 点击按钮：生成并下载日报
 * findViewById(R.id.btn_generate).setOnClickListener(v -> {
 * aiDailyService.generateAndDownload(this, new
 * AiDailyService.OnCompleteListener() {
 * 
 * @Override
 * public void onSuccess(String savedPath, Bitmap bitmap) {
 * Toast.makeText(MainActivity.this,
 * "日报已保存: " + savedPath, Toast.LENGTH_LONG).show();
 * 
 * // 显示到ImageView
 * ImageView imageView = findViewById(R.id.imageView);
 * imageView.setImageBitmap(bitmap);
 * }
 * 
 * @Override
 * public void onError(String error) {
 * Toast.makeText(MainActivity.this,
 * "失败: " + error, Toast.LENGTH_LONG).show();
 * }
 * });
 * });
 * 
 * // 点击按钮：只下载最新（不重新生成）
 * findViewById(R.id.btn_download).setOnClickListener(v -> {
 * aiDailyService.downloadLatest(this, new AiDailyService.OnCompleteListener() {
 * 
 * @Override
 * public void onSuccess(String savedPath, Bitmap bitmap) {
 * Toast.makeText(MainActivity.this,
 * "已下载: " + savedPath, Toast.LENGTH_LONG).show();
 * }
 * 
 * @Override
 * public void onError(String error) {
 * Toast.makeText(MainActivity.this,
 * "失败: " + error, Toast.LENGTH_LONG).show();
 * }
 * });
 * });
 * }
 * 
 * @Override
 * protected void onDestroy() {
 * super.onDestroy();
 * aiDailyService.shutdown();
 * }
 * }
 */

// === AndroidManifest.xml 权限 ===

/*
 * <uses-permission android:name="android.permission.INTERNET" />
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 * 
 * <!-- Android 9及以下需要存储权限 -->
 * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
 * android:maxSdkVersion="28" />
 * <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
 * android:maxSdkVersion="28" />
 */
