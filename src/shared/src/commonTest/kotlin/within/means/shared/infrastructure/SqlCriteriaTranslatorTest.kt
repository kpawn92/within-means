package within.means.shared.infrastructure

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import within.means.shared.domain.criteria.Criteria
import within.means.shared.domain.criteria.Filter
import within.means.shared.domain.criteria.FilterField
import within.means.shared.domain.criteria.FilterOperator
import within.means.shared.domain.criteria.FilterValue
import within.means.shared.domain.criteria.Filters
import within.means.shared.domain.criteria.Order
import within.means.shared.infrastructure.persistence.SqlCriteriaTranslator
import kotlin.test.Test

class SqlCriteriaTranslatorTest {

    private val allowed = setOf("id", "name", "amount", "occurred_on")

    @Test
    fun `empty criteria produces a bare SELECT`() {
        val sql = SqlCriteriaTranslator.translate(Criteria(Filters.none()), "transactions", allowed)
        sql.sql shouldBe "SELECT * FROM transactions"
        sql.args shouldBe emptyList()
    }

    @Test
    fun `single equals filter is parametrized`() {
        val criteria = Criteria(
            Filters.of(Filter(FilterField("name"), FilterOperator.EQUALS, FilterValue("Alice")))
        )
        val sql = SqlCriteriaTranslator.translate(criteria, "users", allowed)
        sql.sql shouldBe "SELECT * FROM users WHERE name = ?"
        sql.args shouldBe listOf("Alice")
    }

    @Test
    fun `multiple filters are joined with AND in declared order`() {
        val criteria = Criteria(
            Filters.of(
                Filter(FilterField("amount"), FilterOperator.GTE, FilterValue("100")),
                Filter(FilterField("amount"), FilterOperator.LT, FilterValue("500")),
            )
        )
        val sql = SqlCriteriaTranslator.translate(criteria, "transactions", allowed)
        sql.sql shouldBe "SELECT * FROM transactions WHERE amount >= ? AND amount < ?"
        sql.args shouldBe listOf("100", "500")
    }

    @Test
    fun `CONTAINS wraps the value with percent signs for LIKE`() {
        val criteria = Criteria(
            Filters.of(Filter(FilterField("name"), FilterOperator.CONTAINS, FilterValue("foo")))
        )
        val sql = SqlCriteriaTranslator.translate(criteria, "transactions", allowed)
        sql.sql shouldBe "SELECT * FROM transactions WHERE name LIKE ?"
        sql.args shouldBe listOf("%foo%")
    }

    @Test
    fun `order, limit and offset are appended`() {
        val criteria = Criteria(
            filters = Filters.none(),
            order = Order.desc("occurred_on"),
            limit = 50,
            offset = 100,
        )
        val sql = SqlCriteriaTranslator.translate(criteria, "transactions", allowed)
        sql.sql shouldBe "SELECT * FROM transactions ORDER BY occurred_on DESC LIMIT 50 OFFSET 100"
        sql.args shouldBe emptyList()
    }

    @Test
    fun `filter on a disallowed field throws`() {
        val criteria = Criteria(
            Filters.of(Filter(FilterField("password"), FilterOperator.EQUALS, FilterValue("x")))
        )
        shouldThrow<IllegalArgumentException> {
            SqlCriteriaTranslator.translate(criteria, "users", allowed)
        }
    }

    @Test
    fun `order on a disallowed field throws`() {
        val criteria = Criteria(filters = Filters.none(), order = Order.asc("password"))
        shouldThrow<IllegalArgumentException> {
            SqlCriteriaTranslator.translate(criteria, "users", allowed)
        }
    }

    @Test
    fun `invalid table name throws`() {
        shouldThrow<IllegalArgumentException> {
            SqlCriteriaTranslator.translate(Criteria(Filters.none()), "users; DROP TABLE users;--", allowed)
        }
    }
}
