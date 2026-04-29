package com.yourname.universalcopy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;

// مكتبات Google ML Kit
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateRemoteModel;

// مكتبة Yandex AppMetrica الحديثة
import io.appmetrica.analytics.AppMetrica;
import io.appmetrica.analytics.AppMetricaConfig;

public class MainActivity extends AppCompatActivity {

    private Button btnEnableAccessibility;
    private ImageButton btnDownloadSmall;
    private LinearLayout layoutLanguageSelector;
    private Spinner spinnerLanguages;

    private String[] languageNames;
    private String[] languageCodes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ========================================================
        // 📊 تفعيل تحليلات AppMetrica الحديثة (مشفرة وآمنة 100%)
        // ========================================================
        try {
            // الشفرة الخاصة بك التي تم توليدها وتشفيرها بنجاح
            String encryptedMetricaKey = "mCa0sWFjVh17GGOY//wGvoBLbYLGApNwM3SCBa0MTmsBYjusaF+gahdkEhup8LWq/vfed29BQqgekgvIdU5hiXc+eHKd/fyn9FnA6iCN5R3sAWumFb0/p4va5k1S8mAdlDMLr7BkneolAX756qaSpcjvgc9KB1h5zWSQAC6qzKxiqG0xx/xFzcwhi2O39iyr+ILH5yECPMeX5HW+519re8TiyfWuQl0G3D3p1Kz93Ta5kXSGeVBNcEGDDb1Sc3+1yoXJKI8JhWRo/tkyx2dCKMJiyXbq58qcVCttSv/mInQyw16iVaye12p2Vv84z1CV9tvFZ8dP72lwwvQGxOef8b9czowRCOlrD6Pypqc0ENPthuc3lMZlvVnRinsqMwN3oravdqzE1AFzLknQHte2yNDZO6ln/x835CjchzQtbUQH3MEujpHmNMdaQq45955GN13MUgDX2Uxb3eO2KFXN0EKbe855AoXWmTL5cNJHR0NK2n/h7CO0ibxGvZSnPU1+6I17r/pgS85bPR1i6hcpEmmXxrDNMvXgyFDNU9vyisI26Kd0FO4cIm71ZgCvRIgJALvDdencIcmuKqmUALNfC2U="; 
            
            String realApiKey = axv.decryptData(this, encryptedMetricaKey);
            if (realApiKey != null && !realApiKey.startsWith("ERR_TAMPERED")) {
                // تفعيل التحليلات بمفتاحك الحقيقي في الذاكرة الحية فقط
                AppMetricaConfig config = AppMetricaConfig.newConfigBuilder(realApiKey).build();
                AppMetrica.activate(this, config);
            }
        } catch (Exception e) {
            // صمت تام في حال التلاعب (لكي لا ينتبه المخترق)
        }
        // ========================================================

        initViews();
        setupLanguageSpinner();
        setupClickListeners();
    }

    private void initViews() {
        btnEnableAccessibility = findViewById(R.id.btn_enable_accessibility);
        btnDownloadSmall = findViewById(R.id.btn_download_model_small);
        layoutLanguageSelector = findViewById(R.id.layout_language_selector);
        spinnerLanguages = findViewById(R.id.spinner_languages);

        Button btnOpenStore = findViewById(R.id.btn_open_font_store);
        if (btnOpenStore != null) {
            btnOpenStore.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, FontStoreActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupLanguageSpinner() {
        languageNames = getResources().getStringArray(R.array.language_names);
        languageCodes = getResources().getStringArray(R.array.language_codes);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, languageNames);
        spinnerLanguages.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String savedLang = prefs.getString("target_lang", "ar");
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(savedLang)) {
                spinnerLanguages.setSelection(i);
                break;
            }
        }

        spinnerLanguages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putString("target_lang", languageCodes[position]).apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupClickListeners() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        btnDownloadSmall.setOnClickListener(v -> {
            String targetLang = prefs.getString("target_lang", "ar");
            Toast.makeText(this, getString(R.string.toast_downloading), Toast.LENGTH_LONG).show();

            RemoteModelManager modelManager = RemoteModelManager.getInstance();
            TranslateRemoteModel model = new TranslateRemoteModel.Builder(targetLang).build();
            DownloadConditions conditions = new DownloadConditions.Builder().build();

            modelManager.download(model, conditions)
                .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, getString(R.string.toast_download_success), Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, getString(R.string.toast_download_fail), Toast.LENGTH_SHORT).show());
        });

        btnDownloadSmall.setOnLongClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DownloadedModelsActivity.class);
            startActivity(intent);
            return true;
        });

        btnEnableAccessibility.setOnClickListener(v -> {
            if (!isAccessibilityServiceEnabled(MainActivity.this, UniversalCopyService.class)) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                Toast.makeText(MainActivity.this, getString(R.string.toast_enable_search), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAccessibilityStatus();
    }

    private void checkAccessibilityStatus() {
        if (isAccessibilityServiceEnabled(this, UniversalCopyService.class)) {
            btnEnableAccessibility.setText(getString(R.string.btn_service_running));
            btnEnableAccessibility.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.btn_active)));
            btnEnableAccessibility.setEnabled(false); 
            layoutLanguageSelector.setVisibility(View.VISIBLE); 
            
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            if (!prefs.getBoolean("has_seen_tutorial", false)) {
                showTutorialDialog(prefs);
            }
        } else {
            btnEnableAccessibility.setText(getString(R.string.btn_enable_now));
            btnEnableAccessibility.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.btn_inactive)));
            btnEnableAccessibility.setEnabled(true);
            layoutLanguageSelector.setVisibility(View.GONE); 
        }
    }

    private void showTutorialDialog(SharedPreferences prefs) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_how_to_use))
                .setMessage(getString(R.string.msg_how_to_use_tile))
                .setPositiveButton(getString(R.string.btn_got_it), (dialog, which) -> {
                    prefs.edit().putBoolean("has_seen_tutorial", true).apply();
                })
                .setCancelable(false)
                .show();
    }

    public boolean isAccessibilityServiceEnabled(Context context, Class<?> accessibilityService) {
        String expectedComponentName = context.getPackageName() + "/" + accessibilityService.getName();
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {}
        
        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
                colonSplitter.setString(settingValue);
                while (colonSplitter.hasNext()) {
                    if (colonSplitter.next().equalsIgnoreCase(expectedComponentName)) return true;
                }
            }
        }
        return false;
    }
}
