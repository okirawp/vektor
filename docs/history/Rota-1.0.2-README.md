# Rota 1.0.2

Android 10+ oyun kolu → oyuna özel ekran kontrolleri. `dev.kolmapper` paketi ve önceki imza geçmişi korunur.

## Kullanım

1. Ana ekranda oyununu seç. Kontrol izni kapalıysa “İzni aç” ile Android erişilebilirlik ayarlarından Rota'yı etkinleştir.
2. “Oyunu aç”a dokun. Yuvarlak ikon yalnızca tanımlı uygulamalarda görünür.
3. İkona dokunarak düzenlemeyi aç; + ile kol tuşunu veya çubuğu seçip ekranda yerini belirle. Atamaya dokunarak basılı tutma, tekrar aralığı, tekrar sayısı ve dokunma süresini ayarla.
4. İkonu kapatınca kontrol devreye girer. Tekrar sayısı 0 ise basılıyken sınırsızdır; tuşu bırakınca durur.

Her uygulama paketi ayrı tuş konumu, makro, analog alanı ve ikon konumu saklar. Tanımlı oyunlar arasında geçince doğru profil otomatik yüklenir. Tanımlanmamış bir uygulamada ikon ve eşleme gizlenir. Düzenlemeden başka uygulamaya geçmek düzenleyiciyi kapatır; oyuna dönünce kaydedilmiş atamalar otomatik çalışır.

Kilitlemek, Durdur veya Geri ile Rota'dan açıkça çıkmak oturumu bitirir. Sistem hareketleri, bildirim perdesi, Home ve son uygulamalar oturumu bitirmez; ilgili oyun dışında ikon ve kontrol gizlenir. Kilit açılınca kendiliğinden başlamaz. Home oturumu açık tutar; ana ekranda eşleme ve ikon gizlenir.

## 0.6'daki düzeni koruma

Önceki sürümün ortak düzeninde hangi oyuna ait olduğu bilgisi yoktur. Bu nedenle başka bir oyuna otomatik uygulanmaz ve silinmez.

**“Önceki düzeni taşı” → düzenin ait olduğu oyunu seç.** Örneğin eski Kafa Topu düzenin için Kafa Topu'nu seç. Bu tek seferlik işlem tuşları, makro değerlerini ve ikon konumunu yalnızca seçtiğin profile kopyalar. Var olan dolu bir profilin üzerine yazmaz. Eski ortak kayıt ayrıca yedek olarak korunur. Sonradan seçilen oyunlar boş, bağımsız bir düzenle başlar.

## Görünüm

Ana ekran kaydırmasızdır; dar veya kısa ekranda ikincil açıklamalar yerini kontrollere bırakır. Yardım her zaman soru işareti düğmesinden açılır. Güneş/ay üst sağda, gizlilik kalkan düğmesindedir. Sürüm alt yazısı kaldırılmıştır. Oyun içi ikon, oynarken de yarı saydam koyu renktedir; yalnız küçük durum noktası değişir. Açılış/basma efektleri sistemin animasyon ayarına uyar; kapanışta ek bekleme yoktur.

## Yedekleme

Üstteki yedek simgesi → Dışa aktar / İçe aktar. `.rota` dosyası Rota'ya özel sürümlü ikili kapsayıcıdır: typed alanlar, Deflate sıkıştırma, SHA-256 bütünlük kontrolü. JSON dosyası değildir; şifreleme veya parola koruması sağlamaz. Android dosya seçicisinde istediğin konuma kaydet; bulut sağlayıcısı seçersen dosyayı o sağlayıcı aktarır.

İçe aktarma dosyayı bütünüyle doğrular, kaç oyunu değiştireceğini sorar ve tek preferences işlemiyle birleştirir. Aynı oyunlar yedekteki düzenle değiştirilir; diğer oyunlar korunur. Öncesindeki tam düzen `before-restore.rota` olarak cihazda tutulur ve Yedekler menüsünden kurtarılabilir. Her atama kaydında önceki geçerli düzen de saklanır; bozuk güncel kayıt okunursa önceki sürüm kullanılır. Cihaz kaybı/kaldırma/veri silmeye karşı dosyayı ayrıca dışa aktar.

Kol eklenmesi/çıkarılması ayar yazmaz. Mevcut temaslar kaldırılır; oyun kontrolü açıksa yeniden bağlanmada ikon dokunuşu gerektirmez.

## Teknik sınırlar

