package com.mochyy.mochymusic.ui.screens.profile

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.mochyy.mochymusic.R

internal object ProfileAvatars {
    // The positions also identify saved presets. Append new pictures to preserve those selections.
    val pictures = intArrayOf(
        R.drawable.pippo_avatar_01, R.drawable.pippo_avatar_02, R.drawable.pippo_avatar_03, R.drawable.pippo_avatar_04,
        R.drawable.pippo_avatar_05, R.drawable.pippo_avatar_06, R.drawable.pippo_avatar_07, R.drawable.pippo_avatar_08,
        R.drawable.pippo_avatar_09, R.drawable.pippo_avatar_10, R.drawable.pippo_avatar_11, R.drawable.pippo_avatar_12,
        R.drawable.pippo_avatar_13, R.drawable.pippo_avatar_14, R.drawable.pippo_avatar_15, R.drawable.pippo_avatar_16,
        R.drawable.pippo_avatar_17, R.drawable.pippo_avatar_18, R.drawable.pippo_avatar_19, R.drawable.pippo_avatar_20,
        R.drawable.pippo_avatar_21, R.drawable.pippo_avatar_22, R.drawable.pippo_avatar_23, R.drawable.pippo_avatar_24,
        R.drawable.pippo_avatar_25, R.drawable.pippo_avatar_26, R.drawable.pippo_avatar_27, R.drawable.pippo_avatar_28,
        R.drawable.pippo_avatar_29, R.drawable.pippo_avatar_30, R.drawable.pippo_avatar_31, R.drawable.pippo_avatar_32,
        R.drawable.pippo_avatar_33, R.drawable.pippo_avatar_34, R.drawable.pippo_avatar_35, R.drawable.pippo_avatar_36,
        R.drawable.pippo_avatar_37, R.drawable.pippo_avatar_38, R.drawable.pippo_avatar_39, R.drawable.pippo_avatar_40,
        R.drawable.pippo_avatar_41, R.drawable.pippo_avatar_42, R.drawable.pippo_avatar_43, R.drawable.pippo_avatar_44,
        R.drawable.pippo_avatar_45, R.drawable.pippo_avatar_46, R.drawable.pippo_avatar_47, R.drawable.pippo_avatar_48,
        R.drawable.pippo_avatar_49, R.drawable.pippo_avatar_50,
    )

    fun uri(context: Context, index: Int): Uri =
        Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE).authority(context.packageName)
            .appendPath(pictures[index].toString()).build()

    fun resourceIndex(context: Context, uri: Uri): Int? {
        if (uri.scheme != ContentResolver.SCHEME_ANDROID_RESOURCE || uri.authority != context.packageName) return null
        return uri.lastPathSegment?.toIntOrNull()?.let { pictures.indexOf(it).takeIf { index -> index >= 0 } }
    }

    fun selectedIndex(context: Context, photo: Any?): Int? {
        val uri = when (photo) {
            is Uri -> photo
            is String -> Uri.parse(photo)
            else -> return null
        }
        return resourceIndex(context, uri) ?: uri.lastPathSegment?.let { name ->
            savedPreset.matchEntire(name)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in pictures.indices }
        }
    }

    private val savedPreset = Regex("""profile_pic_avatar_([0-9]+)_.+\.jpg""")
}
