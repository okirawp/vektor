# GitHub hazırlığı doğrulaması

- Yerel Git deposu main dalında başlatıldı. Mevcut kaynaklar referans commit'ine alındı. CI, ekran testi ve belgeler ayrı commit edildi.
- Derleme ve test iş akışı push, pull_request ve workflow_dispatch olaylarında çalışır. JDK 17 ve SDK 36 kurar, 10 mevcut host davranış testi grubunu ve Android birim testlerini çalıştırır, debug APK'yı artifact olarak yükler.
- Ekran iş akışı Android 15 x86_64 emülatöründe altı ekranı doğrular. Ana ekran, bağlantısız kontrolcü önizlemesi, gizlilik, yedekler, çıkış ve paylaşım diyalogları alınır. Hatalı ekran veya eksik PNG iş akışını başarısız yapar.
- Ekran yakalama kodu yalnız androidTest içindedir. Uygulamanın release kaynaklarına test komutu veya ekran görüntüsü yeteneği eklenmedi.
- CI yalnız contents: read izni ister. İmza parolası veya GitHub secret gerektirmez.
- Debug paketi dev.kolmapper.debug kimliğiyle üretilir. Mevcut üretim uygulamasından ayrı kurulur. Debug LeakCanary bağımlılığı korunmuştur.
- Yerel doğrulamada debug APK, screenshot test APK'sı ve 19 Android birim testi başarıyla derlendi/çalıştı. 10 host test grubu geçti. Shell dosyaları bash -n, workflow dosyaları YAML ayrıştırıcı ile kontrol edildi.
- Bu çalışma sırasında GitHub hesabında repo oluşturulmadı ve uzak iş akışı çalıştırılmadı. Kaynak ZIP'i ve telefondan Codespaces yükleme rehberi hazırlandı.
- Yerel emülatör oturumu çalışma ortamının yeniden başlamasıyla kapandığından yeni altı ekran testi uçtan uca doğrulanamadı. İlk GitHub çalıştırması bu doğrulamayı yapacaktır. Fiziksel kontrolcü testi gereklidir.
- app/src/main içindeki kaynakların tamamı referans commit ile aynıdır. Mapper motoru, veri kaydı, izinler, servisler ve yedek formatına değişiklik yapılmadı.
