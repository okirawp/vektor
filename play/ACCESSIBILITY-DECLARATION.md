# Erişilebilirlik API beyanı ve inceleme videosu

Vektör engellilik odaklı bir erişilebilirlik aracı değildir (`isAccessibilityTool=false`). Temel işlevi kullanıcının manuel belirlediği oyun kolu → ekran dokunma eşlemeleridir.

API'ler: profil seçimi için pencere olayının paket adını izleme, key filtering, Android 14+ raw joystick motion capture ve dispatchGesture. Pencere listesi getWindows() ile kimlik/tür/odak bilgisi için alınır. Bu API Android’de canRetrieveWindowContent=true gerektirir; kök düğüm veya görünüm ağacı istenmez. Pencere içeriği alınmaz; UI ağacı okunmaz; ekran görüntüsü alınmaz. Otomatik karar verme, ekran tanıma, başka uygulama ayarlarını değiştirme veya izin engellerini aşma yoktur. Tekrarlar yalnızca kullanıcının tuttuğu kol tuşuyla ve kaydettiği sabit ayarla çalışır; bırakınca durur.

Uygulama erişilebilirlik ayarlarına gitmeden önce amacı ve veri kullanımını açıklayan bir diyalog gösterir. Kullanıcı “Kabul et ve devam et” ile devam eder; Android servis iznini ayrıca kendisi verir. Ana ekran gizlilik açıklamasına erişim sağlar.

Video sırası:
1. İlk açılış, erişilebilirlik açıklaması ve kullanıcı onayı.
2. Android ayarlarında servisi aç; uygulamaya dön. Oturum başlatılmadan overlay olmadığını göster.
3. Oyun seç, “Oyunu aç”, Vektör ikonuyla düzenleme, tuş ekleme ve nokta seçme. İki uygulamada farklı atamaların ayrı kaldığını göster.
4. Basılı tutma ve makro ayarı; ikonla oynama moduna geçiş.
5. İki tuşun birlikte çalışması, bırakınca temas ve tekrarların durması.
6. İkonla düzenlemeye dönünce kontrolün durması.
7. Ekranı kilitle: overlay kaybolur. Kilidi aç: kendiliğinden başlamaz.
8. Home, bildirim perdesi ve son uygulamaları aç: oyun dışındayken ikon gizlenir; oyuna dönünce otomatik gelir. Durdur ile oturumu bitir.
9. Android ayarlarından izni kaldır.

İnceleme için oturum açma hesabı veya abonelik gerekmiyor. Bluetooth/USB Android oyun kolu ve dokunmatik test alanı kullanılabilir. Ekran eşlemesi sadece kurulumda seçilen çözünürlük/yön için uygulanır. Gerçek kontrol videosu ve güncel politika formu yayın sahibi tarafından gönderilmelidir.

1.0: Minimum Android 10; ham analog yakalama Android 14+. Ret akışında Vazgeç ile dönülür; izin yeniden istendiğinde aynı açıklama gösterilir. Makrolar yalnız basılı düğme süresince sabit kullanıcı kuralıdır; kaynak: https://support.google.com/googleplay/android-developer/answer/10964491

1.0.1: Pencere kimliği değiştiğinde paket eşlemesini onarmak için içerik-değişim ve odak olaylarının yalnız paket/pencere meta verisi işlenir. Olayın metni, kaynak düğümü ve ekran içeriği okunmaz. Geçici servis bağlantı kesilmesinde başlatılmış oturum izni yalnız süreç belleğinde tutulur; kilit, açık durdurma veya süreç kapanması bu izni kaldırır. Video akışına VPN açma/onaylama ve oyuna geri dönüş de eklenmelidir.

1.2.0 yalnız görsel kimlik ve arayüz yenilemesidir. Oturum açma ve izin açıklama/onay akışları korunur. Mevcut Rota kurulumu aynı paket kimliğiyle güncellenir.
