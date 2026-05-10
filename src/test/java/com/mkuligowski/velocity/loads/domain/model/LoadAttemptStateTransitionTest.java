package com.mkuligowski.velocity.loads.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.mkuligowski.velocity.loads.domain.model.events.LoadAttemptAcceptedEvent;
import com.mkuligowski.velocity.loads.domain.model.events.LoadAttemptDeclinedEvent;
import com.mkuligowski.velocity.loads.domain.policy.DeclineReason;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoadAttemptStateTransitionTest {

    private static final LoadId LOAD_ID = LoadId.of("load-1");
    private static final CustomerId CUSTOMER_ID = CustomerId.of("customer-42");
    private static final Money AMOUNT = Money.parseDollars("$123.45");
    private static final Instant LOAD_TIME = Instant.parse("2018-01-01T00:00:00Z");

    @Test
    void initiate_creates_initiated_state_with_no_events() {
        LoadAttemptInitiated initiated = LoadAttempt.initiate(LOAD_ID, CUSTOMER_ID, AMOUNT, LOAD_TIME);
        assertThat(initiated.events()).isEmpty();
        assertThat(initiated.loadId()).isEqualTo(LOAD_ID);
        assertThat(initiated.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(initiated.amount()).isEqualByComparingTo(AMOUNT);
        assertThat(initiated.loadTime()).isEqualTo(LOAD_TIME);
    }

    @Test
    void accept_returns_accepted_state_with_one_accepted_event() {
        var initiated = LoadAttempt.initiate(LOAD_ID, CUSTOMER_ID, AMOUNT, LOAD_TIME);
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2018-01-01T00:00:01Z");

        var withEvent = initiated.accept(eventId, occurredAt);

        assertThat(withEvent.state()).isInstanceOf(LoadAttemptAccepted.class);
        assertThat(withEvent.event()).isInstanceOf(LoadAttemptAcceptedEvent.class);
        assertThat(withEvent.event().eventId()).isEqualTo(eventId);
        assertThat(withEvent.event().occurredAt()).isEqualTo(occurredAt);
        assertThat(withEvent.state().events()).containsExactly(withEvent.event());
    }

    @Test
    void decline_returns_declined_state_carrying_reason_and_event() {
        var initiated = LoadAttempt.initiate(LOAD_ID, CUSTOMER_ID, AMOUNT, LOAD_TIME);
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2018-01-01T00:00:01Z");

        var withEvent = initiated.decline(DeclineReason.DAILY_AMOUNT, eventId, occurredAt);

        assertThat(withEvent.state()).isInstanceOf(LoadAttemptDeclined.class);
        assertThat(((LoadAttemptDeclined) withEvent.state()).declineReason())
                .isEqualTo(DeclineReason.DAILY_AMOUNT);

        assertThat(withEvent.event()).isInstanceOf(LoadAttemptDeclinedEvent.class);
        assertThat(((LoadAttemptDeclinedEvent) withEvent.event()).declineReason())
                .isEqualTo(DeclineReason.DAILY_AMOUNT);
    }

    @Test
    void fromEvents_rebuilds_accepted_state() {
        var initiated = LoadAttempt.initiate(LOAD_ID, CUSTOMER_ID, AMOUNT, LOAD_TIME);
        var withEvent = initiated.accept(UUID.randomUUID(), Instant.parse("2018-01-01T00:00:01Z"));

        LoadAttempt rehydrated = LoadAttempt.fromEvents(withEvent.state().events());

        assertThat(rehydrated).isInstanceOf(LoadAttemptAccepted.class);
        assertThat(rehydrated.loadId()).isEqualTo(LOAD_ID);
        assertThat(rehydrated.customerId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    void fromEvents_rebuilds_declined_state_with_reason() {
        var initiated = LoadAttempt.initiate(LOAD_ID, CUSTOMER_ID, AMOUNT, LOAD_TIME);
        var withEvent = initiated.decline(
                DeclineReason.WEEKLY_AMOUNT, UUID.randomUUID(), Instant.parse("2018-01-01T00:00:01Z"));

        LoadAttempt rehydrated = LoadAttempt.fromEvents(withEvent.state().events());

        assertThat(rehydrated).isInstanceOf(LoadAttemptDeclined.class);
        assertThat(((LoadAttemptDeclined) rehydrated).declineReason())
                .isEqualTo(DeclineReason.WEEKLY_AMOUNT);
    }
}
