package app.nouralroh

import app.nouralroh.prayer.PrayerProviderSelector
import app.nouralroh.prayer.ProviderKind
import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerProviderSelectorTest {
    @Test fun tunisiaUsesDedicatedProvider() {
        assertEquals(ProviderKind.TUNISIA, PrayerProviderSelector.select("TN"))
        assertEquals(ProviderKind.TUNISIA, PrayerProviderSelector.select("tn"))
    }

    @Test fun otherCountriesKeepExistingProvider() {
        assertEquals(ProviderKind.EXISTING, PrayerProviderSelector.select("FR"))
        assertEquals(ProviderKind.EXISTING, PrayerProviderSelector.select("DZ"))
    }
}
