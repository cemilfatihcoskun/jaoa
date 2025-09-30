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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sstek.jaoa.core.JAOATheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IconButtonWithTooltip(
    icon: ImageVector,
    contentDescriptionResId: Int,
    backgroundColor: Color = Color.Transparent,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        // Modifier ile arka plan ve boyut ayarı
        modifier = Modifier
            .background(backgroundColor, shape = RoundedCornerShape(4.dp))
            .size(48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(id = contentDescriptionResId),
            tint = tint
        )
    }
}
