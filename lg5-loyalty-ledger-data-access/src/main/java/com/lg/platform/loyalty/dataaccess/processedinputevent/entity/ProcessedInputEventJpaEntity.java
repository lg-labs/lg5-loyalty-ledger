package com.lg.platform.loyalty.dataaccess.processedinputevent.entity;

import com.lg.platform.loyalty.domain.valueobject.ProcessedInputEventOutcome;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA mapping of {@code loyalty.processed_input_event} (data-model.md
 * §processed_input_event, ADR-003).
 *
 * <p>The {@code @UniqueConstraint} declared here mirrors the
 * Liquibase-created {@code uq_processed_event_type_id} unique index —
 * declaring it in JPA metadata makes the hibernate-validate
 * {@code ddl-auto=validate} phase explicit about its existence (and
 * documents the dedup contract to readers of the entity).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "processed_input_event",
        schema = "loyalty",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_processed_event_type_id",
                columnNames = {"originating_event_type", "originating_event_id"}))
@Entity
public class ProcessedInputEventJpaEntity {

    @Id
    private UUID id;

    private UUID originatingEventId;

    private String originatingEventType;

    private UUID originatingOrderId;

    private UUID originatingCustomerId;

    private ZonedDateTime receivedAt;

    @Enumerated(EnumType.STRING)
    private ProcessedInputEventOutcome outcome;

    /** Nullable: only set when {@code outcome == MOVEMENT_APPENDED}. */
    private UUID movementId;

    @Version
    private int version;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ProcessedInputEventJpaEntity that = (ProcessedInputEventJpaEntity) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
