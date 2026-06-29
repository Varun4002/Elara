package com.elara.music.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureInPicture
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elara.music.R
import com.elara.music.ui.component.GlassButton
import com.elara.music.ui.component.GlassSurface
import com.elara.music.ui.theme.BlurRadius

@Composable
fun PlayerTopBar(
    title: String,
    subtitle: String?,
    isCasting: Boolean,
    onBack: () -> Unit,
    onCast: () -> Unit,
    onPiP: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        blurRadius = BlurRadius.LIGHT.dp,
        alpha = 0.10f,
        borderAlpha = 0.06f,
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GlassButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onBack,
                size = 40.dp,
                iconSize = 22.dp,
            )

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (title.isEmpty()) {
                    Image(
                        painter = painterResource(R.drawable.app_icon),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp)),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = title.ifEmpty { "Elara" },
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (isCasting) {
                GlassButton(
                    icon = Icons.Rounded.Cast,
                    onClick = onCast,
                    size = 40.dp,
                    iconSize = 22.dp,
                    glowColor = Color(0xFFED5564),
                )
            }

            GlassButton(
                icon = Icons.Rounded.PictureInPicture,
                onClick = onPiP,
                size = 40.dp,
                iconSize = 22.dp,
            )

            GlassButton(
                icon = Icons.Rounded.MoreVert,
                onClick = onMenu,
                size = 40.dp,
                iconSize = 22.dp,
            )
        }
    }
}
