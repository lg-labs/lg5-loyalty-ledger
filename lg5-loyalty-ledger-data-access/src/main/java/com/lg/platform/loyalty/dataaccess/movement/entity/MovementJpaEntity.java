package com.lg.platform.loyalty.dataaccess.movement.entity;

import com.lg.platform.loyalty.domain.valueobject.BalanceUpdateCause;
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
 * JPA mapping of the {@code loyalty.movement} table (data-model.md §Movement).
 *
 * <p>Append-only (REQ-013): no {@code update} / {@code delete} is performed
 * by the repository. The {@code @Version} column is kept (RULE-008) for
 * defensive optimistic-locking semantics even though writes are
 * insert-only — this matches the framework convention that every
 * persistent aggregate carries a JPA version field.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "movement", schema = "loyalty")
@Entity
public class MovementJpaEntity {

    @Id
    private UUID id;

    private UUID customerId;

    private int delta;

    /**
     * Mapped against the Postgres {@code loyalty_cause} ENUM.
     * The driver URL declares {@code stringtype=unspecified}, which lets
     * Postgres cast the {@code String} value Hibernate sends for an
     * {@link EnumType#STRING}-mapped field straight into the ENUM type.
     */
    @Enumerated(EnumType.STRING)
    private BalanceUpdateCause cause;

    private UUID originatingOrderId;

    private UUID originatingEventId;

    private String originatingEventType;

    private ZonedDateTime originatingEventReceivedAt;

    private ZonedDateTime appendedAt;

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
        final MovementJpaEntity that = (MovementJpaEntity) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
