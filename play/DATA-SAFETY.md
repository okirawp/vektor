# Veri güvenliği — mevcut kod için taslak
- Kullanıcı verisi toplama: Hayır.
- Kullanıcı verisi paylaşma: Hayır.
- Reklamlar / analiz / çökme gönderimi: Yok.
- Hesap oluşturma / giriş: Yok; hesap silme akışı uygulanmaz.
- Yerel veriler: Oyun paketleri, oyuna özel tuş konumları, davranış ayarları, tema ve ikon konumu SharedPreferences içinde. Ağdan çıkmaz. Android uygulama verilerini silmek veya uygulamayı kaldırmak bunları kaldırır. Android bulut yedeklemesi kapalıdır.
- Aktarım şifreleme: Sunucuya aktarım yok; güncel Console formunun koşullu sorularına göre yanıtla.
- Kol girişleri yalnızca açık oturum sırasında işlenir; tanılama sayaçları süreç belleğinde tutulur ve sunucuya gönderilmez.
- Erişilebilirlik API kullanımı ayrı beyan gerektirir; veri toplamama beyanı bunu ortadan kaldırmaz.

Her yeni SDK, ağ özelliği veya veri akışında bu yanıtları yeniden değerlendir. Gerçek destek adresi ve herkese açık gizlilik URL'si yayın sahibi tarafından tamamlanmalıdır.

0.7: Başlatılabilir uygulamalar yalnız uygulama seçimi için yerel olarak listelenir. Etkin pencerenin paket adı profil seçimi için işlenir; uygulama kullanım geçmişi kaydedilmez. Bu bilgiler cihazdan gönderilmez. QUERY_ALL_PACKAGES veya kullanım erişimi izni yoktur.

0.8: Odaklanmış pencerenin kimlik/tür bilgisi işlenir, kök düğüm veya içerik istenmez. Kullanıcının seçtiği dosya sağlayıcısına .rota dışa aktarma vardır; kullanıcı bulut sağlayıcısı seçerse aktarımı sağlayıcı yapar. Vektör'nın INTERNET izni yoktur. Yerel önceki düzen ve içe aktarma öncesi kurtarma dosyası da cihazda tutulur.

1.0.1: Pencere içerik/değişim olaylarından yalnız paket adı ve pencere kimliği alınır; içerik metni/ağacı okunmaz. Geçici servis yeniden bağlanması için izin ve pencere kimlikleri yalnız işlem belleğinde tutulur, kilitte silinir; diske veya ağa aktarılmaz.
