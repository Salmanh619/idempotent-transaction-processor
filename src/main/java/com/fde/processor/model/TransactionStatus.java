package com.fde.processor.model;

public enum TransactionStatus {

	RECEIVED,
    PROCESSING,
    PROCESSED,
    DUPLICATE,
    PENDING_OUT_OF_ORDER,
    FAILED_VALIDATION,
    FAILED_EXHAUSTED,
    RETRY_PENDING
}
