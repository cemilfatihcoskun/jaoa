import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IconButtonWithTooltip(
    icon: ImageVector,
    tooltipTextRes: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .size(48.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    Toast.makeText(context, context.getString(tooltipTextRes), Toast.LENGTH_SHORT).show()
                }
            ),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = context.getString(tooltipTextRes)
        )
    }
}
