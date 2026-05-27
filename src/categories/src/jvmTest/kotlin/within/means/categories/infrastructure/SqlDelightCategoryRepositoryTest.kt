package within.means.categories.infrastructure

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import within.means.categories.SequentialUuidGenerator
import within.means.categories.db.CategoriesDatabase
import within.means.categories.domain.Category
import within.means.categories.domain.CategoryClassifiers
import within.means.categories.domain.CategoryColor
import within.means.categories.domain.CategoryEssentiality
import within.means.categories.domain.CategoryIcon
import within.means.categories.domain.CategoryId
import within.means.categories.domain.CategoryKind
import within.means.categories.domain.CategoryName
import within.means.categories.domain.CategoryNature
import within.means.categories.domain.EngelGroup
import within.means.categories.infrastructure.persistence.SqlDelightCategoryRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class SqlDelightCategoryRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: CategoriesDatabase
    private lateinit var repo: SqlDelightCategoryRepository

    private val uuids = SequentialUuidGenerator()

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CategoriesDatabase.Schema.create(driver)
        db = CategoriesDatabase(driver)
        repo = SqlDelightCategoryRepository(db, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun expense(name: String, parentId: CategoryId? = null): Category = Category.create(
        id = CategoryId(uuids.next()),
        kind = CategoryKind.EXPENSE,
        name = CategoryName(name),
        color = CategoryColor("#1976D2"),
        icon = CategoryIcon("home"),
        classifiers = CategoryClassifiers(
            nature = CategoryNature.FIXED,
            essentiality = CategoryEssentiality.ESSENTIAL,
            productive = false,
            engelGroup = EngelGroup.HOUSING,
        ),
        parentId = parentId,
        uuids = uuids,
    )

    @Test
    fun `save then findById round-trips a category`() = runTest {
        val c = expense("Alquiler")
        repo.save(c)
        val loaded = repo.search(c.id)!!
        loaded.id shouldBe c.id
        loaded.name shouldBe c.name
        loaded.classifiers shouldBe c.classifiers
    }

    @Test
    fun `all and countAll reflect saved items`() = runTest {
        repo.save(expense("Alquiler"))
        repo.save(expense("Hipoteca"))
        repo.all() shouldHaveSize 2
        repo.countAll() shouldBe 2L
    }

    @Test
    fun `delete removes the row`() = runTest {
        val c = expense("Alquiler")
        repo.save(c)
        repo.delete(c.id)
        repo.search(c.id) shouldBe null
    }
}
