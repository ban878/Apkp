package com.yourname.universalcopy; // تأكد من تطابق اسم الحزمة

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateRemoteModel;

import java.util.Locale;

public class DownloadedModelsActivity extends AppCompatActivity {

    private LinearLayout containerModels;
    private RemoteModelManager modelManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloaded_models);

        containerModels = findViewById(R.id.container_models);
        modelManager = RemoteModelManager.getInstance();

        loadDownloadedModels();
    }

    private void loadDownloadedModels() {
        containerModels.removeAllViews(); // تفريغ الشاشة قبل التحميل
        
        // جلب قائمة اللغات المحملة من مكتبة جوجل
        modelManager.getDownloadedModels(TranslateRemoteModel.class)
                .addOnSuccessListener(models -> {
                    if (models.isEmpty()) {
                        Toast.makeText(this, getString(R.string.toast_no_models), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (TranslateRemoteModel model : models) {
                        addModelToUI(model);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.toast_read_models_error), Toast.LENGTH_SHORT).show());
    }

    // دالة لتصميم سطر لكل لغة برمجياً
    private void addModelToUI(TranslateRemoteModel model) {
        String langCode = model.getLanguage();
        
        // جلب اسم اللغة وعرضه بلغة هاتف المستخدم الحالية تلقائياً (عربي أو إنجليزي)
        Locale loc = new Locale(langCode);
        String langName = loc.getDisplayLanguage(Locale.getDefault()); 

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(30, 30, 30, 30);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 20);
        row.setLayoutParams(params);
        row.setBackgroundResource(R.drawable.bg_card); // استخدام تصميم البطاقة
        row.setElevation(4f);

        // نص اسم اللغة
        TextView tvName = new TextView(this);
        tvName.setText(langName + " (" + langCode.toUpperCase() + ")");
        tvName.setTextSize(18f);
        tvName.setTextColor(Color.parseColor("#2C3E50"));
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        // زر الحذف
        ImageButton btnDelete = new ImageButton(this);
        btnDelete.setImageResource(R.drawable.ic_delete);
        btnDelete.setBackgroundColor(Color.TRANSPARENT);
        btnDelete.setColorFilter(Color.parseColor("#E91E63")); // لون أحمر/وردي
        btnDelete.setPadding(20, 20, 20, 20);

        // برمجة زر الحذف
        btnDelete.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.toast_deleting), Toast.LENGTH_SHORT).show();
            modelManager.deleteDownloadedModel(model)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, getString(R.string.toast_model_deleted), Toast.LENGTH_SHORT).show();
                        containerModels.removeView(row); // إزالة السطر من الشاشة فوراً
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.toast_delete_failed), Toast.LENGTH_SHORT).show());
        });

        row.addView(tvName);
        row.addView(btnDelete);
        containerModels.addView(row);
    }
}