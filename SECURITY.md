# Güvenlik ve Play Protect değerlendirmesi

## Ekrandaki engelin sınıfı

Kullanıcının ekranındaki “Bu uygulama hassas verilere erişim izni isteyebilir. Bu da kimlik
hırsızlığı veya mali sahtekarlık riskini artırabilir” mesajı, Google'ın “App blocked to
protect your device” açıklamasıyla eşleşir. Google, bazı bölgelerde internetten yüklenen
ve erişilebilirlik gibi hassas yetkileri kullanan uygulamaları bu nedenle engelleyebildiğini belirtir.

Bir uygulamanın görünümünü veya imzasını değiştirmek bu değerlendirmeyi kaldırdığının kanıtı değildir.
Bu sürüm için Google tarafından verilmiş onay veya Play Protect temiz raporu bulunmamaktadır.
Bu engel çözüldü iddiasında bulunulmaz. Uygulama Google'ın denetimini veya Android'in kısıtlanmış
ayarlarını devre dışı bırakmaya çalışmaz.

Kaynak: https://developers.google.com/android/play-protect/warning-dev-guidance
Resmî değerlendirmeye itiraz yolu aynı belgededir. Bu çalışma kapsamında Google'a başvuru gönderilmedi.

## Bu APK'daki önlemler

- Android 16 / SDK 36 hedefi; hata ayıklama kapalı yayın derlemesi.
- Ayrı RSA 3072 bit yayın anahtarı; eski uygulama imzasından doğrulanabilir APK Signature v3 imza geçmişi.
- İnternet, SMS, bildirim okuma, mikrofon, kamera ve depolama izni istenmez.
- Erişilebilirlik metaverisinde `canRetrieveWindowContent=true` (yalnız pencere listesi metaverisi), `canTakeScreenshot=false`,
  `isAccessibilityTool=false` açıkça belirtilir. Uygulama engelli kullanıcılar için yardımcı araç olduğunu iddia etmez.
- Tuş yakalama ve dokunma yetkisi kullanıcıya izin ekranından önce açıklanır.
- Reklam/analitik SDK'sı, uzaktan kod indirme, ağ sunucusu ve ağ bağlantısı yoktur.
- Üretim uygulamasının dışarı açık tek aktivitesi ana ekrandır; erişilebilirlik servisi sistemin
  `BIND_ACCESSIBILITY_SERVICE` yetkisiyle korunur.
- Test sağlayıcısı ve test aktivitesi yalnız ayrı `dev.kolmapper.probe` uygulamasındadır.

## Test sınırları

AOSP emülatöründe Google Play Services / Play Protect bulunmaz. ADB ile emülatöre yüklemek,
telefonda tarayıcı veya dosya yöneticisi üzerinden yüklemenin kısıtlarını aynı şekilde test etmez.
Dolayısıyla emülatör kurulumu Play Protect engelinin kaldırıldığını doğrulayamaz.
Özel imza anahtarı/parolası APK'ya veya kaynak arşivine dahil edilmez.

## 0.6 oturum sınırları

Servisin etkin olması tek başına kontrol veya overlay başlatmaz. Oturum kullanıcı eylemiyle başlar. Kilit, ekran kapanması, Durdur ve Vektör’dan açıkça Geri ile çıkış oturumu bitirir. Sistem hareketleri, Home ve son uygulamalar oturumu bitirmez; ilgili oyun dışındayken kontrol ve ikon gizlenir. Her pencere oluşturulmadan önce kilit ve ekran durumu kontrol edilir. Oturum sonlandırılırken animasyonlar ve ertelenmiş açılışlar iptal edilir; her pencere açılışı güncel kilit ve oturum kontrolünden geçer. Ana uygulamada ve oturum kapalıyken giriş yakalama bayrakları kapatılır. Home oturumu korur; kullanıcı ana uygulamada Durdur ile sonlandırabilir.

Android 12+ bulut yedekleme ve cihaz aktarımı için özel uygulama verileri açıkça hariç tutulur. Android ayarlarından servis izni kaldırılınca tüm pencereler ve temaslar temizlenir.

## 0.7 uygulama kapsamı

