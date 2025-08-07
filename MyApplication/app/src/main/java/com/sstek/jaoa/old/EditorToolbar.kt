import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatIndentDecrease
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sstek.jaoa.old.ColorPicker
import com.sstek.jaoa.old.FontPicker
import jp.wasabeef.richeditor.RichEditor
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Base64

fun Uri.toBase64(context: Context): String? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(this)
        val outputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var len: Int

        while (inputStream?.read(buffer).also { len = it ?: -1 } != -1) {
            outputStream.write(buffer, 0, len)
        }

        val bytes = outputStream.toByteArray()
        Base64.getEncoder().encodeToString(bytes)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun EditorToolbar(
    editorRef: RichEditor?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val showTextColorPicker = remember { mutableStateOf(false) }
    val showBackgroundColorPicker = remember { mutableStateOf(false) }
    val showFontPicker = remember { mutableStateOf(false) }

    val context = LocalContext.current


    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            editorRef?.insertImage(uri.toString(), "resim")
        }
    }



    val colors = listOf(
        0xFF000000.toInt(), // Siyah
        0xFFFF0000.toInt(), // Kırmızı
        0xFF00FF00.toInt(), // Yeşil
        0xFF0000FF.toInt(), // Mavi
        0xFFFFFF00.toInt(), // Sarı
        0xFFFFA500.toInt(), // Turuncu
        0xFF800080.toInt(), // Mor
        0xFF808080.toInt(), // Gri
        0xFFFFFFFF.toInt()  // Beyaz
    )

    val fonts = listOf("Arial", "Calibri", "Times New Roman", "Courier New", "Georgia", "Verdana")
    val fontSizes = (8..36 step 2).toList()

    var selectedFont by remember { mutableStateOf(fonts.first()) }
    var selectedFontSize by remember { mutableStateOf(16) }

    if (showFontPicker.value) {
        FontPicker(
            fonts = fonts,
            fontSizes = fontSizes,
            selectedFont = selectedFont
            ,
            selectedFontSize = selectedFontSize,
            onFontSelected = {
                selectedFont = it
                //editorRef
            },
            onFontSizeSelected = {
                selectedFontSize = it
                Log.d("EditorToolbar", "$it")
                editorRef?.setFontSize(it)
            },
            onDismiss = { showFontPicker.value = false }
        )
    }


    if (showTextColorPicker.value) {
        ColorPicker(colors) { color ->
            editorRef?.setTextColor(color)
            showTextColorPicker.value = false
        }
    }

    if (showBackgroundColorPicker.value) {
        ColorPicker(colors) { color ->
            editorRef?.setTextBackgroundColor(color)

            //editorRef?.("javascript:RE.setTextBackgroundColor('$color')")

            showBackgroundColorPicker.value = false
        }
    }


    Row(
        modifier = Modifier
            .padding(8.dp)
            .horizontalScroll(scrollState)
    ) {
        IconButton(onClick = { editorRef?.undo() }) {
            Icon(Icons.Filled.Undo, contentDescription = "Geri")
        }
        IconButton(onClick = { editorRef?.redo() }) {
            Icon(Icons.Filled.Redo, contentDescription = "İleri")
        }

        IconButton(onClick = { editorRef?.setBold() }) {
            Icon(Icons.Filled.FormatBold, contentDescription = "Kalın")
        }
        IconButton(onClick = { editorRef?.setItalic() }) {
            Icon(Icons.Filled.FormatItalic, contentDescription = "İtalik")
        }
        IconButton(onClick = { editorRef?.setUnderline() }) {
            Icon(Icons.Filled.FormatUnderlined, contentDescription = "Altı Çizili")
        }
        IconButton(onClick = { showFontPicker.value = !showFontPicker.value }) {
            Icon(Icons.Filled.FontDownload, contentDescription = "Yazı tipi ve boyutu")
        }
        IconButton(onClick = {
            showBackgroundColorPicker.value = !showBackgroundColorPicker.value
            showTextColorPicker.value = false
        }) {
            Icon(Icons.Filled.FormatColorFill, contentDescription = "Arkaplan rengi")
        }
        IconButton(onClick = {
            showTextColorPicker.value = !showTextColorPicker.value
            showBackgroundColorPicker.value = false
        }) {
            Icon(Icons.Filled.FormatColorText, contentDescription = "Yazı rengi")
        }


        IconButton(onClick = {
            editorRef?.setBullets()
        }) {
            Icon(Icons.Filled.FormatListBulleted, contentDescription = "Sırasız liste")
        }
        IconButton(onClick = {
            editorRef?.setNumbers()
        }) {
            Icon(Icons.Filled.FormatListNumbered, contentDescription = "Sıralı liste")
        }
        IconButton(onClick = {
            galleryLauncher.launch("image/*") // sadece resim dosyaları
        }) {
            Icon(Icons.Filled.Image, contentDescription = "Resim ekleme")
        }
        IconButton(onClick = {

        }) {
            Icon(Icons.Filled.Camera, contentDescription = "Fotoğraf çekme")
        }
        IconButton(onClick = {  }) {
            Icon(Icons.Filled.TableRows, contentDescription = "Tablo ekleme")
        }


        IconButton(onClick = { editorRef?.setAlignLeft() }) {
            Icon(Icons.Filled.FormatAlignLeft, contentDescription = "Sola hizalama")
        }
        IconButton(onClick = { editorRef?.setAlignCenter() }) {
            Icon(Icons.Filled.FormatAlignCenter, contentDescription = "Ortaya hizalama")
        }
        IconButton(onClick = { editorRef?.setAlignRight() }) {
            Icon(Icons.Filled.FormatAlignRight, contentDescription = "Sağa hizalama")
        }
        IconButton(onClick = {

        }) {
            Icon(Icons.Filled.FormatAlignJustify, contentDescription = "Dengeleme")
        }


        IconButton(onClick = { editorRef?.zoomOut() }) {
            Icon(Icons.Filled.ZoomOut, contentDescription = "Uzaklaştır")
        }

        IconButton(onClick = { editorRef?.zoomIn() }) {
            Icon(Icons.Filled.ZoomIn, contentDescription = "Yakınlaştır")
        }

        IconButton(onClick = { editorRef?.setOutdent() }) {
            Icon(Icons.Filled.FormatIndentDecrease, "Girinti kaldır")
        }

        IconButton(onClick = { editorRef?.setIndent() }) {
            Icon(Icons.Filled.FormatIndentIncrease, "Girintile")
        }

    }
}

