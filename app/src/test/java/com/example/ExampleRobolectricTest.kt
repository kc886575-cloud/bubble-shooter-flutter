package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.LevelData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Bubble Shooter", appName)
  }

  @Test
  fun `verify ten levels plus boss level exist`() {
    val levels = LevelData.LEVELS
    assertEquals(11, levels.size)
    val bossLevel = levels.firstOrNull { it.isBoss }
    assertNotNull(bossLevel)
    assertEquals(11, bossLevel?.levelNumber)
  }
}
