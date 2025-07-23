import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import jp.wasabeef.richeditor.RichEditor

@Composable
fun EditorToolbar(editorRef: RichEditor?) {
    val scrollState = rememberScrollState()


    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            editorRef?.insertImage(uri.toString(), "")
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
        IconButton(onClick = { editorRef?.setTextBackgroundColor(0xFFFF00) }) {
            Icon(Icons.Filled.FormatColorFill, contentDescription = "Arkaplan rengi")
        }
        IconButton(onClick = { editorRef?.setTextColor(0xFF0000) }) {
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
        IconButton(onClick = {  }) {
            Icon(Icons.Filled.FormatAlignJustify, contentDescription = "Dengeleme")
        }




    }
}
