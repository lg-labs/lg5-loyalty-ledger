package com.lg.platform.loyalty.dataaccess.movement.adapter;

import com.lg.platform.loyalty.application.ports.output.repository.MovementLedgerRepository;
import com.lg.platform.loyalty.dataaccess.movement.mapper.MovementDataAccessMapper;
import com.lg.platform.loyalty.dataaccess.movement.repository.MovementJpaRepository;
import com.lg.platform.loyalty.domain.entity.Movement;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import com.lg.platform.loyalty.domain.valueobject.MovementId;
import com.lg.platform.loyalty.domain.valueobject.OrderId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Secondary (data-layer) adapter for the {@link MovementLedgerRepository}
 * output port. Bridges the immutable domain aggregate to the JPA entity.
 */
@Component
public class MovementLedgerRepositoryImpl implements MovementLedgerRepository {

    private final MovementJpaRepository movementJpaRepository;
    private final MovementDataAccessMapper movementDataAccessMapper;

    public MovementLedgerRepositoryImpl(final MovementJpaRepository movementJpaRepository,
                                        final MovementDataAccessMapper movementDataAccessMapper) {
        this.movementJpaRepository = movementJpaRepository;
        this.movementDataAccessMapper = movementDataAccessMapper;
    }

    @Override
    public Movement save(final Movement movement) {
        return movementDataAccessMapper.entityToMovement(
                movementJpaRepository.save(movementDataAccessMapper.movementToEntity(movement)));
    }

    @Override
    public Optional<Movement> findById(final MovementId movementId) {
        return movementJpaRepository.findById(movementId.getValue())
                .map(movementDataAccessMapper::entityToMovement);
    }

    @Override
    public boolean existsCreditFor(final OrderId originatingOrderId) {
        return movementJpaRepository.existsByOriginatingOrderIdAndDeltaGreaterThan(
                originatingOrderId.getValue(), 0);
    }

    @Override
    public int sumPositiveDeltaForOrder(final OrderId originatingOrderId) {
        return movementJpaRepository.sumPositiveDeltaForOrder(originatingOrderId.getValue());
    }

    @Override
    public List<Movement> findPageByCustomer(final CustomerId customerId, final int page, final int size) {
        return movementJpaRepository
                .findByCustomerIdOrderByAppendedAtDescIdDesc(customerId.getValue(), PageRequest.of(page, size))
                .stream()
                .map(movementDataAccessMapper::entityToMovement)
                .toList();
    }

    @Override
    public long countByCustomer(final CustomerId customerId) {
        return movementJpaRepository.countByCustomerId(customerId.getValue());
    }
}
