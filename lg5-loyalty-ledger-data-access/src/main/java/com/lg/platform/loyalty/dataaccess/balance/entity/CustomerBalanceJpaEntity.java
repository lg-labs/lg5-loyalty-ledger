package com.lg.platform.loyalty.dataaccess.balance.entity;

import jakarta.persistence.Entity;
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
 * JPA mapping of the {@code loyalty.customer_balance} projection
 * (data-model.md §CustomerBalance, ADR-004).
 *
 * <p>The {@code customerId} doubles as the primary key — there is at
 * most one row per customer. Balance is {@code long} (matches the
 * Postgres {@code bigint} column) and may be negative (REQ-007).
 *
 * <p>{@link Version} is required by RULE-008 and is the mechanism that
 * lets two concurrent inbound listener threads detect a lost-update
 * race — the loser receives an
 * {@link org.springframework.dao.OptimisticLockingFailureException}
 * which the listener swallows as a NO-OP (RULE-010).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer_balance", schema = "loyalty")
@Entity
public class CustomerBalanceJpaEntity {

    @Id
    private UUID customerId;

    private long balance;

    private ZonedDateTime lastUpdatedAt;

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
        final CustomerBalanceJpaEntity that = (CustomerBalanceJpaEntity) o;
        return customerId.equals(that.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }
}
