# İsteğe bağlı kol modeli

Önizleme varsayılan olarak katmanlı Canvas çizimini kullanır. Uygulama bir GLB dosyası olmadan Filament veya SceneView yüklemez.

Bir model entegrasyonu için GLB dosyasını `app/src/main/assets/` köküne koy. Motor bağımlılığını o modelle birlikte ekle. `dev.kolmapper.PadView.ModelFactory` uygulayan sağlayıcıyı `META-INF/services/dev.kolmapper.PadView$ModelFactory` servis kaydıyla tanıt. Sağlayıcı ilgili varlık yolunu alır ve `Renderer` döndürür. Bu bağdaştırıcı gerçek modelin animasyon bilgilerini PadView'dan okuyabilir. Kamera için `pitch()`, `yaw()` ve `zoom()`, giriş için PadView'ın tuş ve eksen durumları kullanılır. Görünmezken çizimi durdur ve ayrılırken motor kaynaklarını bırak.

Model sağlayıcısı bu sürümde yer almıyor. Bir model veya sağlayıcı bulunamazsa katmanlı çizim kullanılır.
