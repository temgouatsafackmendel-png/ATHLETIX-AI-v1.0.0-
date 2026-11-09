package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SeedCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `verify app_name and seed catalog integrity`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Athletix AI", appName)

        // Verify all 10 muscle categories and Cameroonian food items are present in SeedCatalog
        assertTrue(SeedCatalog.initialExercises.size >= 10)
        assertTrue(SeedCatalog.initialPrograms.size >= 9)
        assertTrue(SeedCatalog.initialFoods.any { it.originRegion == "Cameroun" && it.name.contains("Ndolé") })
        assertTrue(SeedCatalog.initialFoods.any { it.originRegion == "Cameroun" && it.name.contains("Poulet DG") })
    }
}
