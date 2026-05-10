package com.lg.platform.loyalty.domain.valueobject;

import com.labs.lg.pentagon.common.domain.valueobject.BaseId;

import java.util.UUID;

public class OrderId extends BaseId<UUID> {

    public OrderId(final UUID value) {
        super(value);
    }

    public static OrderId random() {
        return new OrderId(UUID.randomUUID());
    }
}
