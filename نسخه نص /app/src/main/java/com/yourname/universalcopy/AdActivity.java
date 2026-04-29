package com.yourname.universalcopy; // ⚠️ تأكد من مطابقة اسم حزمتك

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// 🔥 استيرادات التهيئة الإجبارية للإصدار 7.0.0
import com.yandex.mobile.ads.common.InitializationListener;
import com.yandex.mobile.ads.common.MobileAds;

import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequestConfiguration;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.rewarded.Reward;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoader;

public class AdActivity extends Activity {

    private RewardedAdLoader mRewardedAdLoader = null;
    private boolean isRewarded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🛡️ 1. الفحص الأمني السريع قبل عرض أو تحميل أي إعلان
        if (!axv.isAppSecure(this)) {
            Toast.makeText(this, "Security Violation Detected!", Toast.LENGTH_SHORT).show();
            finish(); // إغلاق الصفحة فوراً إذا التطبيق معدل
            return;
        }

        // 🔥 2. التهيئة الإجبارية لمكتبة ياندكس 7.x قبل القيام بأي شيء
        MobileAds.initialize(this, new InitializationListener() {
            @Override
            public void onInitializationCompleted() {
                // بعد أن تتصل المكتبة بسيرفرات ياندكس بنجاح، نبدأ معالجة الإعلان
                handleAdLogic();
            }
        });
    }

    // تم فصل المنطق في دالة مستقلة لكي تُستدعى بعد نجاح التهيئة
    private void handleAdLogic() {
        // هل الإعلان محمل مسبقاً وجاهز؟
        if (UniversalCopyService.mReadyYandexRewardedAd != null) {
            showReadyAd(UniversalCopyService.mReadyYandexRewardedAd);
            UniversalCopyService.mReadyYandexRewardedAd = null; // تفريغه بعد العرض
        } else {
            // تحميل إعلان جديد
            Toast.makeText(this, getString(R.string.toast_loading_ad), Toast.LENGTH_SHORT).show();
            mRewardedAdLoader = new RewardedAdLoader(this);
            mRewardedAdLoader.setAdLoadListener(new RewardedAdLoadListener() {
                @Override
                public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                    showReadyAd(rewardedAd);
                }
                @Override
                public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                    // لا يوجد إعلان متوفر من الشركة، نعطيه الرصيد مجاناً
                    Toast.makeText(AdActivity.this, getString(R.string.toast_ad_failed_free_uses), Toast.LENGTH_LONG).show();
                    grantUsesAndClose();
                }
            });
            
            // 🛡️ 3. استخدام كود الإعلان المشفر (فك التشفير وقت التشغيل فقط)
            String decryptedAdId = axv.decryptData(this, "+28VtDvsdybRfq7rTHGKoYl5JUAEOddyXjqXQTQyZCvWL27DSrMDZfeqcSfRtKmQ53YbLl/6wWUeXmImv5IWRsDg1cJAL5Mpy9kfHtZoL9dN5WIWklb+YpWDLf/D1Gyyuk3hG3MbSV0Zgbpon5fRuOUbH2xkbNwqO0AkizkKh9bY8XUcQjP3NjCx4LIumP8G7aRr0a/1OUypsY3Ie/DbJ+Lpy762QPaZ+88ahXl/Xr0Uxz1RUaKWfBi+MBnEAocMLSyxeUx33ICr4BEW6HK2py9ffVw1P5Z7gcBQRfda74xhmz56Fq/JhMe8SbbwVvBnsAT/p/DUfUG1PYFVgBMBQaPKqJVOWtr/eoaXpcVuOyp5nWvxOfQR9ZXDtxVaUCb7/bq35B6Ur0S3jUkoXAtw7Tt2Lleud/cim5wjzRvwAX183Uf8ZJY5JDSzwkFwRWdYbBfiZ7B2hdUarE4gi5qfunFniEKLXbsUJw==");
            
            // في حالة فشل فك التشفير بسبب التلاعب، نمرر كوداً وهمياً لكي لا ينهار التطبيق
            if (decryptedAdId == null || decryptedAdId.startsWith("ERR_")) {
                decryptedAdId = "INVALID_TAMPERED_AD"; 
            }
            
            AdRequestConfiguration adRequestConfiguration = new AdRequestConfiguration.Builder(decryptedAdId).build();
            mRewardedAdLoader.loadAd(adRequestConfiguration);
        }
    }

    private void showReadyAd(RewardedAd rewardedAd) {
        rewardedAd.setAdEventListener(new RewardedAdEventListener() {
            @Override public void onAdShown() {}
            @Override public void onAdFailedToShow(@NonNull AdError adError) { closeAndLoadNext(); }
            @Override public void onAdDismissed() {
                // إذا أكمل الإعلان نعطيه الرصيد
                if (isRewarded) grantUsesAndClose();
                else closeAndLoadNext(); 
            }
            @Override public void onAdClicked() {}
            @Override public void onAdImpression(@Nullable ImpressionData impressionData) {}
            
            // التأكد من إكمال الإعلان
            @Override public void onRewarded(@NonNull Reward reward) {
                isRewarded = true; 
            }
        });
        rewardedAd.show(this);
    }

    private void grantUsesAndClose() {
        // 🛡️ 4. تشفير النقاط وتخزينها باسم مموه في الذاكرة
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        
        // تشفير الرقم 10 لطلاسم متغيرة
        String encryptedPoints = axv.encryptData("10"); 
        
        if (encryptedPoints != null) {
            // حفظ الطلاسم في مفتاح اسمه يشبه ملفات النظام "sys_cfg_v2"
            prefs.edit().putString("sys_cfg_v2", encryptedPoints).apply(); 
        }

        Toast.makeText(this, getString(R.string.toast_reward_granted), Toast.LENGTH_LONG).show();
        closeAndLoadNext();
    }

    private void closeAndLoadNext() {
        if (UniversalCopyService.getInstance() != null) {
            UniversalCopyService.getInstance().preloadNextAd();
        }
        finishAndRemoveTask();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        closeAndLoadNext();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRewardedAdLoader != null) mRewardedAdLoader.setAdLoadListener(null);
    }
}
