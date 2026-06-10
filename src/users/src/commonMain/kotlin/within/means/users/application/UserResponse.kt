package within.means.users.application

import within.means.shared.domain.bus.query.Response

data class UserResponse(
    val id: String,
    val displayName: String,
    val locale: String,
    val baseCurrency: String,
    val monthlyBudgetCents: Long,
    val spendingAlertsEnabled: Boolean,
    val monthStartDay: Int,
    val hideAmounts: Boolean,
    val createdAt: String,
) : Response

data class OptionalUserResponse(val user: UserResponse?) : Response
