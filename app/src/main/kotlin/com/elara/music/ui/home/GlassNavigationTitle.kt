package com.elara.music.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.elara.music.ui.component.GlassSurface
import com.elara.music.ui.component.GlassTextButton
import com.elara.music.ui.theme.BlurRadius

@Composable
fun GlassNavigationTitle(
    title: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    onPlayAllClick: (() -> Unit)? = null,
) {
    GlassSurface(
        blurRadius = BlurRadius.LIGHT.dp,
        alpha = 0.04f,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (label != null) {
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                if (onPlayAllClick != null) {
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    GlassTextButton(
                        text = "Play All",
                        onClick = onPlayAllClick,
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }
        }
    }
}
