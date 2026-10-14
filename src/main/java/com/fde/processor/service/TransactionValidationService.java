package com.fde.processor.service;

import com.fde.processor.model.TransactionRequest;

import java.math.BigDecimal;

public class TransactionValidationService {

    public void validate(TransactionRequest txn) {
        if (txn == null) {
            throw new IllegalArgumentException("Transaction payload cannot be null.");
        }
        if (txn.getTransactionId() == null || txn.getTransactionId().trim().isEmpty()) {
            throw new IllegalArgumentException("transactionId is mandatory.");
        }
        if (txn.getRequestId() == null || txn.getRequestId().trim().isEmpty()) {
            throw new IllegalArgumentException("requestId is mandatory.");
        }
        if (txn.getAccountId() == null || txn.getAccountId().trim().isEmpty()) {
            throw new IllegalArgumentException("accountId is mandatory.");
        }
        if (txn.getTransactionType() == null) {
            throw new IllegalArgumentException("transactionType is mandatory.");
        }
        if (txn.getAmount() == null || txn.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be strictly positive. Value received: " + txn.getAmount());
        }
        if (txn.getCurrency() == null || txn.getCurrency().trim().isEmpty()) {
            throw new IllegalArgumentException("currency code is mandatory.");
        }
        if (txn.getSequenceNumber() <= 0) {
            throw new IllegalArgumentException("sequenceNumber must be greater than zero.");
        }
    }
}