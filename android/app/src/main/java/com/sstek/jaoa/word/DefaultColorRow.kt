import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DefaultColorRow(colors: List<Color>, onColorSelected: (Color) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(colors) { color ->
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onColorSelected(color) },
                color = color,
                shape = androidx.compose.foundation.shape.CircleShape,
                shadowElevation = 4.dp
            ) {}
        }
    }
}

