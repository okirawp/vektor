# Vektör 1.2.0 doğrulama

Paket dev.kolmapper; versionCode 14, Android 10+ / target 36.

## Bu sürümde geçen kontroller

- Release APK/AAB derlendi; release lint tamamlandı.
- Robolectric 4.14.1 ile API 29 ve 35: 12 test, sıfır hata. Yeni kurulumda koyu tema, tek tema eylemi, güneş/ay geçişi, tema değişiminde atamaların korunması, geri onayının açılması/iptali/tekrar açılması, açık tercih varken de koyu başlangıç rengi, API 35 splash rengi, oyunlara özel rehber durumu, rehberin yedek verilerini değiştirmemesi ve rehber sayfalarında ilerleme/geri dönüş. Başlangıç rengi native graphics modunda çizilerek ölçüldü.
- Ana Java regresyonları: 208 yedek bütünlük kontrolü, 90.000 basılı tut devamı, 10.000 beş tuş dizisi, 100.000 çoklu temas dizisi, 288 overlay yerleşimi ve 11 odak senaryosu dahil mevcut testler geçti.
- 13 veri/giriş sınıfı ve MapperService kontrol/multi-touch motoru 1.1.0 ile bayt düzeyinde aynı.
- Android 10 emülatörüne imzalı APK önceki uygulamanın üzerine kuruldu. Kayıtlı iki oyun ve altı/bir kontrol korunmuş olarak görüntülendi. Koyu/açık ekran, tek güneş/ay düğmesi ve geri onayı native ekran görüntülerinde incelendi.
- Oyun içi rehber native emülatörde gösterildi; seçili oyuna girildiğinde servis foreground=true ve armed=true olarak otomatik etkinleşti.
- APK aynı release sertifikasıyla doğrulandı. Robolectric ve LeakCanary release bağımlılığı değildir.

## Sınırlar

API 35 Robolectric testi fiziksel Android 15 testi değildir. Bu sürüm fiziksel Honor/8BitDo veya gerçek Kafa Topu üzerinde denenmedi. Android 16 cihaz testi ve Google Play/Play Protect onayı iddia edilmez. Emülatör yavaşlığı nedeniyle gecikme/pil için sayısal performans iddiası yoktur. Release içine yeni internet izni veya tanılama SDK’sı eklenmedi.
