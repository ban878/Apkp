# ==========================================
# ملف التشفير المخصص (تشفير 3 ملفات فقط)
# ==========================================

# 1. إيقاف العمليات الثقيلة (التصغير والتحسين) لتوفير الرام
-dontshrink
-dontoptimize

# 2. إيقاف رسائل التحذير التي توقف الـ Terminal
-dontwarn **

# 3. حماية المكتبات العملاقة بالكامل من أي تشفير (لتقليل الضغط)
-keep class androidx.** { *; }
-keep class com.google.** { *; }
-keep class com.yandex.** { *; }

# 4. 🔥 تطبيق فكرتك: حماية كل شيء في التطبيق، واستثناء 3 ملفات فقط ليتم تشفيرها 🔥
-keep class !com.yourname.universalcopy.axv, !com.yourname.universalcopy.UniversalCopyService, !com.yourname.universalcopy.AdActivity, ** { *; }

# 5. (مهم جداً) الاحتفاظ "بالاسم الخارجي" فقط للخدمة والنشاط لكي يتعرف عليهما الأندرويد 
# ملاحظة: الأكواد والطلاسم والدوال الموجودة "داخل" هذه الملفات سيتم تشفيرها بالكامل!
-keepnames class com.yourname.universalcopy.UniversalCopyService
-keepnames class com.yourname.universalcopy.AdActivity
