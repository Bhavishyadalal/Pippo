package com.mochyy.mochymusic.ui.brand

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.R
import kotlin.random.Random

enum class PippoPose { Welcome, Listening, Thanks }

/** One random identity/variation per process; recomposition and rotation never reroll it. */
object PippoArtwork {
    private val male = Random.nextBoolean()
    private val wave = Random.nextBoolean()

    @DrawableRes
    fun resource(pose: PippoPose): Int = when (pose) {
        PippoPose.Welcome -> if (male) {
            if (wave) R.drawable.pippo_male_wave else R.drawable.pippo_male_welcome
        } else {
            if (wave) R.drawable.pippo_female_wave else R.drawable.pippo_female_welcome
        }
        PippoPose.Listening -> if (male) R.drawable.pippo_male_listening else R.drawable.pippo_female_listening
        PippoPose.Thanks -> if (male) R.drawable.pippo_male_thanks else R.drawable.pippo_female_thanks
    }
}

@Composable
fun PippoCharacter(pose: PippoPose, modifier: Modifier = Modifier) {
    val artwork = remember(pose) { PippoArtwork.resource(pose) }
    AsyncImage(model = artwork, contentDescription = null, contentScale = ContentScale.Fit, modifier = modifier)
}
