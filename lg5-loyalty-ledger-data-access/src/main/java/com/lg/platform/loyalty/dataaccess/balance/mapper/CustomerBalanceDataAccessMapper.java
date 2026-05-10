package com.lg.platform.loyalty.dataaccess.balance.mapper;

import com.lg.platform.loyalty.dataaccess.balance.entity.CustomerBalanceJpaEntity;
import com.lg.platform.loyalty.domain.entity.CustomerBalance;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.time.ZonedDateTime;

/**
 * Round-trip mapper between {@link CustomerBalance} (mutable domain
 * aggregate) and {@link CustomerBalanceJpaEntity}.
 *
 * <p>The aggregate has a private canonical constructor; we invoke it
 * reflectively to rehydrate state from a persisted row, mirroring
 * the strategy used in {@code MovementDataAccessMapper}.
 */
@Component
public class CustomerBalanceDataAccessMapper {

    private static final Constructor<CustomerBalance> BALANCE_CTOR;

    static {
        try {
            BALANCE_CTOR = CustomerBalance.class.getDeclaredConstructor(
                    CustomerId.class, long.class, ZonedDateTime.class, int.class);
            BALANCE_CTOR.setAccessible(true);
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException(
                    "CustomerBalanceDataAccessMapper requires the canonical private constructor on CustomerBalance",
                    e);
        }
    }

    public CustomerBalanceJpaEntity domainToEntity(final CustomerBalance domain) {
        return CustomerBalanceJpaEntity.builder()
                .customerId(domain.getId().getValue())
                .balance(domain.getBalance())
                .lastUpdatedAt(domain.getLastUpdatedAt())
                .version(domain.getVersion())
                .build();
    }

    public CustomerBalance entityToDomain(final CustomerBalanceJpaEntity entity) {
        try {
            return BALANCE_CTOR.newInstance(
                    new CustomerId(entity.getCustomerId()),
                    entity.getBalance(),
                    entity.getLastUpdatedAt(),
                    entity.getVersion());
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to rehydrate CustomerBalance from CustomerBalanceJpaEntity (customerId="
                            + entity.getCustomerId() + ")", e);
        }
    }
}
