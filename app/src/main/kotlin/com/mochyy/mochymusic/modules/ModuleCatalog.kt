// the modules people can find from the modules page, read from modules/catalog.json in mochy's repo so the list grows without an app update

package com.mochyy.mochymusic.modules

import com.mochyy.mochymusic.module.ModuleFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class CatalogModule(
    val name: String,
    val description: String,
    /** the module's link, what "add" checks and installs */
    val url: String,
    val adds: Set<ModuleFeature>,
    val author: String?,
    /** a link to its icon, shown on its row before it's added */
    val icon: String? = null,
)

// No upstream catalogue is contacted. Local and user-supplied modules remain supported.
@Singleton
class ModuleCatalog @Inject constructor() {
    suspend fun load(): Result<List<CatalogModule>> = Result.success(emptyList())
}
