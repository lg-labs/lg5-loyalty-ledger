package com.lg.platform.loyalty.domain.valueobject;

import com.labs.lg.pentagon.common.domain.valueobject.BaseId;

import java.util.UUID;

public class ProcessedInputEventId extends BaseId<UUID> {

    public ProcessedInputEventId(final UUID value) {
        super(value);
    }

    public static ProcessedInputEventId random() {
        return new ProcessedInputEventId(UUID.randomUUID());
    }
}
