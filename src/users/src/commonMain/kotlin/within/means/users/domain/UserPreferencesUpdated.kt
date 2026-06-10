package within.means.users.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import within.means.shared.domain.bus.event.DomainEvent

@Serializable
@SerialName(UserPreferencesUpdated.NAME)
data class UserPreferencesUpdated(
    override val eventId: String,
    override val aggregateId: String,
    override val occurredOn: Instant,
    val displayName: String,
    val locale: String,
    val baseCurrency: String,
    val monthlyBudgetCents: Long = 0L,
    val spendingAlertsEnabled: Boolean = true,
) : DomainEvent {
    override val eventName: String = NAME

    companion object {
        const val NAME = "users.preferences_updated"
    }
}
