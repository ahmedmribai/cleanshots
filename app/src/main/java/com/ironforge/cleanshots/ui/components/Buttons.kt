package com.ironforge.cleanshots.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ironforge.cleanshots.ui.theme.BrandGradient
import com.ironforge.cleanshots.ui.theme.OnAccent
import com.ironforge.cleanshots.ui.theme.OnDark
import com.ironforge.cleanshots.ui.theme.Stroke
import com.ironforge.cleanshots.ui.theme.SurfaceHigh

/** Scale applied while an element is pressed, so taps feel physical. */
@Composable
fun pressScale(interaction: MutableInteractionSource, pressed: Float = 0.96f): Float {
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) pressed else 1f,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    return scale
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: ImageVector? = null,
    brush: Brush = BrandGradient,
    contentColor: Color = OnAccent
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction)
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.4f }
            .shadow(if (enabled) 20.dp else 0.dp, shape, ambientColor = Color(0xFF2EE6C5), spotColor = Color(0xFF4F8CFF))
            .clip(shape)
            .background(brush)
            .clickable(interaction, ripple(color = Color.White), enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 17.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            leading?.let {
                Icon(it, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(text, color = contentColor, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    tint: Color = OnDark,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction)
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.4f }
            .clip(shape)
            .background(SurfaceHigh)
            .border(1.dp, Stroke, shape)
            .clickable(interaction, ripple(), enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            leading?.let {
                Icon(it, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = tint, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    brush: Brush? = null,
    tint: Color = OnDark,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction, pressed = 0.88f)
    Box(
        modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.35f }
            .shadow(if (brush != null) 16.dp else 0.dp, CircleShape)
            .clip(CircleShape)
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(SurfaceHigh))
            .border(1.dp, Stroke, CircleShape)
            .clickable(interaction, ripple(), enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.42f))
    }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, brush: Brush? = null, textColor: Color = OnDark, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(SurfaceHigh).border(1.dp, Stroke, shape))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = textColor, style = MaterialTheme.typography.labelSmall)
    }
}
