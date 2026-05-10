package com.lg.platform.loyalty.domain.valueobject;

import com.labs.lg.pentagon.common.domain.valueobject.BaseId;

import java.util.UUID;

public class MovementId extends BaseId<UUID> {

    public MovementId(final UUID value) {
        super(value);
    }

    public static MovementId random() {
        return new MovementId(UUID.randomUUID());
    }
}
