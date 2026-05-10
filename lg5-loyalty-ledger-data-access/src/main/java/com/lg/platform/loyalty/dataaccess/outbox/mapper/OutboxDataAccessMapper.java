package com.lg.platform.loyalty.dataaccess.outbox.mapper;

import com.lg.platform.loyalty.application.outbox.model.OutboxMessage;
import com.lg.platform.loyalty.dataaccess.outbox.entity.OutboxJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OutboxDataAccessMapper {

    public OutboxJpaEntity messageToEntity(final OutboxMessage message) {
        return OutboxJpaEntity.builder()
                .id(message.id())
                .sagaId(message.sagaId())
                .type(message.type())
                .payload(message.payload())
                .outboxStatus(message.status())
                .createdAt(message.createdAt())
                .version(message.version())
                .build();
    }

    public OutboxMessage entityToMessage(final OutboxJpaEntity entity) {
        return new OutboxMessage(
                entity.getId(),
                entity.getSagaId(),
                entity.getType(),
                entity.getPayload(),
                entity.getOutboxStatus(),
                entity.getCreatedAt(),
                entity.getVersion());
    }
}
