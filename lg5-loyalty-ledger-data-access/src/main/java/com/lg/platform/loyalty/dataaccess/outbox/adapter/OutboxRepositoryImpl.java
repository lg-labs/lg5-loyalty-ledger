package com.lg.platform.loyalty.dataaccess.outbox.adapter;

import com.lg.platform.loyalty.application.outbox.model.OutboxMessage;
import com.lg.platform.loyalty.application.ports.output.repository.OutboxRepository;
import com.lg.platform.loyalty.dataaccess.outbox.entity.OutboxJpaEntity;
import com.lg.platform.loyalty.dataaccess.outbox.mapper.OutboxDataAccessMapper;
import com.lg.platform.loyalty.dataaccess.outbox.repository.OutboxJpaRepository;
import com.lg5.spring.outbox.OutboxStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class OutboxRepositoryImpl implements OutboxRepository {

    private final OutboxJpaRepository outboxJpaRepository;
    private final OutboxDataAccessMapper outboxDataAccessMapper;

    public OutboxRepositoryImpl(final OutboxJpaRepository outboxJpaRepository,
                                final OutboxDataAccessMapper outboxDataAccessMapper) {
        this.outboxJpaRepository = outboxJpaRepository;
        this.outboxDataAccessMapper = outboxDataAccessMapper;
    }

    @Override
    public OutboxMessage save(final OutboxMessage outboxMessage) {
        return outboxDataAccessMapper.entityToMessage(
                outboxJpaRepository.save(outboxDataAccessMapper.messageToEntity(outboxMessage)));
    }

    @Override
    public List<OutboxMessage> findAllByStatusOrderByCreatedAtAsc(final OutboxStatus status) {
        return outboxJpaRepository.findAllByOutboxStatusOrderByCreatedAtAsc(status).stream()
                .map(outboxDataAccessMapper::entityToMessage)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAllByStatus(final OutboxStatus status) {
        outboxJpaRepository.deleteAllByOutboxStatus(status);
    }

    @Override
    @Transactional
    public void markCompleted(final UUID id) {
        updateStatus(id, OutboxStatus.COMPLETED);
    }

    @Override
    @Transactional
    public void markFailed(final UUID id) {
        updateStatus(id, OutboxStatus.FAILED);
    }

    private void updateStatus(final UUID id, final OutboxStatus newStatus) {
        final OutboxJpaEntity entity = outboxJpaRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Outbox row not found: id=" + id));
        entity.setOutboxStatus(newStatus);
        entity.setCreatedAt(entity.getCreatedAt() == null ? ZonedDateTime.now() : entity.getCreatedAt());
        outboxJpaRepository.save(entity);
    }
}
