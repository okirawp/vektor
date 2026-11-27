# Rota 1.0.1 — düzeltme doğrulama raporu

Paket dev.kolmapper, versionName 1.0.1, versionCode 11. Android 10/API 29 ve üzeri; hedef/derleme API 36. Android 11, 12, 13, 14 ve 15 de bu minimum sürüm kapsamındadır. Ham analog yakalama Android 14+ gerektirir.

## Değişiklikler

Tuş ayarları diyaloğu veya klavye oyun üzerinde odaklandığında düzenleme korunur. Başka uygulama, VPN ve sistem izin penceresi ise giriş/overlay kapsamı dışındadır; oyun oturumu bitirilmez. Oyuna dönülünce kontrol otomatik etkinleşir.

Geçici erişilebilirlik servis bağlantısında kullanıcı oturum izni aynı süreç belleğinde korunur. Pencere kimliklerinin paket eşlemesi sınırlı bir önbellekte tutulur; kimlik yenilenince yalnız meta veriyle onarılır. Her ekran içerik olayında pencere listesi sorgulanmaz. Ekran kilidi ve açık durdurma bu izni kaldırır. Süreç ölümü/force-stop sonrası diskten oturum başlatılmaz. Bu sınır kalıcı oyun atamalarını silmez.

Girdi motoru, çoklu temas, makro zamanlaması ve profil/yedek veri biçimi değiştirilmedi. Güncelleme mevcut uygulamanın üzerine kurulmalıdır; veri silme gerekmez. Yeni üretim izni veya internet erişimi eklenmedi. VPN test uygulaması üretim paketinin parçası değildir.

## Bu pakette geçen kontroller

- Release APK/AAB derlemesi ve lint.
- Saf Java girdi, analog, çoklu temas, yerleşim, oturum ve profil regresyonları; 208 ikili yedek kontrolü.
- 9 yeni odak senaryosu: tuş ayarı, klavye, VPN, farklı oyun ve bilinmeyen pencere.
- API 36 emülatöründe imzalı güncelleme, 22 Android profil depolama/yedek kontrolü ve A/B test profilleri.
- APK imzası, önceki sürümle sertifika devamlılığı ve bundletool AAB doğrulaması.

API 36 üzerinde son imzalı APK’de: kayıtlı oyunun otomatik etkinleşmesi; atanmış tuş diyaloğunun açık kalması; basılı tutma/makro değiştirme, kaydetme ve yeniden açma; VPN uygulamasında giriş/ikonun gizlenip oturumun korunması; Android VPN onayıyla gerçek test VPN hizmetinin çalışması; Rota ana ekranına dönmeden oyunda kontrolün geri gelmesi; geçici erişilebilirlik bağlantısından otomatik devam geçti. VPN ve yeniden bağlantıdan sonra iki ek mod değişimi/kaydı da düzenlemeyi kapatmadı. Tüm oyunların bindings değerleri başlangıç kaydıyla birebir karşılaştırıldı ve değişmedi.

Test VPN’i yalnız 192.0.2.0/24 dokümantasyon ağına rota tanımlar; ticari bir VPN sunucusu veya üretici VPN uygulaması test edilmiş sayılmaz. Test APK’sı teslim paketine dahil değildir.

İlk denemelerde emülatör Quickstep ANR, ekran uykusu ve Android tam ekran bilgilendirmesiyle kesildi; bir erken yeniden açmada da düzenleyici kapanması gözlendi. Son APK’de sınırlı pencere meta verisi tanılamasıyla tekrarlandı ve yukarıdaki mod çevrimleri geçti. Test aracında atomik SharedPreferences XML yenilemesi sırasında oluşan geçici dosya yokluğuna yeniden deneme eklendi. UiAutomator dump komutu başarısız dönmesine rağmen Android servis bağlantısı gerçekten kesilip geri bağlandı; geri dönüş ve reason=reconnected ayrıca doğrulandı. Hiçbiri atama silinmesi veya uygulama çökmesi olarak sayılmadı.

Yeni pencere/VPN cihaz akışı sonuçları ayrı günlükte tutulur; yalnız PASS satırları tamamlanmış kontrol sayılır. Önceki 1.0 sürümünün Android 10 gerçek uinput ve Android 16 kontrolleri docs/history/TEST-REPORT-1.0.md içindedir; bu sürümde yeniden çalıştırılmış gibi sunulmaz.

Servis bileşeni geçici olarak yokken aynı uygulama süreci içinde ekran kilitleme/açma testi de geçti: oturum izni kaldırıldı, servis yeniden bağlanınca oturum ve overlay başlamadı.

## Sınırlar ve yayın hazırlığı

Gerçek Honor/8BitDo Bluetooth/USB bağlantısı ve tüm üreticilerin Android 10–16 sistemleri bu emülatörle doğrulanamaz. Google Play Protect sonucu veya Google inceleme onayı garanti edilmez. Tam süreç ölümü sonrası yeni kullanıcı başlatması gerekir; atamalar cihazda kalır. Emülatör donanım hızlandırması olmadan çalıştığından sistem arayüzü gecikmeleri uygulama performans ölçümü sayılmaz.

Play hazırlık arşivi yayın değildir. Gerçek destek e-postası, herkese açık HTTPS gizlilik adresi, inceleme videosu ve Console beyanları yayın sahibinin tamamlayacağı adımlardır. Kaynaklar play/PLAY-READINESS.md içindedir.

## Teslim APK kimliği

SHA-256: `581a0f1457a82de96a2249ca04fc3fb448153b3f6a7c58764be62338caab2241`.
