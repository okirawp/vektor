# Rota 1.0.2 doğrulama

Android 10+; versionCode 12, target SDK 36. Mevcut paket kimliği, imza geçmişi ve profil/yedek biçimi korunur. Uygulama verileri silinmez.

Düzeltmeler: Odaklı kendi ayar penceresinin bazı üreticilerde alttaki oyun penceresini listeden gizlemesi desteklenir. Kendi diyalog geçişindeki en fazla 750 ms boş odak aralığı düzenlemeyi kapatmaz. Başka uygulamanın pencere olayı kapsamı hemen kaldırır. Overlay olayları oyun pencere kimliğinin paket eşlemesini ezmez. Ekran ölçüleri her ihtiyaçta güncel okunur; ana ekranın eski yön ölçüsü oyuna dönünce atamaları gizlemez. Görünür kontrol silme düğmesi eklendi. Yeni fiziksel tuşlar Basılı tut olarak eklenir; mevcut makrolar değiştirilmez.

Release APK/AAB derlemesi ve lint geçti. Girdi, analog, çoklu temas, yerleşim, oturum ve profil Java regresyonları; 11 odak senaryosu ve 208 ikili yedek kontrolü geçti. Önceki sürümlerin cihaz testleri docs/history altında ayrı tutulur; bu sürümde tekrar edilmiş sayılmaz.

Gerçek Honor/8BitDo donanımı bu ortamda yoktur. Google Play veya Play Protect onayı garanti edilmez. Yayın için önceki hazırlık belgelerindeki destek adresi, HTTPS gizlilik sayfası ve Console beyanları gereklidir. Yayımlama yapılmamıştır.

Yeni emülatör UI denemesinde erişilebilirlik bağlantısı doğrulandı; emülatör uygulama yüzeyi siyah kaldığı için bu sürümün uçtan uca tuş silme/oyuna dönüş UI akışı tamamlanmış sayılmadı. Honor üzerinde test yapılmadı.

APK SHA-256: 66a3ab1a67ab217cd0c91be8805e76c6b7e576593d4ab5b5558e040a8a9f8107