Minimum Android 10 / API 29; ham analog hareket yakalama Android 14+ gerektirir; Android 16 hedeflenir. Android'in eşzamanlı dokunma sınırı en fazla 10'dur; fiziksel koldan gelmeyen tuş olayı geri oluşturulamaz. Shizuku veya root uygulamanın kullanımında gerekmez.

Uygulama değişimini pencere olaylarının paket adı ve getWindows() listesindeki kimlik/tür/odak bilgisiyle ayırt eder. Android bu liste için canRetrieveWindowContent yeteneğini zorunlu tutar; Rota getRoot/getRootInActiveWindow çağırmaz; ekran ağacını, metnini veya görüntüsünü okumaz. Kullanım geçmişi tutulmaz. Android'e uygulama değişiminden önce gönderilmiş bir dokunma geri alınamayabilir; genel erişilebilirlik jest API'si uygulama UID'sine atomik hedefleme sağlamaz. Eski temaslar boşaltılmadan yeni profil etkinleştirilmez. Bu, profil geçişi sırasında sıfır gecikme veya sıfır eski olay garantisi değildir.

Gerçek Honor Pad X9 / Ultimate 2C Bluetooth/USB aktarımı ve Play Protect sonucu bu ortamda ölçülemez. Doğrulama kapsamı TEST-REPORT.md; güvenlik SECURITY.md; yayınlanmadan hazırlanmış AAB ve mağaza belgeleri `play/PLAY-READINESS.md` içindedir.

## Derleme ve test

JDK 17+, SDK 36, AGP 8.10.1, Gradle 8.14.3:

```sh
./gradlew :app:assembleRelease :app:bundleRelease :app:assembleReleaseAndroidTest :app:lintRelease :probe:assembleDebug :probe2:assembleDebug
mkdir -p tests/out
javac -d tests/out app/src/main/java/dev/kolmapper/{RepeatState,BadgePosition,ContactPolicy,OverlayPlacement,AnalogInput,HoldPath,InputLedger,TouchStartPlan,SessionRules,ProfileRules,FocusRules,BackupCodec}.java tests/*.java
for test in RepeatStateTest ContactPolicyTest OverlayPlacementTest InputLedgerTest AnalogInputTest TouchStartPlanTest SessionRulesTest ProfileRulesTest FocusRulesTest BackupCodecTest; do java -cp tests/out dev.kolmapper.$test; done
```

`ProfileInstrumentation` 22 kontrolle ikili yedeklemeyi, geri yüklemeyi, benzer paket adlarına sahip diğer oyunların korunmasını ve iki profilin depolama ayrımını ve eski ortak düzenin açıkça taşınmasını doğrular; `-e seed_profiles true` sadece özel emülatörde A/B fixture'larını hazırlar. `LayoutInstrumentation` ana ekranda scroll container veya kesilmiş kontrol olmadığını denetler. `scripts/session-resume-test.py` iki ayrı Android paketi ve gerçek Linux uinput olayları kullanır. Testler root erişimli API 36 emülatör, 1280×800 / 160 dpi ve özel sanal dokunmatik/oyun kolu fixture'ı içindir. Önceki sürümlerin betikleri tarihsel tanılama içindir; ortak profil ve eski ana ekran varsayımları 1.0 için geçerli değildir.

`probe`, `probe2` ve `vpnprobe` ayrı test uygulamalarıdır; üretim APK/AAB'sine dahil değildir. Test araçlarının root erişimi, uygulamanın çalışma izni değildir. APK için `scripts/sign-release.sh` dışarıdan verilen özel keystore ve imza geçmişini kullanır. AAB release/yükleme anahtarıyla ayrıca imzalanır. Özel anahtar/parola kaynak arşivine konmaz.

Teslim arşivleri `scripts/package-rota.py` ile hazırlanır; girdi oturumu son kontrolü `scripts/final-session-smoke-test.py` ile özel emülatörde yapılır.

1.0.1: Tuş ayarı odağı oyun üzerinde korunur. VPN/izin penceresinde giriş ve overlay gizlenir, oyuna dönüşte profil otomatik gelir. Geçici erişilebilirlik yeniden bağlantısı yalnız aynı işlem belleğinde ve kilit açılmışsa korunur; kilit/Bitir yetkiyi kaldırır. Tam işlem ölümü veya zorla durdurma sonrasında eski oturum diskten başlatılmaz.

1.0.2: OEM modal odak geçişi, dönüşte güncel ekran ölçüleri, görünür kontrol silme ve yeni tuşlarda Basılı tut varsayılanı. Mevcut makrolar korunur.
