package com.lg.platform.loyalty.dataaccess.movement.mapper;

import com.lg.platform.loyalty.dataaccess.movement.entity.MovementJpaEntity;
import com.lg.platform.loyalty.domain.entity.Movement;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import com.lg.platform.loyalty.domain.valueobject.MovementId;
import com.lg.platform.loyalty.domain.valueobject.OrderId;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.time.ZonedDateTime;

/**
 * Round-trip mapper between the immutable {@link Movement} domain
 * aggregate and the mutable {@link MovementJpaEntity}.
 *
 * <p>The {@link Movement} aggregate has no public constructor and no
 * mutators (REQ-013); to rehydrate one from a row we call the private
 * canonical constructor reflectively. This is intentional: the domain
 * factory methods enforce business invariants for newly created
 * movements, but loading a previously persisted row must bypass those
 * factories (the row already passed them at write time).
 */
@Component
public class MovementDataAccessMapper {

    private static final Constructor<Movement> MOVEMENT_CTOR;

    static {
        try {
            MOVEMENT_CTOR = Movement.class.getDeclaredConstructor(
                    MovementId.class,
                    CustomerId.class,
                    int.class,
                    com.lg.platform.loyalty.domain.valueobject.BalanceUpdateCause.class,
                    OrderId.class,
                    java.util.UUID.class,
                    String.class,
                    ZonedDateTime.class,
                    ZonedDateTime.class,
                    int.class);
            MOVEMENT_CTOR.setAccessible(true);
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException(
                    "MovementDataAccessMapper requires the canonical private constructor on Movement", e);
        }
    }

    public MovementJpaEntity movementToEntity(final Movement movement) {
        return MovementJpaEntity.builder()
                .id(movement.getId().getValue())
                .customerId(movement.getCustomerId().getValue())
                .delta(movement.getDelta())
                .cause(movement.getCause())
                .originatingOrderId(movement.getOriginatingOrderId().getValue())
                .originatingEventId(movement.getOriginatingEventId())
                .originatingEventType(movement.getOriginatingEventType())
                .originatingEventReceivedAt(movement.getOriginatingEventReceivedAt())
                .appendedAt(movement.getAppendedAt())
                .version(movement.getVersion())
                .build();
    }

    public Movement entityToMovement(final MovementJpaEntity entity) {
        try {
            return MOVEMENT_CTOR.newInstance(
                    new MovementId(entity.getId()),
                    new CustomerId(entity.getCustomerId()),
                    entity.getDelta(),
                    entity.getCause(),
                    new OrderId(entity.getOriginatingOrderId()),
                    entity.getOriginatingEventId(),
                    entity.getOriginatingEventType(),
                    entity.getOriginatingEventReceivedAt(),
                    entity.getAppendedAt(),
                    entity.getVersion());
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to rehydrate Movement aggregate from MovementJpaEntity (id=" + entity.getId() + ")", e);
        }
    }
}
