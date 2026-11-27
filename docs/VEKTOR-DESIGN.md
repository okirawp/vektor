# Vektör görsel kimliği

Yön fikrini taşıyan özgün V biçimi ve ayrı yön ucu. Tasarımda emoji, stok logo, sürekli hareket, oyun görüntüsünü taklit eden sahte veriler ve büyük renkli oyun ikonu kullanılmaz.

Koyu: #0F131B zemin, #181F2B yüzey, #A5BAFF vurgu. Açık: #F4F6FA zemin, beyaz yüzey, #3357D5 vurgu. Birincil metinler ve normal boyuttaki düğmeler her iki temada en az 4.5:1 kontrast hedefini karşılar.

Android’in sans-serif / sans-serif-medium yazıları kullanılır; font indirilmez. Düğmeler ve tema kontrolleri en az 48 dp, oyun içi ikonun dokunma alanı 56 dp. Ana ekran kaymaz; küçük yatay ekranda ikincil açıklamalar öncelikli oyun eylemlerine yer açar. Büyük yazıda tek satırlık oyun adı kısaltılabilir; tam ad erişilebilirlik açıklamasında korunur.

Ana ekran: marka + tema, seçili oyun + gerçek kontrol sayıları, ana eylem, bağlantı etiketi, etiketli kısayollar. Geniş ekranda statik kullanım çizimi ve üç adım yer alır. Görsel çizim canlı cihaz durumu olarak sunulmaz.

Overlay: nötr grafit daire, açık renk yön işareti, gerçek çalışma durumunu gösteren küçük nokta. Düzenleyici yeni pencereler veya yeni izinler eklemez; çalışan 1.0.2 odağı ve oturum yönetimi korunur. Aynı pencere/panel sistemine yalnız stil ve metin değişiklikleri uygulanır.

Ad değişikliği paket kimliğini, veritabanını veya yedek biçimini değiştirmez. Önceki .rota yedekleri geçerlidir.
