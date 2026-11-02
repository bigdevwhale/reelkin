package app.reelkin.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.reelkin.R
import app.reelkin.data.Genre

/**
 * A small picture palace with the lights down: plum velvet, a warm brass trim, crimson seats and
 * cream ticket paper. Only the booth, the hall, the pet and the posters are pixels; the rest is soft.
 */
@Immutable
object C {
    val bg = Color(0xFF171019)
    val surface = Color(0xFF231A26)
    val surface2 = Color(0xFF2D2231)
    val line = Color(0xFF3E2F42)
    val ink = Color(0xFFF3E9DA)
    val muted = Color(0xFFB7A6AC)
    val faint = Color(0xFF85727C)
    val velvet = Color(0xFFD8435E)
    val velvetSoft = Color(0xFF3F1C29)
    val gold = Color(0xFFE8B850)
    val goldSoft = Color(0xFF3B2E18)
    val onGold = Color(0xFF241608)
    val paper = Color(0xFFF4E6C8)
    val paperD = Color(0xFFE2CFA6)
    val paperInk = Color(0xFF2A1A20)
    val paperMuted = Color(0xFF7C6457)
    val screen = Color(0xFFFFF2C8)

    fun genre(g: Genre) = Color(0xFF000000 or g.color.toLong())
}

/** Prata for titles: the lettering of an old picture house. */
val Prata = FontFamily(Font(R.font.prata, FontWeight.Normal))

/** Narrow capitals, as printed on a ticket. */
val Ticket = FontFamily(Font(R.font.pt_sans_narrow_bold, FontWeight.Bold))

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

object R8 {
    val Xl = RoundedCornerShape(26.dp)
    val Lg = RoundedCornerShape(20.dp)
    val Md = RoundedCornerShape(14.dp)
    val Sm = RoundedCornerShape(10.dp)
}

object T {
    val Display = TextStyle(fontFamily = Prata, fontSize = 30.sp, lineHeight = 36.sp, color = C.ink)
    val Title = TextStyle(fontFamily = Prata, fontSize = 23.sp, lineHeight = 29.sp, color = C.ink)
    val Name = TextStyle(fontFamily = Prata, fontSize = 18.sp, lineHeight = 23.sp, color = C.ink)
    val Eyebrow = TextStyle(fontFamily = Ticket, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.12.em, color = C.faint)
    val Stamp = TextStyle(fontFamily = Ticket, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 0.06.em, color = C.paperInk)
    val Strong = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 15.5.sp, lineHeight = 21.sp, color = C.ink)
    val Body = TextStyle(fontFamily = Manrope, fontSize = 15.sp, lineHeight = 22.sp, color = C.ink)
    val Muted = TextStyle(fontFamily = Manrope, fontSize = 13.5.sp, lineHeight = 19.sp, color = C.muted)
    val Small = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, color = C.faint)
    val Button = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    val Number = TextStyle(fontFamily = Ticket, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = C.gold, fontFeatureSettings = "tnum")
    val Say = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, lineHeight = 20.sp, color = C.paperInk)
}

@Composable
fun ReelkinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.velvet, onPrimary = Color.White, background = C.bg, surface = C.surface, onSurface = C.ink,
            onBackground = C.ink, surfaceContainer = C.surface, surfaceContainerHigh = C.surface2, outline = C.line, error = C.velvet,
        ),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
