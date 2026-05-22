package pl.gwsh.stockanalysis

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * Smoke test Fazy 1 — potwierdza, że JUnit 5 + Truth są poprawnie podpięte.
 * Realne testy domeny pojawiają się od Fazy 2 (mappery) i 4 (strategie).
 */
class SmokeTest {

    @Test
    fun `JUnit 5 i Truth dzialaja`() {
        assertThat(2 + 2).isEqualTo(4)
    }
}
