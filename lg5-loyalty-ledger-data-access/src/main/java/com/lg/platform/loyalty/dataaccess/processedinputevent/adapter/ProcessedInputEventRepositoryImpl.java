package com.lg.platform.loyalty.dataaccess.processedinputevent.adapter;

import com.lg.platform.loyalty.application.ports.output.repository.ProcessedInputEventRepository;
import com.lg.platform.loyalty.dataaccess.processedinputevent.mapper.ProcessedInputEventDataAccessMapper;
import com.lg.platform.loyalty.dataaccess.processedinputevent.repository.ProcessedInputEventJpaRepository;
import com.lg.platform.loyalty.domain.entity.ProcessedInputEvent;
import com.lg.platform.loyalty.domain.valueobject.ProcessedInputEventId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProcessedInputEventRepositoryImpl implements ProcessedInputEventRepository {

    private final ProcessedInputEventJpaRepository processedInputEventJpaRepository;
    private final ProcessedInputEventDataAccessMapper processedInputEventDataAccessMapper;

    public ProcessedInputEventRepositoryImpl(
            final ProcessedInputEventJpaRepository processedInputEventJpaRepository,
            final ProcessedInputEventDataAccessMapper processedInputEventDataAccessMapper) {
        this.processedInputEventJpaRepository = processedInputEventJpaRepository;
        this.processedInputEventDataAccessMapper = processedInputEventDataAccessMapper;
    }

    @Override
    public ProcessedInputEvent save(final ProcessedInputEvent processedInputEvent) {
        return processedInputEventDataAccessMapper.entityToDomain(
                processedInputEventJpaRepository.save(
                        processedInputEventDataAccessMapper.domainToEntity(processedInputEvent)));
    }

    @Override
    public Optional<ProcessedInputEvent> findById(final ProcessedInputEventId id) {
        return processedInputEventJpaRepository.findById(id.getValue())
                .map(processedInputEventDataAccessMapper::entityToDomain);
    }
}
