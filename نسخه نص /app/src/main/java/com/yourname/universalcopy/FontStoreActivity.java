package com.yourname.universalcopy; // تأكد من مطابقة اسم حزمتك

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FontStoreActivity extends Activity {

    private TextView previewText;
    private LinearLayout fontsListContainer;
    private EditText searchInput;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    
    private boolean showDownloadedOnly = false;
    private boolean isDarkMode = false;

    private static class FontItem {
        String name; String url;
        FontItem(String n, String u) { name = n; url = u; }
    }

    private final List<FontItem> MASTER_FONTS = new ArrayList<>();
    private List<FontItem> currentDisplayList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // التحقق من الوضع الداكن
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        
        initMasterList();
        setupUI();
        loadRandomFonts();
    }

    private void initMasterList() {
        try {
            InputStream is = getAssets().open("fonts.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                MASTER_FONTS.add(new FontItem(obj.getString("name"), obj.getString("url")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            mainHandler.post(() -> Toast.makeText(this, getString(R.string.toast_db_error), Toast.LENGTH_LONG).show());
            MASTER_FONTS.add(new FontItem("Cairo", "https://raw.githubusercontent.com/google/fonts/main/ofl/cairo/Cairo-Regular.ttf"));
        }
    }

    private void loadRandomFonts() {
        currentDisplayList.clear();
        List<FontItem> shuffled = new ArrayList<>(MASTER_FONTS);
        Collections.shuffle(shuffled);
        
        int limit = Math.min(30, shuffled.size());
        for (int i = 0; i < limit; i++) {
            currentDisplayList.add(shuffled.get(i));
        }
        updateUIList();
    }

    private void filterFonts(String query) {
        currentDisplayList.clear();
        if (query.isEmpty()) {
            loadRandomFonts();
            return;
        }
        for (FontItem font : MASTER_FONTS) {
            if (font.name.toLowerCase().contains(query.toLowerCase())) {
                currentDisplayList.add(font);
            }
        }
        updateUIList();
    }

    private void setupUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(isDarkMode ? Color.parseColor("#121212") : Color.parseColor("#F5F7FA"));
        root.setPadding(dpToPx(16), dpToPx(24), dpToPx(16), dpToPx(16));

        // Header
        TextView headerTitle = new TextView(this);
        headerTitle.setText(getString(R.string.header_title));
        headerTitle.setTextSize(28f);
        headerTitle.setTypeface(null, Typeface.BOLD);
        headerTitle.setTextColor(isDarkMode ? Color.parseColor("#FFFFFF") : Color.parseColor("#1A1A2E"));
        headerTitle.setGravity(Gravity.CENTER);
        headerTitle.setPadding(0, 0, 0, dpToPx(8));
        root.addView(headerTitle);

        // Search Card
        CardView searchCard = new CardView(this);
        searchCard.setCardBackgroundColor(isDarkMode ? Color.parseColor("#2C2C2C") : Color.parseColor("#FFFFFF"));
        searchCard.setRadius(dpToPx(12));
        searchCard.setCardElevation(dpToPx(2));
        searchCard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        
        LinearLayout searchLayout = new LinearLayout(this);
        searchLayout.setOrientation(LinearLayout.HORIZONTAL);
        searchLayout.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        searchLayout.setGravity(Gravity.CENTER_VERTICAL);
        
        searchInput = new EditText(this);
        searchInput.setHint(getString(R.string.search_hint));
        searchInput.setHintTextColor(isDarkMode ? Color.parseColor("#808080") : Color.parseColor("#9E9E9E"));
        searchInput.setTextColor(isDarkMode ? Color.parseColor("#FFFFFF") : Color.parseColor("#212121"));
        searchInput.setBackground(null);
        searchInput.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchInput.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterFonts(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
        
        searchLayout.addView(searchInput);
        searchCard.addView(searchLayout);
        root.addView(searchCard);

        // Buttons Row
        LinearLayout buttonsRow = new LinearLayout(this);
        buttonsRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonsRow.setPadding(0, dpToPx(16), 0, dpToPx(16));
        buttonsRow.setWeightSum(2);
        
        Button refreshBtn = createStyledButton(getString(R.string.btn_random), "#4A90E2");
        refreshBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        refreshBtn.setOnClickListener(v -> loadRandomFonts());
        
        Button toggleDownloadedBtn = createStyledButton(getString(R.string.btn_my_library), "#9B59B6");
        toggleDownloadedBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        toggleDownloadedBtn.setOnClickListener(v -> {
            showDownloadedOnly = !showDownloadedOnly;
            toggleDownloadedBtn.setText(showDownloadedOnly ? getString(R.string.btn_store) : getString(R.string.btn_my_library));
            GradientDrawable drawable = (GradientDrawable) toggleDownloadedBtn.getBackground();
            drawable.setColor(Color.parseColor(showDownloadedOnly ? "#E67E22" : "#9B59B6"));
            
            if (showDownloadedOnly) {
                currentDisplayList.clear();
                currentDisplayList.addAll(MASTER_FONTS);
            } else {
                loadRandomFonts();
            }
            updateUIList();
        });
        
        buttonsRow.addView(refreshBtn);
        buttonsRow.addView(toggleDownloadedBtn);
        root.addView(buttonsRow);

        // Preview Card
        CardView previewCard = new CardView(this);
        previewCard.setCardBackgroundColor(isDarkMode ? Color.parseColor("#1E1E1E") : Color.parseColor("#FFFFFF"));
        previewCard.setRadius(dpToPx(16));
        previewCard.setCardElevation(dpToPx(4));
        previewCard.setPreventCornerOverlap(false);
        previewCard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        
        LinearLayout previewContainer = new LinearLayout(this);
        previewContainer.setOrientation(LinearLayout.VERTICAL);
        previewContainer.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        previewContainer.setGravity(Gravity.CENTER);
        
        TextView previewLabel = new TextView(this);
        previewLabel.setText(getString(R.string.preview_label));
        previewLabel.setTextSize(12f);
        previewLabel.setTextColor(isDarkMode ? Color.parseColor("#B0B0B0") : Color.parseColor("#757575"));
        previewLabel.setPadding(0, 0, 0, dpToPx(8));
        previewContainer.addView(previewLabel);
        
        previewText = new TextView(this);
        previewText.setText(getString(R.string.preview_text));
        previewText.setTextSize(24f);
        previewText.setTextColor(isDarkMode ? Color.parseColor("#FFFFFF") : Color.parseColor("#2C3E50"));
        previewText.setGravity(Gravity.CENTER);
        previewText.setLineSpacing(dpToPx(4), 1.2f);
        
        String activeFontPath = getActiveFontPath();
        if (!activeFontPath.isEmpty()) {
            try { previewText.setTypeface(Typeface.createFromFile(activeFontPath)); } catch (Exception e) {}
        }
        previewContainer.addView(previewText);
        previewCard.addView(previewContainer);
        root.addView(previewCard);

        // Space
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(16)));
        root.addView(spacer);

        // Fonts List Header
        TextView listHeader = new TextView(this);
        listHeader.setText(getString(R.string.list_header));
        listHeader.setTextSize(16f);
        listHeader.setTypeface(null, Typeface.BOLD);
        listHeader.setTextColor(isDarkMode ? Color.parseColor("#FFFFFF") : Color.parseColor("#1A1A2E"));
        listHeader.setPadding(dpToPx(4), dpToPx(8), dpToPx(4), dpToPx(8));
        root.addView(listHeader);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackground(null);
        scrollView.setPadding(0, 0, 0, dpToPx(8));
        
        fontsListContainer = new LinearLayout(this);
        fontsListContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(fontsListContainer);
        root.addView(scrollView);

        setContentView(root);
    }

    private Button createStyledButton(String text, String colorHex) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setTypeface(null, Typeface.BOLD);
        button.setTextSize(14f);
        button.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dpToPx(25));
        drawable.setColor(Color.parseColor(colorHex));
        button.setBackground(drawable);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        button.setLayoutParams(params);
        
        return button;
    }

    private void updateUIList() {
        fontsListContainer.removeAllViews();
        List<String> downloadedFonts = getDownloadedFontNames();
        String activeFontName = getActiveFontName();
        boolean hasItems = false;
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);

        for (FontItem fontData : currentDisplayList) {
            String fontName = fontData.name;
            String fontUrl = fontData.url;
            boolean isDownloaded = downloadedFonts.contains(fontName);
            boolean isActive = fontName.equals(activeFontName);

            if (showDownloadedOnly && !isDownloaded) continue;
            hasItems = true;

            CardView card = new CardView(this);
            card.setRadius(dpToPx(12));
            card.setCardElevation(dpToPx(3));
            card.setPreventCornerOverlap(false);
            card.setUseCompatPadding(true);
            
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, dpToPx(12));
            card.setLayoutParams(cardParams);
            
            if (isActive) {
                card.setCardBackgroundColor(isDarkMode ? Color.parseColor("#1B3B1A") : Color.parseColor("#E8F5E9"));
            } else {
                card.setCardBackgroundColor(isDarkMode ? Color.parseColor("#1E1E1E") : Color.parseColor("#FFFFFF"));
            }

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.HORIZONTAL);
            content.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
            content.setGravity(Gravity.CENTER_VERTICAL);

            TextView nameText = new TextView(this);
            nameText.setText(fontName);
            nameText.setTextColor(isActive ? (isDarkMode ? Color.parseColor("#81C784") : Color.parseColor("#2E7D32")) : (isDarkMode ? Color.parseColor("#E0E0E0") : Color.parseColor("#212121")));
            nameText.setTextSize(16f);
            nameText.setTypeface(null, isActive ? Typeface.BOLD : Typeface.NORMAL);
            nameText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            
            if (isActive) {
                TextView activeBadge = new TextView(this);
                activeBadge.setText(getString(R.string.badge_active));
                activeBadge.setTextSize(11f);
                activeBadge.setTextColor(isDarkMode ? Color.parseColor("#81C784") : Color.parseColor("#2E7D32"));
                GradientDrawable badgeBg = new GradientDrawable();
                badgeBg.setShape(GradientDrawable.RECTANGLE);
                badgeBg.setCornerRadius(dpToPx(12));
                badgeBg.setColor(isDarkMode ? Color.parseColor("#2E7D32") : Color.parseColor("#C8E6C9"));
                activeBadge.setBackground(badgeBg);
                activeBadge.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
                activeBadge.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                ((LinearLayout.LayoutParams) activeBadge.getLayoutParams()).setMargins(dpToPx(8), 0, 0, 0);
                content.addView(activeBadge);
            }
            
            content.addView(nameText);

            LinearLayout buttonsContainer = new LinearLayout(this);
            buttonsContainer.setOrientation(LinearLayout.HORIZONTAL);

            if (isDownloaded) {
                if (!isActive) {
                    Button selectBtn = createSmallButton(getString(R.string.btn_select), "#4CAF50");
                    selectBtn.setOnClickListener(v -> {
                        setActiveFont(fontName, getFontFile(fontName).getAbsolutePath());
                        Toast.makeText(this, getString(R.string.toast_font_selected), Toast.LENGTH_SHORT).show();
                        updateUIList();
                        try { previewText.setTypeface(Typeface.createFromFile(getFontFile(fontName))); } catch (Exception e) {}
                    });
                    buttonsContainer.addView(selectBtn);
                }

                Button deleteBtn = createSmallButton(getString(R.string.btn_delete), "#F44336");
                deleteBtn.setOnClickListener(v -> deleteFont(fontName));
                buttonsContainer.addView(deleteBtn);
            } else {
                Button previewBtn = createSmallButton(getString(R.string.btn_preview), "#FF9800");
                previewBtn.setOnClickListener(v -> previewFontLive(fontName, fontUrl, false));
                buttonsContainer.addView(previewBtn);
                
                Button downloadBtn = createSmallButton(getString(R.string.btn_download), "#2196F3");
                downloadBtn.setOnClickListener(v -> downloadFont(fontName, fontUrl, downloadBtn));
                buttonsContainer.addView(downloadBtn);
            }
            
            content.addView(buttonsContainer);
            card.addView(content);
            fontsListContainer.addView(card);
            card.startAnimation(fadeIn);
        }

        if (!hasItems) {
            CardView emptyCard = new CardView(this);
            emptyCard.setRadius(dpToPx(12));
            emptyCard.setCardBackgroundColor(isDarkMode ? Color.parseColor("#1E1E1E") : Color.parseColor("#FFF3E0"));
            emptyCard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            
            TextView emptyText = new TextView(this);
            emptyText.setText(showDownloadedOnly ? getString(R.string.empty_library) : getString(R.string.empty_search));
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setTextColor(isDarkMode ? Color.parseColor("#B0B0B0") : Color.parseColor("#E65100"));
            emptyText.setTextSize(14f);
            emptyText.setPadding(dpToPx(32), dpToPx(48), dpToPx(32), dpToPx(48));
            emptyCard.addView(emptyText);
            fontsListContainer.addView(emptyCard);
        }
    }

    private Button createSmallButton(String text, String colorHex) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12f);
        button.setAllCaps(false);
        button.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dpToPx(20));
        drawable.setColor(Color.parseColor(colorHex));
        button.setBackground(drawable);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        button.setLayoutParams(params);
        
        return button;
    }

    private void previewFontLive(String name, String urlStr, boolean isDownloaded) {
        if (isDownloaded) {
            try { previewText.setTypeface(Typeface.createFromFile(getFontFile(name))); } catch (Exception e) {}
            Toast.makeText(this, getString(R.string.toast_preview_ready) + " " + name, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, getString(R.string.toast_previewing), Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            try {
                File cacheFile = new File(getCacheDir(), "temp_preview_" + System.currentTimeMillis() + ".ttf");
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(15000);
                conn.connect();
                InputStream is = conn.getInputStream();
                FileOutputStream fos = new FileOutputStream(cacheFile);
                byte[] buffer = new byte[4096]; int len;
                while ((len = is.read(buffer)) != -1) fos.write(buffer, 0, len);
                fos.close(); is.close();

                mainHandler.post(() -> {
                    try { 
                        previewText.setTypeface(Typeface.createFromFile(cacheFile));
                        Toast.makeText(this, getString(R.string.toast_preview_ready) + " " + name, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, getString(R.string.toast_preview_failed), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this, getString(R.string.toast_preview_failed), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void downloadFont(String name, String urlStr, Button btn) {
        if (getDownloadedFontNames().size() >= 15) {
            Toast.makeText(this, getString(R.string.toast_limit_reached), Toast.LENGTH_LONG).show();
            return;
        }

        btn.setText(getString(R.string.btn_downloading));
        btn.setEnabled(false);

        executor.execute(() -> {
            try {
                File fontFile = getFontFile(name);
                fontFile.getParentFile().mkdirs();
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.connect();
                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) throw new Exception();

                InputStream is = conn.getInputStream();
                FileOutputStream fos = new FileOutputStream(fontFile);
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) fos.write(buffer, 0, len);
                fos.close();
                is.close();

                saveFontToPrefs(name, fontFile.getAbsolutePath());
                mainHandler.post(() -> {
                    Toast.makeText(this, getString(R.string.toast_download_success) + " " + name, Toast.LENGTH_SHORT).show();
                    updateUIList();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    btn.setText(getString(R.string.btn_download));
                    btn.setEnabled(true);
                    Toast.makeText(this, getString(R.string.toast_download_failed), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void deleteFont(String name) {
        File fontFile = getFontFile(name);
        if (fontFile.exists()) fontFile.delete();
        removeFontFromPrefs(name);
        
        if (name.equals(getActiveFontName())) {
            setActiveFont("", "");
            previewText.setTypeface(Typeface.DEFAULT);
        }
        Toast.makeText(this, getString(R.string.toast_delete_success) + " " + name, Toast.LENGTH_SHORT).show();
        updateUIList();
    }

    private File getFontFile(String name) {
        String safeName = name.replaceAll("[^a-zA-Z0-9\\u0600-\\u06FF]", "").replace(" ", "_");
        return new File(getFilesDir() + "/fonts/", safeName + ".ttf");
    }

    private void setActiveFont(String name, String path) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().putString("active_font_name", name).putString("active_font_path", path).apply();
    }
    
    private String getActiveFontName() { 
        return getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("active_font_name", ""); 
    }
    
    private String getActiveFontPath() { 
        return getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("active_font_path", ""); 
    }

    private List<String> getDownloadedFontNames() {
        List<String> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("saved_fonts", "[]"));
            for (int i = 0; i < array.length(); i++) list.add(array.getJSONObject(i).getString("name"));
        } catch (Exception e) {}
        return list;
    }

    private void saveFontToPrefs(String name, String path) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        try {
            JSONArray array = new JSONArray(prefs.getString("saved_fonts", "[]"));
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("path", path);
            array.put(obj);
            prefs.edit().putString("saved_fonts", array.toString()).apply();
        } catch (Exception e) {}
    }

    private void removeFontFromPrefs(String name) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        try {
            JSONArray array = new JSONArray(prefs.getString("saved_fonts", "[]"));
            JSONArray newArray = new JSONArray();
            for (int i = 0; i < array.length(); i++) {
                if (!array.getJSONObject(i).getString("name").equals(name)) newArray.put(array.getJSONObject(i));
            }
            prefs.edit().putString("saved_fonts", newArray.toString()).apply();
        } catch (Exception e) {}
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}