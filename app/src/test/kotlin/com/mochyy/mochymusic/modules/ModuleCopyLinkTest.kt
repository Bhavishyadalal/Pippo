package com.mochyy.mochymusic.modules

import org.junit.Assert.*
import org.junit.Test

class ModuleCopyLinkTest {
    @Test fun preservesInstalledManifestAndQueryExactly() {
        assertEquals("https://service.example/manifest.json?version=2", moduleCopyLink("custom", " https://service.example/manifest.json?version=2 ", "https://other.example", emptyList(), null))
    }
    @Test fun builtInMonochromeUsesServerBeforeProject() {
        assertEquals("https://stream.example", moduleCopyLink("monochrome", null, null, listOf("https://stream.example"), "https://github.com/owner/repo"))
    }
    @Test fun githubInstallAndWebOriginHaveUsableFallbacks() {
        assertEquals("https://github.com/owner/repo", moduleCopyLink("custom", "https://github.com/owner/repo", null, emptyList(), null))
        assertEquals("https://service.example", moduleCopyLink("custom", null, "https://service.example", emptyList(), null))
    }
    @Test fun excludedPlatformsNeverOfferCopy() {
        for (id in listOf("youtube", "jiosaavn")) assertNull(moduleCopyLink(id, "https://example.com", null, listOf("https://server.example"), "https://project.example"))
    }
    @Test fun invalidAndLocalSourcesAreNotCopied() {
        assertNull(moduleCopyLink("local", "file:///sdcard/mod.zip", "javascript:alert(1)", listOf("https:///no-host", "not a URL"), null))
        assertEquals("https://valid.example", moduleCopyLink("custom", "broken", null, emptyList(), "https://valid.example"))
    }
}
