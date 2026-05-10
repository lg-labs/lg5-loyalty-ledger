package com.lg.platform.loyalty.dataaccess.outbox.entity;

import com.lg5.spring.outbox.OutboxStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
 * JPA mapping of {@code loyalty.outbox} (data-model.md §outbox / RULE-008).
 *
 * <p>Standard framework outbox shape: {@code sagaId} doubles as a
 * correlation id (we set it to the originating event id; ADR-001
 * RULE-007 clarification), {@code payload} is JSON-as-String (the
 * {@code jsonb} cast is handled by the JDBC URL parameter
 * {@code stringtype=unspecified}), and {@code outboxStatus} maps to
 * the Postgres {@code outbox_status} ENUM via
 * {@link EnumType#STRING}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "outbox", schema = "loyalty")
@Entity
public class OutboxJpaEntity {

    @Id
    private UUID id;

    private UUID sagaId;

    private String type;

    private String payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus outboxStatus;

    private ZonedDateTime createdAt;

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
        final OutboxJpaEntity that = (OutboxJpaEntity) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
