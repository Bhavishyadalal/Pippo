package com.mochyy.mochymusic.ui.player.styles

import com.mochyy.mochymusic.constants.PlayerStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStyleSelectionTest {
    @Test fun `phone selector offers every built in style exactly once and leaves modules conditional`() {
        val offered = PlayerStyle.selectorColumns.flatten()
        assertEquals(PlayerStyle.entries.filter { it != PlayerStyle.MODULE }.toSet(), offered.toSet())
        assertEquals(offered.size, offered.distinct().size)
        assertTrue(PlayerStyle.selectorColumns.all { it.isNotEmpty() })
    }

    @Test fun `existing style positions and saved names survive new additions`() {
        val names = listOf("FLAGSHIP", "CLASSIC", "TURNTABLE", "VIDEO", "IPOD", "POSTER", "TIMELINE", "LYRICS", "BEDSIDE", "BEDSIDE_TILES")
        assertEquals(names, PlayerStyle.entries.take(names.size).map { it.name })
        for (style in PlayerStyle.entries) assertEquals(style, PlayerStyle.valueOf(style.name))
        assertEquals(PlayerStyle.FLAGSHIP, PlayerStyle.fromStored("DEFAULT"))
        assertEquals(PlayerStyle.TIMELINE, PlayerStyle.fromStored("SIMPLE_COMPACT"))
        assertEquals(PlayerStyle.LYRICS, PlayerStyle.fromStored("LYRICS_ABSTRACT"))
    }

    @Test fun `new screens rotate using alternate layouts rather than the flagship editor`() {
        val styles = listOf(PlayerStyle.CASSETTE, PlayerStyle.MIDNIGHT_GLASS, PlayerStyle.PIPPO_DUO, PlayerStyle.CD_WALKMAN)
        styles.forEach {
            assertTrue(it.isPremium)
            assertTrue(it.turnable)
            assertFalse(it.flagshipFamily)
            assertFalse(it.hasFullEditor)
        }
        assertEquals(styles.toSet(), PlayerStyle.entries.filter { it.isPremium }.toSet())
    }

    @Test fun `tv offers the remote friendly new screens and retains its flagship fallback`() {
        val offered = PlayerStyle.selectorColumns.flatten().filter { it.availableOnTv }
        assertTrue(PlayerStyle.FLAGSHIP in offered)
        assertTrue(PlayerStyle.MIDNIGHT_GLASS in offered)
        assertTrue(PlayerStyle.PIPPO_DUO in offered)
        assertFalse(PlayerStyle.CASSETTE in offered)
        assertFalse(PlayerStyle.CD_WALKMAN in offered)
    }
}
