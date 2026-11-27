# Rota 1.0 — yayın doğrulama raporu

Paket dev.kolmapper, versionName 1.0, versionCode 10. Minimum Android 10/API 29; hedef ve derleme Android 16/API 36. Release debuggable=false. Veri anahtarları ve .rota biçim sürümü 1 korunur; güncelleme için veriler silinmez.

## Geçen kontroller

- Release APK/AAB derlemesi ve lint. Android 10’dan yeni API çağrıları SDK koşullarıyla korunur. Android 10 açık temada sistem çubuğu simgeleri için eski View bayrakları kullanılır. Android 10/11 sistem yedekleme davranışı da fullBackupContent=false ile tanımlıdır; elle .rota dışa aktarma ayrı çalışır.
- Android 16/API 36 emülatöründe 0.8 üzerine 1.0 imzalı güncelleme kuruldu. 22 Android depolama kontrolü geçti: profil ayrımı, eski ortak düzenin açıkça taşınması, ikili yedek, önceki nesilden kurtarma, bozuk dosyanın reddi, benzer paket adları ve diğer oyunların korunması.
- Aynı API 36’de ana ekran yerleşimi: 1280×800, 7 görünür tıklanabilir kontrol, 5 ikon; kesilmiş kontrol veya ana sayfada kaydırma kabı yok.
- Saf Java girdi regresyonları: 10.000 sürekli temas, 10.000 beş tuş dizisi, 90.000 analog devamı, 100.000 pointer kimliği dizisi, 288 panel yerleşimi ve oturum/kilit koşulları.
- 208 ikili yedek kontrolü: tipler, deterministik çıktı, kesilme, header/sürüm/hash bozulması, sıkıştırılmış verinin bozulması, boyut ve tip sınırları.
- APK imzası doğrulandı; aynı release sertifikası kullanılır. İmzalı AAB ve bundletool validate geçti. Uygulama yerel .so kütüphanesi içermez; native 16 KB sayfa hizalaması gerektiren bir dosya yoktur.

## Android 10 çalışma kontrolü

API 29/x86 AOSP emülatöründe imzalı 1.0 APK kuruldu. Aynı 22 depolama kontrolü ve ana ekran yerleşimi (7 tıklanabilir kontrol, 5 ikon, 1280×800) geçti.

Gerçek Linux uinput HID olaylarıyla şu kontroller geçti: erişilebilirlik servisinin bağlanması; kayıtlı oyun açıldığında ikona dokunmadan kontrolün etkinleşmesi; overlay’in bağlanması; sağ yön basılı tutulurken A makrosunun eşzamanlı tekrarlaması; bırakınca tüm temasların kapanması; sağ yönün makro sırasında tek DOWN/UP ile kesintisiz kalması; Home’da oturum korunurken overlay’in gizlenmesi; oyuna dönünce kontrolün otomatik gelmesi; ekran kapatılınca oturumun ve overlay’in kapanması.

İlk deneme emülatörün System UI ANR penceresiyle kesildi; Rota’da oturum henüz başlamamıştı. Emülatörün sorunlu radyo servisi durdurulup sistem penceresi kapatıldıktan sonra tekrarlandı. Test aracı Android 10’un sayısal `ty=2032` pencere gösterimini tanıyacak şekilde düzeltildi; uygulama kodu bu yüzden değiştirilmedi. Son sonuç `rota10-api29-input-results.log`, temas kaydı `rota10-api29-touch-trace.txt` içindedir.

## Kapsam ve önceki girdi doğrulaması

1.0’da hareket enjeksiyon motoru, çoklu temas, makro zamanlayıcısı, cihaz çıkarma/takma ve oyun penceresi takibi değiştirilmedi. 0.8’in gerçek uinput olaylarıyla geçen yeniden bağlantı, sistem hareketleri, oyun dışı gizlenme, oyuna dönüş, kesilen düzenleme, gerçek DocumentsUI yedek akışı ve kilit testleri docs/history/TEST-REPORT-0.8.md içinde arşivlidir. 1.0’a ait yeni çalıştırılan sonuçlar bu raporda ayrı listelenir.

Ham analog çubuk yakalama Android 14+ gerektirir. Android 10–13’te tuş olayları ve dijital yön tuşlarıyla eşleme/makro vardır; bu sürümlere ham analog desteği vaat edilmez. Arayüz ve mağaza metni bu sınırı belirtir.

Gerçek Honor/8BitDo Bluetooth/USB protokolü, bütün Android 11–15 üretici sistemleri ve Google Play Protect sonucu bu emülatör testlerinden çıkarılamaz. Android’in dispatchGesture API’si daha önce gönderilmiş bir dokunmayı uygulama geçişinde atomik geri alamaz; 0.8 raporundaki bir geçişte gözlenen tek DOWN/UP sınırı sürer. Sıfır gecikme veya yüzde yüz hatasızlık iddiası yoktur.

## Google Play durumu

Teknik yayın arşivi, mağaza metinleri, görseller, erişilebilirlik formu taslağı, sürüm notları ve gizlilik HTML’i hazırlanır; Google Play’e gönderim yapılmaz. Gerçek destek e-postası, herkese açık HTTPS gizlilik adresi, geliştirici hesabı beyanları/inceleme videosu ve Google erişilebilirlik onayı yayın sahibinin Console adımlarıdır. Bunlar olmadan tam yayın onayı var denmez. Resmi kaynaklar play/PLAY-READINESS.md içindedir.

## Teslim APK kimliği

SHA-256: `2d3463ac31a700629a94f5ae6a322a68abfcccf40a89a02ac9a75c9a7d69de9d`. Bu, Android 10 ve Android 16 kontrollerinde kullanılan imzalı 1.0 paketidir.
