package com.lg.platform.loyalty.domain.valueobject;

import com.labs.lg.pentagon.common.domain.valueobject.BaseId;

import java.util.UUID;

public class CustomerId extends BaseId<UUID> {

    public CustomerId(final UUID value) {
        super(value);
    }

    public static CustomerId random() {
        return new CustomerId(UUID.randomUUID());
    }
}
