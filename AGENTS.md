# Vektör geliştirme kuralları

- VEKTÖR ULTIMATE tasarımını ve çalışan özellikleri koru.
- Mapper çekirdeği, tuş eşleme, giriş işleme ve sıralama, dokunma simülasyonu, analog, makro, Shizuku, servisler, izinler, veritabanı, yedekleme/geri yükleme mantığı, .rota formatı ve kayıtlı kullanıcı verilerini değiştirme.
- İş bu alanlara dokunmayı gerektirirse dur, nedenini açıkla ve kullanıcıya sor.
- Tek marka vurgu rengi cyan olsun. Mor/lavanta kullanma. Diğer renkleri anlamlı durumlar için kullan.
- Renk ve boyut değerleri yalnız merkezi Ui.java içinde, süre ve hareket değerleri merkezi Motion.java içinde tanımlansın. Ekranlara dağılmış sabitler ekleme.
- Köşeler 16–20 dp, minimum dokunma alanı 48 × 48 dp olsun. Diyaloglar dahil tüm bileşenler uygulamanın temasını izlesin.
- Butonlarda basma ölçeği %96 olsun. Yalnız Android sistem animasyon tercihini izle. Animasyonlar giriş işlemeden bağımsız çalışsın.
- Pil, bağlantı durumu ve ilerleme yüzdesi gerçek veriden gelsin. Bilinmeyen değer için tahmin üretme. CI test verisini kullanıcı verisine yazma.
- Telefon/tablet, dikey/yatay, bölünmüş ekran, koyu/açık tema ve büyük sistem yazısını destekle.
- Her değişiklikten sonra derle ve mevcut tuş eşleme, makro, yedek/geri yükleme testlerini çalıştır. Bozulma varsa kendi değişikliğini düzelt veya geri al. Kullanıcının çalışmasını geri alma.
- Temel doğrulama için scripts/ci/host-tests.sh ve ./gradlew :app:assembleDebug :app:testReleaseUnitTest çalıştır. Ekran testi için -PciScreenshots=true kullan.
- Her tamamlanan aşamayı ayrı commit et. Anahtar, parola, yerel SDK yolu ve kullanıcı yedeğini commit etme.
- Bitirince değişen dosyaları, yapılan testleri ve kalan sorunları kısa raporla. Çalıştırılmamış testi geçmiş sayma.
