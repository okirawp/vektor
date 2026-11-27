# Vektör

Android 10 ve üzeri için uygulamaya özel kol eşleme aracı. Java ve yerel Android görünümleri kullanır. Ham analog yakalama Android 14 ve üzerini gerektirir.

Koyu tema varsayılandır. Ana ekranda kayıtlı uygulamalar ve kontrolcü durumu, Ayarlar içinde rehber, gizlilik ve .rota yedekleri bulunur. Atamalar cihazda saklanır. .rota dosyalarında bütünlük doğrulaması vardır ve dosyalar şifreli değildir.

Üretim uygulaması app modülündedir. probe, probe2, vpnprobe ve androidTest QA içindir. Release derlemesi özel imza anahtarlarını dışarıdan alır. GitHub Actions debug paketini kendi test anahtarıyla imzalar.

## GitHub'a telefondan yükleme

1. Hazırlanan kaynak ZIP'ini telefonuna indir. Tarayıcıdan github.com hesabına gir. New repository ile örneğin vektor adlı bir repo oluştur. Görünürlüğü seç ve başlangıç README'si ekle.
2. Repo sayfasında Code → Codespaces → Create codespace on main seç. Telefon tarayıcısında Masaüstü sitesi görünümünü aç. Codespaces kullanım kotası GitHub hesabına bağlıdır.
3. Açılan VS Code görünümünde Explorer bölümüne kaynak ZIP'ini yükle. Terminal → New Terminal aç.
4. Aşağıdaki komutları sırayla çalıştır. ZIP'i farklı bir konuma yüklediysen ilk komutta o yolu kullan.

```sh
unzip -o Vektor-GitHub-kaynak.zip
rm Vektor-GitHub-kaynak.zip
chmod +x gradlew scripts/ci/*.sh
git add .
git commit -m "Vektor kaynaklari ve otomatik testler"
git push
```

5. ZIP içeriği repo köküne açılmalı. .github, app, gradle, gradlew ve AGENTS.md aynı düzeyde olmalı. .github gizli klasördür ve pakete dahildir.
6. Actions sekmesini aç. Derleme ve test iş akışı APK ve birim testleri üretir. Emülatör ekran görüntüleri altı ekranı PNG olarak üretir. Her push ve pull request ile çalışırlar. Run workflow ile elle de başlatabilirsin.
7. İşin bitince Codespaces sayfasından ortamı durdur. Sonraki değişiklikleri GitHub web arayüzünden düzenleyip commit edebilirsin.

Kaynak ZIP'ini tek dosya olarak repo içine yüklemek Actions'ın projeyi bulmasını sağlamaz. Codespaces terminalinde kaynakları açma adımını tamamla.

## Telefondan APK indirme ve kurma

1. GitHub hesabınla giriş yap ve reponun Actions sekmesini aç.
2. Derleme ve test içindeki yeşil tamamlanmış çalıştırmayı seç.
3. Sayfanın altındaki Artifacts bölümünden Vektor-debug dosyasını indir. Dosya ZIP olarak gelir.
4. Telefonun Dosyalar uygulamasında ZIP'i aç ve içindeki app-debug.apk dosyasına dokun.
5. Android isterse dosyayı açtığın tarayıcıya veya Dosyalar uygulamasına Bilinmeyen uygulamaları yükle izni ver. Kurulumdan sonra bu izni kapatabilirsin.
6. Test sürümü dev.kolmapper.debug kimliğiyle ayrı kurulur. Yayın sürümünün verilerini taşımaz. Debug derlemesinde LeakCanary geliştirme aracı vardır.
7. Emülatör ekran görüntüleri çalıştırmasındaki Vektor-ekran-goruntuleri artifact'i ana ekran, bağlantısız kol önizleme, gizlilik, yedekler, çıkış ve paylaşım diyaloglarını içerir.

Artifact indirmek için GitHub oturumu gerekir. Dosyalar GitHub'ın artifact saklama süresince tutulur. Hatalı çalıştırmalarda kırmızı adımı aç veya Summary içindeki kısa hata raporunu ve tanı artifact'ini incele.

## Otomatik doğrulama

JDK 17, Android SDK 36 ve Gradle Wrapper kullanılır.

```sh
scripts/ci/host-tests.sh
./gradlew :app:assembleDebug :app:testReleaseUnitTest
./gradlew -PciScreenshots=true :app:assembleDebug :app:assembleDebugAndroidTest
scripts/ci/screenshots.sh
```

Son komut için kilidi açık bir test emülatörü gerekir. Komut emülatör çözünürlüğünü değiştirir ve ayrı debug paketini kurar. Ekran testi fiziksel kol bağlantısı üretmez. Açılan ekranı doğrular ve eksik görüntüde başarısız olur. Mapper girişleri ve arşiv uyumluluğu mevcut davranış testleriyle denetlenir.
