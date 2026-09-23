package io.github.nku100.webui.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals

class FloatingBottomBarTest {
    @Test
    fun dragStartNearTheLastTabSelectsTheLastTab() {
        assertEquals(
            expected = 3,
            actual = tabIndexAt(
                positionX = 366f,
                totalWidthPx = 400f,
                tabWidthPx = 98f,
                tabsCount = 4,
                horizontalPaddingPx = 4f,
                isLtr = true,
            )
        )
    }
}
