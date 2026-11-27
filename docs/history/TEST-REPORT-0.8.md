# Rota 0.8 — doğrulama

Android 16/API 36 AOSP x86_64 yazılımsal emülatör, 1280×800 / 160 dpi. Gerçek Honor Pad X9 / 8BitDo Ultimate 2C Bluetooth ve USB donanım aktarımı bu ortamda ölçülmez. Play Protect sonucu veya yüzde yüz hatasızlık garantisi verilmez.

## Bu sürümün değişiklikleri

- Sistem hareketlerinde oturumun Rota task varlığına bağlı olarak kapanması kaldırıldı. Pencere kimliği/türü/odağıyla ilgili oyuna dönüş izlenir. Oyun dışında overlay ve giriş gizlenir; oturum korunur. Kilit/Durdur/açıkça Rota’dan Geri ile çıkış oturumu bitirir.
- InputDevice değişimi/çıkarılması `stopAll()` ile armed durumunu kalıcı kapatıyordu. Temas temizliği sonrasında önceki oynama durumu korunur. Hot-plug atama kaydı yazmaz.
- Düzenleme sırasında kalıcı ready=false yazılması kaldırıldı. Kaydedilmiş atamaları olan oyun etkinleşince ek ikon dokunuşu olmadan çalışır.
- Atamalar diske commit edilir; önceki geçerli nesil saklanır. Bozuk güncel JSON sessizce kısmi liste olarak kullanılmaz; önceki kayıt denenir.
- .rota sürümlü ikili yedek; Deflate + SHA-256 bütünlük kontrolü. İçe aktarma tür/paket/alan/koordinat kontrollerinden sonra onayla atomik birleştirilir. Öncesindeki tam durum AtomicFile kurtarma dosyasında tutulur. Şifreleme yoktur.
- Aramalı oyun kartları, kayıtlı oyunların öne alınması ve üstte Yedekler kısayolu.

## Geçen kontroller

- Release derlemesi ve lint. APK imzası, imzalı AAB ve bundletool doğrulaması.
- Saf Java girdi regresyonları: tekrar zamanlaması; 10.000 sürekli temas, 10.000 eşzamanlı beş tuş dizisi, 90.000 analog devamı, 100.000 pointer kimliği dizisi ve 288 yerleşim.
- 208 ikili yedek kontrolü: bütün tiplerin ve iki profilin round-trip dönüşümü, deterministik çıktı, tüm kesilme uzunlukları, header/version/hash bozulması, sıkıştırılmış veri bozulması, boyut/tip sınırları.
- Son teslim APK’sında 22 Android depolama kontrolü: profil ayrımı, eski ortak düzenin açıkça taşınması, tam ikili yedek dönüşümü, iki profilin farklı değerlerinin geri gelmesi, içe aktarma öncesi kurtarma kopyası, bozuk güncel kayıtta önceki nesil ve bozuk içe aktarmada mevcut kayıtların değişmemesi. Boş kurulumdan geçerli yedek ve bozuk güncel atamanın önceki nesilden dışa aktarılması da geçti.

- Native uinput koşusu geçti: sağ + A/X/R2 birlikte; bırakınca tüm temaslar kapanır. Sağ basılıyken cihaz gerçekten kaldırılır; temas kapanır, yeni cihaz eklendiğinde ikona dokunmadan sağ yeniden çalışır ve bırakılınca durur.
- Home/launcher’da ikon yoktur, oturum korunur; oyuna dönünce otomatik gelir. Bildirim perdesi ve son uygulamalar oturumu bitirmez. Son uygulamalardaki gerçek oyun kartına fiziksel dokunmayla dönüş test edildi; yalnız adb am start komutu recents’i kapatmadığı için o yöntem başarı sayılmadı.
- Tanımlanmamış Ayarlar ekranında ikon ve giriş yoktur; oyuna dönüş Rota’yı yeniden açmayı gerektirmez. Yarım bırakılan düzenlemeden dönüş kaydedilmiş profili otomatik çalıştırır. A/B profilleri farklı kalır; A makrosu tekrar çalışır ve bırakınca durur.
- İlk fixture’ın tüm tuş alanları yeniden bağlantıdan sonra aynı kaldı. Sonraki sistem hareketleri, düzenleme kesilmesi ve A/B geçişleri sonrasında kayıtlı bindings metinleri de değişmedi.

Native koşu 0.8 aday APK’sında (`95953a…1035d`) geçti. Girdi/ekran/dosya seçici koşusundaki APK (`9fd29d…3a256`) sonraki yedek kurtarma doğrulamalarını, açıklama metinlerini ve ekran güvenliği koşulunun saf SessionRules fonksiyonuna taşınmasını içerir; bu koşul ayrıca tüm kombinasyonlarla test edildi. Daha sonra içe aktarmada yalnız tam paket adının alanlarını değiştiren düzeltme eklendi; girdi/overlay kodu değişmedi. Bu teslim APK’sında 22 depolama kontrolü ayrıca çalıştırıldı. Ekran ve dosya seçici kontrolleri aşağıda ayrıca doğrulandı. Ham sonuçlar Play hazırlık paketindeki verification dizinindedir.

