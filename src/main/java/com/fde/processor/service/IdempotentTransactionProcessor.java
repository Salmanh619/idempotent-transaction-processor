package com.fde.processor.service;

import com.fde.processor.model.*;
import com.fde.processor.simulator.TransientFailureSimulator;
import com.fde.processor.store.TransactionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class IdempotentTransactionProcessor {
    private static final Logger log = LoggerFactory.getLogger(IdempotentTransactionProcessor.class);
    private static final int MAX_RETRIES = 2;

    private final TransactionStore store;
    private final AccountLedgerService ledgerService;
    private final TransactionValidationService validator;
    private final TransientFailureSimulator failureSimulator;
    private final ProcessingSummary summary = new ProcessingSummary();

    public IdempotentTransactionProcessor(TransactionStore store,
                                         AccountLedgerService ledgerService,
                                         TransactionValidationService validator,
                                         TransientFailureSimulator failureSimulator) {
        this.store = store;
        this.ledgerService = ledgerService;
        this.validator = validator;
        this.failureSimulator = failureSimulator;
    }

    public ProcessingSummary getSummary() {
        return summary;
    }

    public void process(TransactionRequest txn) {
        log.info("Processing request: {}", txn);

        // 1. Validation
        try {
            validator.validate(txn);
        } catch (IllegalArgumentException e) {
            log.warn("Validation failed for {}: {}", txn.getTransactionId(), e.getMessage());
            txn.setStatus(TransactionStatus.FAILED_VALIDATION);
            txn.setFailureReason(e.getMessage());
            summary.recordStatus(TransactionStatus.FAILED_VALIDATION);
            return;
        }

        // 2. Idempotency & Duplicate Check
        if (store.isProcessedOrDuplicate(txn.getTransactionId(), txn.getRequestId())) {
            log.warn("DUPLICATE detected. Skipping side-effects for txn: {}, req: {}", 
                    txn.getTransactionId(), txn.getRequestId());
            txn.setStatus(TransactionStatus.DUPLICATE);
            summary.recordStatus(TransactionStatus.DUPLICATE);
            return;
        }

        // 3. Ordering Check (Option A: Sequence-Based Processing)
        long lastSeq = store.getLastProcessedSequence(txn.getAccountId());
        long expectedSeq = lastSeq + 1;

        if (txn.getSequenceNumber() > expectedSeq) {
            log.info("Out-of-order transaction detected for account {}. Expected: {}, Received: {}. Buffering.",
                    txn.getAccountId(), expectedSeq, txn.getSequenceNumber());
            txn.setStatus(TransactionStatus.PENDING_OUT_OF_ORDER);
            store.bufferPending(txn);
            summary.recordStatus(TransactionStatus.PENDING_OUT_OF_ORDER);
            return;
        } else if (txn.getSequenceNumber() <= lastSeq) {
            log.warn("Stale sequence number {} received for account {} (last processed: {}). Marking DUPLICATE.",
                    txn.getSequenceNumber(), txn.getAccountId(), lastSeq);
            txn.setStatus(TransactionStatus.DUPLICATE);
            summary.recordStatus(TransactionStatus.DUPLICATE);
            return;
        }

        // 4. Execution with Retry Mechanism
        executeWithRetry(txn);

        // 5. Unbuffer Subsequent In-Order Transactions for this Account
        drainPendingTransactions(txn.getAccountId());
    }

    private void executeWithRetry(TransactionRequest txn) {
        boolean executed = false;
        while (!executed && txn.getRetryCount() <= MAX_RETRIES) {
            try {
                txn.setStatus(TransactionStatus.PROCESSING);
                
                // Simulate transient failures if targeted
                failureSimulator.simulate(txn.getTransactionId());

                // Apply atomic balance change & mark idempotency
                ledgerService.applyTransaction(txn);
                store.recordProcessed(txn);
                store.setLastProcessedSequence(txn.getAccountId(), txn.getSequenceNumber());
                
                txn.setStatus(TransactionStatus.PROCESSED);
                summary.recordStatus(TransactionStatus.PROCESSED);
                log.info("Successfully executed: {}", txn.getTransactionId());
                executed = true;

            } catch (Exception ex) {
                txn.incrementRetryCount();
                log.warn("Transient error on txn {} (attempt {}): {}", 
                        txn.getTransactionId(), txn.getRetryCount(), ex.getMessage());

                if (txn.getRetryCount() > MAX_RETRIES) {
                    txn.setStatus(TransactionStatus.FAILED_EXHAUSTED);
                    txn.setFailureReason("Retry limit exceeded: " + ex.getMessage());
                    summary.recordStatus(TransactionStatus.FAILED_EXHAUSTED);
                    log.error("Transaction {} exhausted all retries.", txn.getTransactionId());
                    break;
                } else {
                    txn.setStatus(TransactionStatus.RETRY_PENDING);
                }
            }
        }
    }

    private void drainPendingTransactions(String accountId) {
        List<TransactionRequest> pendingList = store.getPendingForAccount(accountId);
        for (TransactionRequest pending : pendingList) {
            long currentSeq = store.getLastProcessedSequence(accountId);
            if (pending.getSequenceNumber() == currentSeq + 1) {
                log.info("Draining buffered transaction: {} (Seq: {})", 
                        pending.getTransactionId(), pending.getSequenceNumber());
                store.removePending(accountId, pending);
                executeWithRetry(pending);
            } else {
                break; // Gap still exists
            }
        }
    }
}