package com.lg.platform.loyalty.domain.exception;

import com.labs.lg.pentagon.common.domain.exception.DomainException;

public class LoyaltyLedgerDomainException extends DomainException {

    public LoyaltyLedgerDomainException(final String message) {
        super(message);
    }

    public LoyaltyLedgerDomainException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
