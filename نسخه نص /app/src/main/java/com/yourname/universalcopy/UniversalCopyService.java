package com.yourname.universalcopy; // ⚠️ تأكد من اسم حزمتك

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;

// مكتبات إعلان المكافأة ياندكس
import com.yandex.mobile.ads.common.MobileAds;
import com.yandex.mobile.ads.common.AdRequestConfiguration;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoader;

// مكتبات الترجمة ML Kit
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

public class UniversalCopyService extends AccessibilityService {

    private WindowManager windowManager;        
    private SmartPopupContainer popupWrapper; 
    private View popupView;        
    private SelectionOverlayView selectionOverlayView;        
    private static UniversalCopyService instance;        
    
    private boolean isSelectionModeActive = false;        
    private boolean isPopupShowing = false; 
    
    public static RewardedAd mReadyYandexRewardedAd = null;
    private RewardedAdLoader mRewardedAdLoader = null;
            
    private final Handler mainHandler = new Handler(Looper.getMainLooper());        
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();        
        
   @Override        
   protected void onServiceConnected() {        
    super.onServiceConnected();        
    
    //🛡️ 1. الدرع الأمني (تم التعطيل مؤقتاً لكي تعمل الخدمة أثناء التجربة)
     if (!axv.isAppSecure(this)) {
        android.util.Log.e("SECURITY", "Tampered App Detected! Killing Service...");
        stopSelf(); 
         return;
     }

    // 2. إعدادات النافذة والنسخة
    windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);        
    instance = this;  
    
