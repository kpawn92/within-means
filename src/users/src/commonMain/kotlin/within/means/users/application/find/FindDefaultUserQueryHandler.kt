package within.means.users.application.find

import within.means.shared.domain.bus.query.QueryHandler
import within.means.users.application.OptionalUserResponse
import within.means.users.application.UserResponse
import within.means.users.domain.UserProfile
import within.means.users.domain.UserProfileRepository
import kotlin.reflect.KClass

class FindDefaultUserQueryHandler(
    private val repository: UserProfileRepository,
) : QueryHandler<FindDefaultUserQuery, OptionalUserResponse> {

    override val queryType: KClass<FindDefaultUserQuery> = FindDefaultUserQuery::class

    override suspend fun handle(query: FindDefaultUserQuery): OptionalUserResponse {
        val profile = repository.searchDefault()
        return OptionalUserResponse(user = profile?.toResponse())
    }

    private fun UserProfile.toResponse(): UserResponse = UserResponse(
        id = id.value,
        displayName = displayName.value,
        locale = locale.code,
        baseCurrency = baseCurrency.code,
        monthlyBudgetCents = monthlyBudgetCents,
        spendingAlertsEnabled = spendingAlertsEnabled,
        monthStartDay = monthStartDay,
        hideAmounts = hideAmounts,
        createdAt = createdAt.toString(),
    )
}
