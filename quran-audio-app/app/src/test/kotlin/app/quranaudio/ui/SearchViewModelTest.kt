package app.quranaudio.ui

import app.cash.turbine.test
import app.quranaudio.data.TestGraph
import app.quranaudio.ui.search.SearchViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SearchViewModelTest {
    private val g = TestGraph()

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        kotlinx.coroutines.runBlocking { g.catalog.refresh() }
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        g.db.close()
    }

    private suspend fun SearchViewModel.awaitResult(predicate: (app.quranaudio.ui.search.SearchUiState) -> Boolean) =
        state.test(timeout = 10.seconds) {
            var item = awaitItem()
            while (!predicate(item)) item = awaitItem()
            cancelAndIgnoreRemainingEvents()
            item
        }

    @Test fun `finds reciters surahs by name and number and reports empty`() = runTest {
        val vm = SearchViewModel(g.catalog, g.library)
        vm.setQuery("alafasy")
        val r = vm.awaitResult { it.query == "alafasy" && !it.searching && it.reciters.isNotEmpty() }
        assertThat(r.reciters.map { it.id }).containsExactly("mp3quran:123")

        vm.setQuery("36")
        val s = vm.awaitResult { it.query == "36" && !it.searching && it.surahs.isNotEmpty() }
        assertThat(s.surahs.single().nameTransliterated).isEqualTo("Ya-Sin")

        vm.setQuery("zzzzqq")
        val e = vm.awaitResult { it.query == "zzzzqq" && it.isEmptyResult }
        assertThat(e.reciters).isEmpty()
    }
}
