package com.mochyy.mochymusic.ui.screens.setup.welcome

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mochyy.mochymusic.ui.brand.PippoCharacter
import com.mochyy.mochymusic.ui.brand.PippoPose

@Composable
internal fun WelcomeGirl(modifier: Modifier = Modifier) {
    PippoCharacter(PippoPose.Welcome, modifier)
}
