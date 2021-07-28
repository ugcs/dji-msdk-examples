package com.example.ugcssample

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext: Context = InstrumentationRegistry.getInstrumentation().getTargetContext()
        assertEquals("com.example.ugcssample", appContext.packageName)
    }
}