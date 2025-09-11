# jaoa

## Word 

### Yapılanlar
* Kalın, italik, altı çizili, girintileme, metin sollama, ortalama, sağlama yapabiliyor.
* Yazı rengi ayarlanabiliyor.
* Resim ekleniyor, boyutu ayarlanabiliyor.
* Sıralı ve sırasız liste eklenebiliyor.
* Font ayarlama çözüldü. Şu an arial, calibri, times new roman 3 farklı font var. 8pt dan başlayarak
72pt kadar düzenli bir sıra olmadan font boyutu ayarlanabiliyor.
* Açılış ekranından docx dosyaları seçilebiliyor. Android 31 (12) ve 35 (15) te denendi.
* Birlikte açla docx dosyaları açılabiliyor.
* Geri gitme, ileri gitme komutu eklendi.
* Uygulama ana ekranında documents, downloads klasöründeki docx dosyaları listeleniyor.
* Arkaplan rengi seçilebiliyor.
* Farklı dosya tiplerine navigasyon edilebiliyor.
* Docx paylaşılabiliyor.
* Docx yazdırılabiliyor.

### Yapılamayanlar
* Tablo okuma sıkıntılı metinler tablonun dışına taşıyor.
* Büyük dosyalar açılıyor ama düzenlenirken çok kasıyor.
* Sayfa boyutu hesaplama sıkıntılı.

Excel Module Test Senaryoları(libreoffice kullanılarak test edilmiştir.Excel2007-365 (.xlsx) format):
1. Dosya Açma/Kapama
   + Boş Excel dosyası açma
   + Veri içeren Excel dosyası açma 
   (https://ogris.gazi.edu.tr/view/page/249594/kazananlar-ilan-listexlsxasil)
   (https://www.google.com/url?sa=t&source=web&rct=j&opi=89978449&url=https://webupload.gazi.edu.tr/upload/364/2023/9/12/19525c63-7135-4e98-9a97-52abe2f03030-mezuniyet-tezi-tercih-sablonu-2023.xlsx&ved=2ahUKEwie183VqtCPAxWgA9sEHeu3LikQFnoECBcQAQ&usg=AOvVaw3mZOzWLT6nmuD2ejKu0FVr)
2. Dosya Kaydetme
   + Yeni dosya oluşturup kaydetme
   + Mevcut dosyayı üzerine kaydetme
   + Farklı isimle kaydetme
3. Temel Editing
   + Cell'e text yazma
   + Cell'e number yazma
   + Cell'e formula yazma
   + Cell içeriğini silme (formul silme eksiklikler olabilir)
   + Undo/redo işlemleri
4. Font ve Text Formatting
   - Font family değiştirme(kaydetme çalışıyor ama gösterirken doğru çalışmıyor.default font family belirlenip eklenmeli)
   + Font size değiştirme
   - Bold, italic, underline, strikethrough (underline,strikethrough)
   + Font color değiştirme
   + Text alignment (left, center, right)
   + Vertical alignment (top, middle, bottom)
5. Cell Formatting
   + Background color ekleme
   + Border ekleme/kaldırma
   + Border style değiştirme
   + Border color değiştirme
   + Cell merge/unmerge
   + Text wrap açma/kapama(default olarak clip gösteriliyor kaydederken özellikle seçilmezse (libreoffice gibi) overflow olarak kaydediliyor tekrar açıldığında overflow)
6. Number Formatting
   - Para birimleri, yüzde, date, binlik format 
7. Basit Formüller (türkçe desteği yok. lucksheet ile libreoffice farklı formuller olabilir )
   + =A1+B1 gibi basit toplama
   + =SUM(A1:A10) gibi range sum
   + =AVERAGE(A1:A10) ortalama
   + =MAX(A1:A10), =MIN(A1:A10)
   + =COUNT(A1:A10),
10. Multiple Sheets
    + Yeni sheet ekleme 
    + Sheet silme
    + Sheet adını değiştirme
    + Sheet'ler arası geçiş
    ? Sheet kopyalama
    - Sheet renklendirme
11. Row/Column İşlemleri
    ? Column ekleme/silme
    + Row/column genişliği değiştirme
