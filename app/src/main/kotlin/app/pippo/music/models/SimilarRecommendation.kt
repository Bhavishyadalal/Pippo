// SimilarRecommendation.kt

package app.pippo.music.models

import com.music.innertube.models.YTItem
import app.pippo.music.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