Her oyunun bağları, makroları, hazır durumu ve ikon koordinatları paket adına göre ayrı anahtarlarda saklanır. Boş veya bilinmeyen profil eski ortak kayda geri düşmez. Eski kayıt tek seferlik, açıkça seçilmiş aktarım için yedek kalır.

Etkin oturumda pencere olayının yalnız paket adı doğru profili belirlemek için kullanılır. Başlatılabilir uygulama listesi cihazda seçim amacıyla işlenir; uygulama kullanım geçmişi saklanmaz. Paket sorgusu MAIN/LAUNCHER ile sınırlıdır; QUERY_ALL_PACKAGES ve Usage Access yoktur. Gizlilik açıklaması bunu belirtir.

Başka uygulamaya geçmek kontrolü durdurur ve düzenleyici/ayar/nokta seçme pencerelerini kaldırır. Eski jest callback'leri temasları bıraktıktan sonra yeni profil yüklenir. Profil dönem kimliği, eski pencerenin gecikmiş kaydetme işleminin başka oyuna yazmasını engeller. Genel erişilebilirlik API'sinin önceden gönderilmiş, uygulama geçişine denk gelen bir dokunmayı geri alma garantisi yoktur; emülatörde bu sınır gözlenip TEST-REPORT.md içinde kaydedilmiştir.

## 0.8 değişiklikleri

Pencere listesi metadata denetimi için canRetrieveWindowContent=true ve flagRetrieveInteractiveWindows kullanılır. Bu Android API önkoşuludur; uygulama getRoot/getRootInActiveWindow çağırmaz, görünüm ağacı veya ekran metni istemez. Önceki sürümlerin capability=false notu 0.8 için geçerli değildir.

.rota yedekleri özel ikili kapsayıcı, Deflate ve SHA-256 kullanır; şifreli değildir ve imzalı kaynak doğrulaması sağlamaz. Dosya boyutu, genişletilmiş boyut, tipler, paket adları, koordinatlar ve içerik kontrol edilir. İçe aktarma açık kullanıcı onayıyla çalışır, aynı profilleri atomik değiştirir ve önceki durumu AtomicFile ile saklar. Harici dosya kullanıcı seçimiyle Android SAF üzerinden yazılır; bulut sağlayıcısı seçilirse sağlayıcı aktarabilir. Uygulama INTERNET izni istemez.

## 1.0 yayın sınırı

Minimum Android 10/API 29, target API 36, sürüm 1.0/code 10. Android 10/11 için fullBackupContent=false, Android 12+ için dataExtractionRules ve allowBackup=false kullanılır; kullanıcının .rota dışa aktarması ayrıdır. Ham analog yakalama Android 14+ gerektirir. Mevcut paket/imza ve .rota sürüm 1 korunur. Güncel yayın bilgileri play/PLAY-READINESS.md, gerçek test kapsamı TEST-REPORT.md içindedir.

## 1.0.1 odak ve bağlantı düzeltmesi

Kendi odaklanmış ayar penceresi yalnız aynı oyunun uygulama penceresi altta kaldığında düzenleyiciyi korur. VPN ve yabancı uygulama pencereleri oyun kontrolünü askıya alır. Eksik pencere kimliği içerik olayının sadece paket/kimlik metaverisiyle tamamlanır; kök düğüm veya metin istenmez.

Servis geçici olarak ayrılıp aynı süreçte yeniden bağlandığında kullanıcı oturumu RAM’den korunabilir. Uygulama bağlamındaki ekran kapanma/alınan kilit açma yayını yetkiyi siler; açık Bitir, Geri, profil değişikliği ve içe aktarma da yetkiyi kaldırır. Tam süreç ölümü, zorla durdurma, izin kaldırma veya yeniden başlatmada diskte oturum yetkisi bulunmaz. Ek foreground servis, ağ veya VPN izni üretim uygulamasına eklenmemiştir. vpnprobe yalnız emülatör test APK’sıdır.

1.1.0 görsel yenilemesi: üretim izni, ağ bağlantısı veya yeni SDK eklenmedi. Kol bağlantı etiketi yalnız Android InputDevice bilgisini işler. Giriş motoru, profil/yedek biçimi, oturum ve kilit kuralları 1.0.2 ile aynıdır.