## Sınırlar

Pencere listesi Android’de canRetrieveWindowContent=true yeteneği ister. Rota yalnız getWindows() kimlik/tür/odak bilgisini alır; getRoot/getRootInActiveWindow, ekran metni, görünüm ağacı veya ekran görüntüsü istemez. İnternet izni yoktur. Kullanıcının seçtiği bulut dosya sağlayıcısı .rota yedeğini aktarabilir.

Daha önce Android’e gönderilmiş bir dokunma uygulama geçişiyle atomik geri alınamaz. 0.7 testinde bir DOWN/UP çifti yeni pencereye ulaştı; açık temas kalmadı. Bu API sınırı devam eder; eski temaslar boşaltılmadan yeni profil devreye alınmaz.

Test uinput daemon ve probe APK’ları üretim paketinde bulunmaz. Tuş testleri UiAutomation gamepad enjeksiyonu yerine gerçek Linux input olayları kullanır. Native hot-plug USB/Bluetooth bağlantı protokolünün donanım testi değildir.

Yayınlama yapılmaz. Console inceleme videosu, gerçek yayıncı iletişim bilgisi ve barındırılmış gizlilik URL’si yayın sahibince tamamlanmalıdır.

## 0.8 ekran kontrolleri

1280×800 yatay, 800×1280 dikey ve 360×640 küçük ekran/yüzde 130 yazı boyutu geçti. Her birinde 7 görünür tıklanabilir kontrol ve 5 ikon vardır; kesilmiş kontrol ve ana sayfada ScrollView yoktur. Açık/koyu düğmeleri gerçek UI’den çalıştırıldı; ekran görüntülerinde arka plan renkleri de doğrulandı. Aramalı oyun kartlarında A=6, B=1 kontrol ayrı görünür. Yeni ekran görüntüleri Play hazırlık paketine eklendi.

## Android dosya sağlayıcısıyla gerçek yedek akışı

Arayüzden Dışa aktar → Android DocumentsUI → Downloads → Save çalıştırıldı. Gerçek 407 baytlık .rota dosyası indirildi; bağımsız host çözümleyicisi ROTA başlığını/sürümünü, SHA-256 ve Deflate verisini doğruladı, A/B profillerini çıkardı.

Arayüzden İçe aktar → aynı dosyayı seç → Geri yükle çalıştırıldı. Her iki oyunun bindings metni önceki değerleriyle aynı kaldı. Cihazdaki AtomicFile kurtarma dosyası eski test durumundan yeni tam profile değişti ve dışa aktarılan tam ikili veriye eşit oldu; bu, gerçek activity-result akışının restore işlemini tamamladığını doğrular. Kaynak kayıt değiştirilip geri getirilmesi ayrıca 22 Android depolama kontrolünde denendi.

Bulut dosya sağlayıcıları ve diğer üretici dosya uygulamaları bu oturumda test edilmedi.

## Son girdi kontrolü ve profil koruma düzeltmesi

9fd29d…3a256 derlemesinde 12 son girdi kontrolü geçti: servis bağlanması, ikon dokunmadan otomatik oynama, gerçek sağ DOWN, basılıyken gerçek cihaz çıkarma/temasın kapanması, yeniden bağlanma ve yeni sağ basışı/bırakışı, Home’da görünmeme ve oturumun korunması, oyuna otomatik dönüş, ekran kapanınca oturumun ve tüm overlay pencerelerinin kapanması.

Ardından yalnız Backups.restore alan temizliği değişti: önekle eşleştirme yerine her oyunun 5 alanı tam anahtarla değiştirilir. Böylece `test.rota.gameA` içe aktarılırken `test.rota.gameA.child` profilinin verileri silinmez. Son depolama koşusu bu ilişkinin ve bağımsız başka bir mevcut oyunun korunmasını da içerir. Girdi motoru, pencere takibi, ana ekran ve dosya seçici akışı bu son düzeltmede değiştirilmedi.

## Teslim kimliği

Son APK SHA-256: `6e653ff0b18241597ebcc44b20f088c3c5385a11fae179554e20be50d390cbea`. Bu APK’ya yükseltme kuruldu; 22 Android depolama kontrolü geçti. Son değişiklik yalnız içe aktarmada profil alanlarının tam adla temizlenmesidir; girdi ve arayüz kodu önceki geçen koşuyla aynıdır. Son derleme/lint, APK imzası ve imzalı AAB doğrulaması geçti.
