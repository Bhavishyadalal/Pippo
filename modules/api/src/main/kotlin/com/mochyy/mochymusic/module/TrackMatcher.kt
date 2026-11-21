package com.mochyy.mochymusic.module

import kotlin.math.abs

/** A song in a service's catalog, as its search returns it. */
class CatalogTrack(
    val id: String,
    val title: String,
    /** every credited artist, as one line ("benny blanco, Halsey & Khalid") */
    val artist: String,
    val album: String? = null,
    val durationMs: Long? = null,
    val isrc: String? = null,
)

/**
 * Finds the catalog song that is the same recording as the one Mochy is playing, for any
 * module. Shared so every source matches alike. Titles are
 * compared without the decorations sources add ("feat.", "Remastered", "Official Video"), but the
 * words that make it a different recording (remix, live, acoustic, sped up...) count against it.
 */
object TrackMatcher {

    private val versionPatterns = listOf(
        "remix", "live", "acoustic|unplugged", "instrumental", "karaoke",
        "sped[ -]up|speed[ -]up|nightcore", "slowed|slow[ -]down", "demo", "mash[ -]?up",
        "(?:radio|extended) edit", "cover", "orchestral",
    ).map { Regex("""\b(?:$it)\b""", RegexOption.IGNORE_CASE) }
    private val previewPattern = Regex("""(?:^preview$|[(\[]\s*(?:song |track |audio )?preview\b|\s[-–]\s*preview\b)""", RegexOption.IGNORE_CASE)

    private val contentVersion = Regex("""(?:[(\[]|\s[-–]\s*)(explicit|clean)\b""", RegexOption.IGNORE_CASE)

    /** Reject clear mismatches while accepting providers that omit optional catalog fields. */
    fun isCompatible(query: TrackQuery, title: String, durationMs: Long?): Boolean {
        if (previewPattern.containsMatchIn(title) && !previewPattern.containsMatchIn(query.title)) return false

        // A word in the song name itself is harmless when both titles contain it.
        // Use title markers for the version check: catalog album names vary by service.
        if (title.isNotBlank() && versionPatterns.any {
                it.containsMatchIn(title) != it.containsMatchIn(query.title)
            }
        ) return false

        val wantedVersion = contentVersion.find(query.title)?.groupValues?.get(1)
        val actualVersion = contentVersion.find(title)?.groupValues?.get(1)
        if (wantedVersion != null && actualVersion != null && !wantedVersion.equals(actualVersion, ignoreCase = true)) return false
        return isDurationCompatible(query.durationMs, durationMs)
    }

    fun isDurationCompatible(expectedMs: Long?, durationMs: Long?): Boolean {
        val wanted = expectedMs?.takeIf { it > 0 } ?: return true
        val actual = durationMs?.takeIf { it > 0 } ?: return true
        // Music videos sometimes have an intro/outro. Allow moderate drift on long tracks
        // while rejecting catalog entries that are a small fraction of the original.
        val tolerance = (wanted / 5).coerceIn(5_000L, 45_000L)
        return abs(wanted - actual) <= tolerance
    }

    class Scored(val track: CatalogTrack, val score: Float)

    fun best(query: TrackQuery, candidates: List<CatalogTrack>): Scored? = ranked(query, candidates).firstOrNull()

    /**
     * Every acceptable match, best first. The same recording often sits in the catalog several
     * times (the single, the album, a deluxe, a compilation), and they differ in which streams they
     * carry, so the caller tries the near-equal ones in turn.
     */
    fun ranked(query: TrackQuery, candidates: List<CatalogTrack>): List<Scored> {
        val isrc = query.isrc
        val scored = candidates.map { song ->
            val score = when {
                !isrc.isNullOrBlank() && song.isrc.equals(isrc, ignoreCase = true) ->
                    if (isCompatible(query, song.title, song.durationMs)) 1f else 0f
                else -> score(query, song)
            }
            Scored(song, score)
        }
        return scored.filter { it.score >= 0.7f }.sortedByDescending { it.score }
    }

    fun score(query: TrackQuery, song: CatalogTrack): Float {
        if (!isCompatible(query, song.title, song.durationMs)) return 0f
        val qTitle = core(query.title)
        val sTitle = core(song.title)
        val title = similarity(qTitle, sTitle)
        if (title < 0.55f) return 0f

        val songArtists = words(song.artist)
        val artist = query.artists.maxOfOrNull { a ->
            val w = words(a)
            if (w.isEmpty()) 0f else w.count { it in songArtists }.toFloat() / w.size
        } ?: 0f
        if (artist < 0.5f) return 0f

        val wantMs = query.durationMs
        val haveMs = song.durationMs
        val duration = if (wantMs != null && haveMs != null) {
            val diff = abs(wantMs - haveMs) / 1000f
            when {
                diff <= 2f -> 1f
                diff >= 15f -> 0f
                else -> 1f - (diff - 2f) / 13f
            }
        } else 0.6f

        return (0.5f * title + 0.3f * artist + 0.2f * duration).coerceIn(0f, 1f)
    }

    /** the title without what sources hang on it */
    fun core(title: String): String {
        var t = title.lowercase()
        t = t.replace(Regex("""[(\[](?:feat|ft|with|prod)\.?\s[^)\]]*[)\]]"""), " ")
        t = t.replace(Regex("""\s[-–]\s.*\b(?:remaster(?:ed)?|official|video|audio|lyrics?|visuali[sz]er|mv)\b.*$"""), " ")
        t = t.replace(Regex("""[(\[][^)\]]*\b(?:remaster(?:ed)?|official|video|audio|lyrics?|visuali[sz]er|mv|explicit|clean)\b[^)\]]*[)\]]"""), " ")
        t = t.replace(Regex("""\b(?:feat|ft)\.?\s.*$"""), " ")
        return words(t).joinToString(" ")
    }

    private fun words(text: String): List<String> =
        text.lowercase().split(Regex("""[^\p{L}\p{N}]+""")).filter { it.isNotEmpty() && it != "the" && it != "and" }

    private fun similarity(a: String, b: String): Float {
        if (a == b) return 1f
        val wa = a.split(' ').filter { it.isNotEmpty() }.toSet()
        val wb = b.split(' ').filter { it.isNotEmpty() }.toSet()
        if (wa.isEmpty() || wb.isEmpty()) return 0f
        return (wa intersect wb).size.toFloat() / (wa union wb).size
    }
}