    // 3. تهيئة إعلانات ياندكس
    MobileAds.setAgeRestrictedUser(true);
    MobileAds.initialize(this, () -> {
        mainHandler.post(() -> {
            mRewardedAdLoader = new RewardedAdLoader(this);
            mRewardedAdLoader.setAdLoadListener(new RewardedAdLoadListener() {
                @Override
                public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                    mReadyYandexRewardedAd = rewardedAd; 
                }
                @Override
                public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                    mReadyYandexRewardedAd = null;
                }
            });
            // تحميل الإعلان الأول مسبقاً
            preloadNextAd();
        });
    });
}      
    
    public void preloadNextAd() {
        if (mReadyYandexRewardedAd == null && mRewardedAdLoader != null) {
            // 🛡️ تشفير كود الإعلان
            String decryptedAdId = axv.decryptData(this, "+28VtDvsdybRfq7rTHGKoYl5JUAEOddyXjqXQTQyZCvWL27DSrMDZfeqcSfRtKmQ53YbLl/6wWUeXmImv5IWRsDg1cJAL5Mpy9kfHtZoL9dN5WIWklb+YpWDLf/D1Gyyuk3hG3MbSV0Zgbpon5fRuOUbH2xkbNwqO0AkizkKh9bY8XUcQjP3NjCx4LIumP8G7aRr0a/1OUypsY3Ie/DbJ+Lpy762QPaZ+88ahXl/Xr0Uxz1RUaKWfBi+MBnEAocMLSyxeUx33ICr4BEW6HK2py9ffVw1P5Z7gcBQRfda74xhmz56Fq/JhMe8SbbwVvBnsAT/p/DUfUG1PYFVgBMBQaPKqJVOWtr/eoaXpcVuOyp5nWvxOfQR9ZXDtxVaUCb7/bq35B6Ur0S3jUkoXAtw7Tt2Lleud/cim5wjzRvwAX183Uf8ZJY5JDSzwkFwRWdYbBfiZ7B2hdUarE4gi5qfunFniEKLXbsUJw==");
            
            // حماية من الانهيار إذا كان التطبيق معدلاً
            if (decryptedAdId == null || decryptedAdId.startsWith("ERR_")) {
                decryptedAdId = "INVALID_TAMPERED_AD"; 
            }
            
            AdRequestConfiguration adReq = new AdRequestConfiguration.Builder(decryptedAdId).build();
            mRewardedAdLoader.loadAd(adReq);
        }
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // 🛡️ نظام النقاط المشفر
    private boolean consumeUse() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        
        // قراءة الطلاسم من المفتاح المموه
        String encryptedUses = prefs.getString("sys_cfg_v2", null);
        int uses = 0;
        
        if (encryptedUses != null) {
            try {
                String decryptedStr = axv.decryptData(this, encryptedUses);
                uses = Integer.parseInt(decryptedStr);
            } catch (Exception e) {
                uses = 0; // عقاب للمخترق
            }
        } else {
            uses = 10; // المستخدم الجديد يأخذ 10 محاولات
        }
        
        if (uses > 0) {
            uses--; 
            
            // إعادة التشفير والحفظ
            String newEncryptedUses = axv.encryptData(String.valueOf(uses));
            if (newEncryptedUses != null) {
                prefs.edit().putString("sys_cfg_v2", newEncryptedUses).apply();
            }
            return true; 
            
        } else {
            if (!isNetworkAvailable(this)) {
                mainHandler.post(() -> Toast.makeText(this, getString(R.string.toast_offline_free), Toast.LENGTH_LONG).show());
                return true; 
            } else {
                closeAndResetMode();
                Intent adIntent = new Intent(this, AdActivity.class);
                adIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(adIntent);
                return false; 
            }
        }
    }
        
    public static UniversalCopyService getInstance() { return instance; }        
    public boolean isSelectionModeActive() { return isSelectionModeActive; }        
        
    public static void toggleCopyMode(boolean isActive) {        
        if (instance != null) {        
            if (isActive && instance.isSelectionModeActive) return;        
            instance.isSelectionModeActive = isActive;        
            if (isActive) {        
                try { instance.performGlobalAction(15); } catch (Exception e) {}        
                try { instance.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)); } catch (Exception e) {}        
                instance.mainHandler.postDelayed(() -> instance.startSelectionMode(), 500);        
            } else {        
                instance.closeAndResetMode();        
            }        
        }        
    }        
        
    public static void toggleImageCopyMode(boolean isActive) {        
        if (instance != null && isActive) { instance.isSelectionModeActive = false; }        
    }        
        
    private void startSelectionMode() {        
        removeOverlays();        
        List<TextNode> textNodes = new ArrayList<>();        
        List<AccessibilityWindowInfo> windows = getWindows();
        if (windows != null && !windows.isEmpty()) {
            for (AccessibilityWindowInfo window : windows) {
                AccessibilityNodeInfo rootNode = window.getRoot();
                if (rootNode != null) extractTextNodes(rootNode, textNodes);
            }
        } else {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();        
            if (rootNode != null) extractTextNodes(rootNode, textNodes);        
        }
        
        if (textNodes.isEmpty()) {        
            Toast.makeText(this, getString(R.string.toast_no_text), Toast.LENGTH_SHORT).show();        
            isSelectionModeActive = false;        
            return;        
        }        
        
        selectionOverlayView = new SelectionOverlayView(this, textNodes);        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(        
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, 
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,        
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,        
                PixelFormat.TRANSLUCENT);        
        try { windowManager.addView(selectionOverlayView, params); } catch (Exception e) { isSelectionModeActive = false; }        
    }        
        
    private void extractTextNodes(AccessibilityNodeInfo node, List<TextNode> textNodes) {        
        if (node == null) return;        
        if (node.getPackageName() != null && node.getPackageName().toString().contains("com.android.systemui")) return;
        CharSequence text = node.getText();        
        CharSequence contentDesc = node.getContentDescription();        
        String extractedText = null;        
        if (text != null && text.toString().trim().length() > 0) extractedText = text.toString().trim();        
        else if (contentDesc != null && contentDesc.toString().trim().length() > 0) extractedText = contentDesc.toString().trim();        
        if (extractedText != null) {        
            Rect bounds = new Rect();        
            node.getBoundsInScreen(bounds);        
            if (bounds.width() > 5 && bounds.height() > 5 && bounds.bottom > 0 && bounds.right > 0) {        
                boolean isDuplicate = false;
                for (TextNode existingNode : textNodes) {
                    if (existingNode.rect.equals(bounds) && existingNode.text.equals(extractedText)) { isDuplicate = true; break; }
                }
                if (!isDuplicate) textNodes.add(new TextNode(bounds, extractedText));        
            }        
        }        
        for (int i = 0; i < node.getChildCount(); i++) extractTextNodes(node.getChild(i), textNodes);        
    }        
        
    private void showPopupWindow(String textContent) {        
        if (popupWrapper != null) { windowManager.removeView(popupWrapper); popupWrapper = null; popupView = null; }
        popupWrapper = new SmartPopupContainer(this);
        popupView = LayoutInflater.from(this).inflate(R.layout.popup_layout, popupWrapper, false);        
        
        EditText editText = popupView.findViewById(R.id.edit_text_popup_content);        
        editText.setText(textContent); 
        
        editText.setTextIsSelectable(true);
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        
        ImageButton btnClose = popupView.findViewById(R.id.btn_close_popup);
        if (btnClose != null) btnClose.setOnClickListener(v -> closePopupOnly());
        
        int buttonBackgroundColor = Color.parseColor("#333333"); 
        int buttonIconColor = Color.WHITE; 

        ImageButton btnTranslate = popupView.findViewById(R.id.btn_translate_popup);        
        btnTranslate.setBackgroundColor(buttonBackgroundColor); btnTranslate.setColorFilter(buttonIconColor);
        btnTranslate.setOnClickListener(v -> { if(consumeUse()) handleTranslation(editText, false); });        
        btnTranslate.setOnLongClickListener(v -> { if(consumeUse()) handleTranslation(editText, true); return true; });        
        
        ImageButton btnCopy = popupView.findViewById(R.id.btn_copy_final);        
        btnCopy.setBackgroundColor(buttonBackgroundColor); btnCopy.setColorFilter(buttonIconColor);
        btnCopy.setOnClickListener(v -> {        
            if(!consumeUse()) return; 
            
            int start = editText.getSelectionStart();
            int end = editText.getSelectionEnd();
            String textToCopy = "";
            
            if (start != -1 && end != -1 && start != end) {
                textToCopy = editText.getText().toString().substring(Math.min(start, end), Math.max(start, end));
            } else {
                textToCopy = editText.getText().toString();
            }
            
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);        
            clipboard.setPrimaryClip(ClipData.newPlainText("Copied Text", textToCopy));        
            Toast.makeText(this, getString(R.string.toast_copied), Toast.LENGTH_SHORT).show();        
            
            closeAndResetMode(); 
        });        
        
        ImageButton btnShare = popupView.findViewById(R.id.btn_share_popup);        
        btnShare.setBackgroundColor(buttonBackgroundColor); btnShare.setColorFilter(buttonIconColor);
        btnShare.setOnClickListener(v -> {        
            if(!consumeUse()) return;
            
            int start = editText.getSelectionStart();
            int end = editText.getSelectionEnd();
            String textToShare = "";
            
            if (start != -1 && end != -1 && start != end) {
                textToShare = editText.getText().toString().substring(Math.min(start, end), Math.max(start, end));
            } else {
                textToShare = editText.getText().toString();
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);        
            shareIntent.setType("text/plain");        
            shareIntent.putExtra(Intent.EXTRA_TEXT, textToShare);        
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);        
            startActivity(shareIntent);        
            closePopupOnly();        
        });        
        
        ImageButton btnMagic = popupView.findViewById(R.id.btn_clear_magic);        
        btnMagic.setBackgroundColor(buttonBackgroundColor); btnMagic.setColorFilter(buttonIconColor);
        btnMagic.setOnClickListener(v -> {        
            if(!consumeUse()) return;
            editText.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC));        
        });        
        
        popupWrapper.addView(popupView);
        popupWrapper.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Rect popupRect = new Rect();
                popupView.getGlobalVisibleRect(popupRect);
                if (!popupRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    closePopupOnly(); return true;
                }
            }
            return false;
        });
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(        
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,         
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,        
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);        
        params.gravity = Gravity.BOTTOM;
        try { windowManager.addView(popupWrapper, params); isPopupShowing = true; } catch (Exception e) { closePopupOnly(); }        
    }        
        
    private void handleTranslation(EditText editText, boolean forceOffline) {        
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        String textToTranslate = "";
        
        if (start != -1 && end != -1 && start != end) {
            textToTranslate = editText.getText().toString().substring(Math.min(start, end), Math.max(start, end));
        } else {
            textToTranslate = editText.getText().toString();
        }

        if (textToTranslate.trim().isEmpty()) return;        
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);        
        String targetLang = prefs.getString("target_lang", "ar");        
        
        if (forceOffline) {        
            editText.setText(getString(R.string.txt_translating_forced));         
            performOfflineTranslation(textToTranslate, targetLang, editText);        
        } else {        
            editText.setText(getString(R.string.txt_translating));         
            
            final String finalTextToTranslate = textToTranslate;
            
            backgroundExecutor.execute(() -> {        
                try {        
                    String encodedText = URLEncoder.encode(finalTextToTranslate, "UTF-8");        
                    String urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=" + targetLang + "&dt=t&q=" + encodedText;        
                    HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();        
                    
                    conn.setConnectTimeout(5000);         
                    conn.setReadTimeout(5000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");        
                    
                    if (conn.getResponseCode() != 200) {
                        throw new Exception("HTTP Error");
                    }

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));        
                    StringBuilder response = new StringBuilder(); 
                    String inputLine;        
                    while ((inputLine = in.readLine()) != null) response.append(inputLine);        
                    in.close();        
                    
                    JSONArray jsonArray = new JSONArray(response.toString()).getJSONArray(0);        
                    StringBuilder translation = new StringBuilder();        
                    for (int i = 0; i < jsonArray.length(); i++) {
                        translation.append(jsonArray.getJSONArray(i).getString(0));        
                    }
                    mainHandler.post(() -> editText.setText(translation.toString()));        
                } catch (Exception e) {        
                    mainHandler.post(() -> {        
                        editText.setText(getString(R.string.txt_translating_offline));        
                        performOfflineTranslation(finalTextToTranslate, targetLang, editText);        
                    });        
                }        
            });        
        }        
    }        
        
    // 🔥 تم تعديل النصوص الثابتة لدعم اللغات المتعددة
    private void performOfflineTranslation(String text, String targetLang, EditText editText) {        
        LanguageIdentifier languageIdentifier = LanguageIdentification.getClient();        
        languageIdentifier.identifyLanguage(text).addOnSuccessListener(sourceLang -> {        
            
            if (sourceLang == null || sourceLang.equals("und")) { 
                mainHandler.post(() -> {
                    editText.setText(text); 
                    Toast.makeText(this, getString(R.string.toast_lang_not_recognized), Toast.LENGTH_SHORT).show();
                });
                return; 
            }        

            // إذا كان النص بنفس اللغة التي تريد الترجمة إليها
            if (sourceLang.equals(targetLang)) {
                mainHandler.post(() -> editText.setText(text));
                return;
            }

            TranslatorOptions options = new TranslatorOptions.Builder().setSourceLanguage(sourceLang).setTargetLanguage(targetLang).build();        
            final Translator translator = Translation.getClient(options);        
            
            // الترجمة المباشرة
            translator.translate(text).addOnSuccessListener(res -> {        
                mainHandler.post(() -> editText.setText(res)); 
                translator.close();        
            }).addOnFailureListener(e -> {
                mainHandler.post(() -> {
                    editText.setText(text); 
                    Toast.makeText(this, getString(R.string.toast_model_not_downloaded), Toast.LENGTH_LONG).show();
                });
                translator.close();
            });        

        }).addOnFailureListener(e -> {
            mainHandler.post(() -> {
                editText.setText(text);
                Toast.makeText(this, getString(R.string.toast_engine_error), Toast.LENGTH_SHORT).show();
            });
        });        
    }        
    
    public void closePopupOnly() {
        isPopupShowing = false;
        mainHandler.post(() -> {
            if (popupWrapper != null) {
                try { windowManager.removeView(popupWrapper); } catch (Exception e) {}
                popupWrapper = null;
                popupView = null;
            }
            if (selectionOverlayView != null) selectionOverlayView.clearSelection(); 
        });
    }

    public void closeAndResetMode() {        
        isSelectionModeActive = false;        
        isPopupShowing = false;
        mainHandler.post(this::removeOverlays);        
    }        
        
    private void removeOverlays() {        
        try {        
            if (popupWrapper != null) { windowManager.removeView(popupWrapper); popupWrapper = null; popupView = null; }        
            if (selectionOverlayView != null) { windowManager.removeView(selectionOverlayView); selectionOverlayView = null; }        
        } catch (Exception e) {}        
    }        
        
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}        
    @Override public void onInterrupt() { closeAndResetMode(); }        
    @Override public void onDestroy() {        
        super.onDestroy(); closeAndResetMode(); backgroundExecutor.shutdown(); instance = null;        
    }        
        
    private class SmartPopupContainer extends FrameLayout {
        public SmartPopupContainer(Context context) { super(context); }
        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                closePopupOnly(); return true;
            }
            return super.dispatchKeyEvent(event);
        }
    }

    private static class TextNode {        
        Rect rect; String text;        
        TextNode(Rect rect, String text) { this.rect = rect; this.text = text; }        
    }        
        
    private class SelectionOverlayView extends View {        
        private final List<TextNode> nodes;        
        private final Paint normalPaint, selectedPaint; 
        private TextNode selectedNode = null; 
        private int navBarHeight = 0; 
        
        public SelectionOverlayView(Context context, List<TextNode> nodes) {        
            super(context); this.nodes = nodes;        
            normalPaint = new Paint(); normalPaint.setColor(0x4D0088FF); normalPaint.setStyle(Paint.Style.FILL);        
            selectedPaint = new Paint(); selectedPaint.setColor(0x880055CC); selectedPaint.setStyle(Paint.Style.FILL); selectedPaint.setStrokeWidth(5);
            
            int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                navBarHeight = getResources().getDimensionPixelSize(resourceId);
            }
        }        

        public void clearSelection() {
            this.selectedNode = null; invalidate();
            try {
                WindowManager.LayoutParams overlayParams = (WindowManager.LayoutParams) getLayoutParams();
                overlayParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE; 
                windowManager.updateViewLayout(this, overlayParams);
            } catch (Exception e) {}
        }
        
        @Override        
        protected void onDraw(Canvas canvas) {        
            super.onDraw(canvas);        
            for (TextNode node : nodes) {        
                if (node.rect != null) canvas.drawRect(node.rect, node == selectedNode ? selectedPaint : normalPaint);        
            }        
        }        
        
        @Override        
        public boolean onTouchEvent(MotionEvent event) {        
            if (event.getAction() == MotionEvent.ACTION_DOWN) {        
                int x = (int) event.getX(), y = (int) event.getY();        
                
                if (y > getHeight() - navBarHeight - 50) { 
                    closeAndResetMode(); 
                    return false; 
                }

                TextNode bestMatch = null; int minArea = Integer.MAX_VALUE;
                for (TextNode node : nodes) {        
                    if (node.rect != null && node.rect.contains(x, y)) {        
                        int area = node.rect.width() * node.rect.height();
                        if (area < minArea) { minArea = area; bestMatch = node; }
                    }        
                }        
                if (bestMatch != null) {
                    this.selectedNode = bestMatch; invalidate(); 
                    try {
                        WindowManager.LayoutParams overlayParams = (WindowManager.LayoutParams) getLayoutParams();
                        overlayParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE; 
                        windowManager.updateViewLayout(this, overlayParams);
                    } catch (Exception e) {}
                    showPopupWindow(bestMatch.text); return true;
                }
                closeAndResetMode(); return false;      
            }        
            return super.onTouchEvent(event);        
        }        
    }
}
