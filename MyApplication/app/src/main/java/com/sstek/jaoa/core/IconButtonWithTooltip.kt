import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// import com.sstek.jaoa.core.JAOATheme // JAOATheme burada kullanılmadığı için yorum satırına aldım

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IconButtonWithTooltip(
    icon: ImageVector,
    contentDescriptionResId: Int, // Bu String Resource ID'si (R.string.xxx)
    backgroundColor: Color = Color.Transparent,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    val context = LocalContext.current // Toast göstermek için Context gerekiyor

    // String Resource ID'sini metne çeviriyoruz (Tooltip için gereken metin)
    val tooltipText = stringResource(id = contentDescriptionResId)

    Box(
        // IconButton'ın arka plan ve boyut ayarını Box'a taşıdık
        modifier = Modifier
            .background(backgroundColor, shape = RoundedCornerShape(4.dp))
            .size(48.dp)
            // combinedClickable, hem tek basma hem de uzun basma işlevini yönetir.
            .combinedClickable(
                onClick = onClick, // Kısa basma
                onLongClick = {
                    // Uzun basınca Toast mesajını göster
                    Toast.makeText(context, tooltipText, Toast.LENGTH_SHORT).show()
                }
            ),
        contentAlignment = Alignment.Center // Icon'u Box'ın ortasına hizalar
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltipText, // Content Description'ı da çevrilmiş metinle güncelledik
            tint = tint,
            // Standart Icon boyutu genellikle 24.dp'dir.
            modifier = Modifier.size(24.dp)
        )
    }
}