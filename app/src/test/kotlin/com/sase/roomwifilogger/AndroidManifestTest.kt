package com.sase.roomwifilogger

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidManifestTest {
    @Test
    fun declaresRequiredNetworkAndLocationPermissionsOnly() {
        val permissions = manifestPermissions()

        assertTrue(permissions.contains("android.permission.ACCESS_WIFI_STATE"))
        assertTrue(permissions.contains("android.permission.ACCESS_FINE_LOCATION"))
        assertTrue(permissions.contains("android.permission.ACCESS_NETWORK_STATE"))
        assertFalse(permissions.contains("android.permission.INTERNET"))
        assertFalse(permissions.any { it.contains("STORAGE") })
    }

    private fun manifestPermissions(): Set<String> {
        val manifest = File("src/main/AndroidManifest.xml")
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifest)
        val nodes = document.getElementsByTagName("uses-permission")
        return buildSet {
            for (index in 0 until nodes.length) {
                val node = nodes.item(index)
                add(node.attributes.getNamedItem("android:name").nodeValue)
            }
        }
    }
}
