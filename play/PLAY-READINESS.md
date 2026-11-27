# Vektör 1.2.0 — Google Play yayın paketi

Teknik sürüm paketi hazırlandığında imzalı AAB yükleme dosyasıdır; APK mevcut kurulumları güncellemek ve yerel kurulum içindir. Yayınlama yapılmaz. Console hesabı, geliştirici kimliği, gerçek iletişim adresi, barındırılmış gizlilik URL’si ve Google incelemesi bu arşivle otomatik tamamlanamaz.

## Teknik kimlik

- applicationId `dev.kolmapper`; versionCode **14**, versionName **1.2.0**.
- Minimum Android **10 / API 29**; compile/target **Android 16 / API 36**.
- Tuş eşleme, basılı tutma, makro, oyun profilleri ve .rota yedekleri Android 10+. Ham analog yakalama Android 14+; eski sürümlerde bu sınır arayüzde ve mağaza metninde açıklanır.
- Release debuggable=false. Java uygulaması; yerel .so kütüphanesi yok. ABI/64 bit ve 16 KB native sayfa hizalama koşulu uygulanacak native dosya bulunmuyor.
- INTERNET, QUERY_ALL_PACKAGES, MANAGE_EXTERNAL_STORAGE, kullanım erişimi, kurulum veya tehlikeli runtime izinleri yok. Erişilebilirlik özel izni kullanıcı onayıyla Android ayarlarında açılır.
- canRetrieveWindowContent=true pencere listesini almak için Android’in zorunlu yeteneğidir. Yalnız pencere kimliği/tür/odak ve olay paket adı kullanılır; getRoot/getRootInActiveWindow, görünüm ağacı/metin veya ekran görüntüsü istenmez. isAccessibilityTool=false.
- Erişilebilirlik açıklaması olağan kurulum akışında ayrı diyalogdur; Vazgeç ve Kabul et ve devam et seçenekleri vardır. Ayarlarda ayrıca Android onayı verilir.
- Makro yalnız kullanıcının tuttuğu düğme boyunca, kendi kaydettiği sabit kuralla çalışır; bırakınca durur. Ekran tanıma, otonom karar verme veya izin engellerini aşma yoktur.
- Önceki paket kimliği, imza geçmişi, kayıtlı profiller ve .rota sürüm 1 dosyaları korunur. Güncellemek için uygulama verileri silinmez.

## Console’da yayın sahibinin tamamlayacağı bilgiler

1. Gerçek destek e-postası/geliştirici iletişim bilgileri ve hesap doğrulaması. Uydurma bilgiler arşive eklenmez.
2. privacy.html içindeki iletişim bölümünü gerçek adresle doldurup kalıcı, herkese açık HTTPS URL’sinde barındır. Console gizlilik alanına bu URL’yi gir. Uygulamada çevrimdışı politika ayrıca bulunur.
3. ACCESSIBILITY-DECLARATION.md form taslağı ve inceleme videosunu gönder; izin açıklaması, kabul/ret, gerçek kol eşlemesi ve bırakınca durmayı göster. API kullanımı Google onayı gerektirir.
4. DATA-SAFETY.md metnini mevcut Console formuna aktar. İçerik derecelendirmesi, hedef kitle, reklam yok, hesap gerekmiyor ve uygulama erişimi alanlarını tamamla. Önerilen mağaza kategorisi Araçlar’dır; uygulama çocuklara özel tasarlanmamıştır. Yaş/ülke/ücret tercihleri yayın sahibi tarafından doğrulanır.
5. Play App Signing: mevcut yan yüklenen kurulumların güncellenmesi isteniyorsa aynı release uygulama imzalama anahtarı kullanılmalıdır. Console’un varsayılan farklı anahtarını seçmek doğrudan güncelleme uyumunu bozabilir. Yükleme ve uygulama imzalama anahtarları farklı rollerdir. Özel anahtarlar bu arşivde bulunmaz; güvenli imzalama alanındadır.
6. Hesaba uygulanan kapalı test koşulları, dahili test ve Play ön lansman raporunu tamamla. Gerçek Bluetooth/USB cihaz kapsamını test raporundaki sınırlarla birlikte değerlendir.
7. STORE-LISTING-tr.md ve graphics içindeki gerçek ekran görüntülerini Console’a yerleştir. Üçüncü taraf oyun markaları kullanılmaz.

Bu adımlar tamamlanmadan “Console’a gönderildi / Google onayladı” denmez. Play Protect sonucu veya tüm üreticilerde hatasızlık garanti edilemez.

## Kontrol edilen resmi kaynaklar — 20 Kasım 2026

- Hedef API: https://developer.android.com/google/play/requirements/target-sdk
- Erişilebilirlik ve sabit kurallı otomasyon: https://support.google.com/googleplay/android-developer/answer/10964491
- Hassas API politikası: https://support.google.com/googleplay/android-developer/answer/16558241

1.2.0 adı Vektör’dür; önceki Rota ile aynı paket/imza ve yedek biçimini kullanır. Girdi motoru değiştirilmedi; yeni izin veya SDK yoktur. Yeni simge ve tanıtım grafiği play/graphics altındadır. QA ekran görüntülerindeki Touch Probe test oyunu açıklanmalıdır; eski Rota görselleri yeni sürüm için kullanılmamalıdır.
