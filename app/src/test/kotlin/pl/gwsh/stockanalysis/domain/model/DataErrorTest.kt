package pl.gwsh.stockanalysis.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class DataErrorTest {

    @Test
    fun `DataErrorException niesie typowany DataError`() {
        val err = DataError.RateLimited
        val ex = DataErrorException(err)

        assertThat(ex.error).isSameInstanceAs(err)
        assertThat(ex.message).contains("RateLimited")
    }

    @Test
    fun `Result failure z DataErrorException jest rozpakowywalny`() {
        val result: Result<Int> = Result.failure(DataErrorException(DataError.NotFound))

        val extracted = result.exceptionOrNull() as? DataErrorException
        assertThat(extracted).isNotNull()
        assertThat(extracted!!.error).isEqualTo(DataError.NotFound)
    }
}
