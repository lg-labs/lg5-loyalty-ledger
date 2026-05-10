package com.lg.platform.loyalty.dataaccess.processedinputevent.mapper;

import com.lg.platform.loyalty.dataaccess.processedinputevent.entity.ProcessedInputEventJpaEntity;
import com.lg.platform.loyalty.domain.entity.ProcessedInputEvent;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import com.lg.platform.loyalty.domain.valueobject.MovementId;
import com.lg.platform.loyalty.domain.valueobject.OrderId;
import com.lg.platform.loyalty.domain.valueobject.ProcessedInputEventId;
import com.lg.platform.loyalty.domain.valueobject.ProcessedInputEventOutcome;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.time.ZonedDateTime;
import java.util.UUID;

@Component
public class ProcessedInputEventDataAccessMapper {

    private static final Constructor<ProcessedInputEvent> PIE_CTOR;

    static {
        try {
            PIE_CTOR = ProcessedInputEvent.class.getDeclaredConstructor(
                    ProcessedInputEventId.class, UUID.class, String.class, OrderId.class,
                    CustomerId.class, ZonedDateTime.class, ProcessedInputEventOutcome.class,
                    MovementId.class, int.class);
            PIE_CTOR.setAccessible(true);
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException(
                    "ProcessedInputEventDataAccessMapper requires the canonical private constructor", e);
        }
    }

    public ProcessedInputEventJpaEntity domainToEntity(final ProcessedInputEvent domain) {
        return ProcessedInputEventJpaEntity.builder()
                .id(domain.getId().getValue())
                .originatingEventId(domain.getOriginatingEventId())
                .originatingEventType(domain.getOriginatingEventType())
                .originatingOrderId(domain.getOriginatingOrderId().getValue())
                .originatingCustomerId(domain.getOriginatingCustomerId().getValue())
                .receivedAt(domain.getReceivedAt())
                .outcome(domain.getOutcome())
                .movementId(domain.getMovementId() == null ? null : domain.getMovementId().getValue())
                .version(domain.getVersion())
                .build();
    }

    public ProcessedInputEvent entityToDomain(final ProcessedInputEventJpaEntity entity) {
        try {
            return PIE_CTOR.newInstance(
                    new ProcessedInputEventId(entity.getId()),
                    entity.getOriginatingEventId(),
                    entity.getOriginatingEventType(),
                    new OrderId(entity.getOriginatingOrderId()),
                    new CustomerId(entity.getOriginatingCustomerId()),
                    entity.getReceivedAt(),
                    entity.getOutcome(),
                    entity.getMovementId() == null ? null : new MovementId(entity.getMovementId()),
                    entity.getVersion());
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to rehydrate ProcessedInputEvent (id=" + entity.getId() + ")", e);
        }
    }
}
