package within.means.android.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import within.means.android.ui.components.WmPrimaryButton
import within.means.android.ui.theme.WmTheme

private data class IntroSlide(val art: IntroArt, val title: String, val body: String)

private enum class IntroArt { BALANCE, KEYPAD, BARS }

private val SLIDES = listOf(
    IntroSlide(
        IntroArt.BALANCE,
        "Tu dinero,\nde un vistazo",
        "Registra ingresos y gastos para ver tu disponible y tu ritmo diario sin hojas de cálculo.",
    ),
    IntroSlide(
        IntroArt.KEYPAD,
        "Apunta\nen segundos",
        "Un teclado dedicado y atajos rápidos: añade un movimiento antes de guardar el ticket.",
    ),
    IntroSlide(
        IntroArt.BARS,
        "Entiende\na dónde va",
        "Categorías, periodos y tendencias para que sepas en qué se va realmente cada mes.",
    ),
)

/**
 * The 3-slide intro carousel from the design: illustrative art, a two-line
 * title, body, animated progress dots, and a Saltar / Siguiente / Empezar
 * CTA. Reused by first-run onboarding and the Settings "Ver introducción".
 *
 * [onDone] fires when the user finishes (Empezar) or taps Saltar.
 */
@Composable
fun IntroCarousel(onDone: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { SLIDES.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == SLIDES.lastIndex

    Column(modifier = Modifier.fillMaxSize().padding(28.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                "Saltar",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickableNoRipple(onDone)
                    .padding(8.dp),
            )
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            val slide = SLIDES[page]
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IntroArtwork(slide.art)
                Spacer(Modifier.height(40.dp))
                Text(slide.title, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Text(slide.body, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(SLIDES.size) { i ->
                val selected = i == pagerState.currentPage
                val width by animateDpAsState(if (selected) 22.dp else 8.dp, label = "dotW")
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = width, height = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        ),
                )
            }
        }

        WmPrimaryButton(
            text = if (isLast) "Empezar" else "Siguiente",
            onClick = {
                if (isLast) onDone()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun IntroArtwork(art: IntroArt) {
    Box(
        modifier = Modifier
            .size(width = 220.dp, height = 160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        when (art) {
            IntroArt.BALANCE -> ArtBalance()
            IntroArt.KEYPAD -> ArtKeypad()
            IntroArt.BARS -> ArtBars()
        }
    }
}

@Composable
private fun ArtBalance() {
    val onBrand = MaterialTheme.colorScheme.onPrimaryContainer
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(width = 70.dp, height = 10.dp).clip(RoundedCornerShape(5.dp)).background(onBrand.copy(alpha = 0.4f)))
        Box(Modifier.size(width = 130.dp, height = 22.dp).clip(RoundedCornerShape(6.dp)).background(onBrand))
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(onBrand.copy(alpha = 0.25f))) {
            Box(Modifier.fillMaxWidth(0.62f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(WmTheme.colors.pos))
        }
    }
}

@Composable
private fun ArtKeypad() {
    val onBrand = MaterialTheme.colorScheme.onPrimaryContainer
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(onBrand.copy(alpha = 0.85f)))
                }
            }
        }
    }
}

@Composable
private fun ArtBars() {
    val onBrand = MaterialTheme.colorScheme.onPrimaryContainer
    val heights = listOf(40.dp, 72.dp, 54.dp, 96.dp, 64.dp)
    val colors = listOf(WmTheme.colors.pos, onBrand, WmTheme.colors.savings, onBrand, WmTheme.colors.neg)
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        heights.forEachIndexed { i, h ->
            Box(Modifier.size(width = 18.dp, height = h).clip(RoundedCornerShape(6.dp)).background(colors[i]))
        }
    }
}

/** Lightweight tap target without the ripple plumbing — fine for text actions. */
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = interaction, indication = null, onClick = onClick)
}
