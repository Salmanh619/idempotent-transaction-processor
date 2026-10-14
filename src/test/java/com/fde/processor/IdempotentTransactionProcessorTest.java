package com.fde.processor;

import com.fde.processor.model.*;
import com.fde.processor.service.*;
import com.fde.processor.simulator.TransientFailureSimulator;
import com.fde.processor.store.InMemoryTransactionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class IdempotentTransactionProcessorTest {

    private InMemoryTransactionStore store;
    private AccountLedgerService ledger;
    private TransactionValidationService validator;
    private TransientFailureSimulator failureSimulator;
    private IdempotentTransactionProcessor processor;

    @BeforeEach
    void setUp() {
        store = new InMemoryTransactionStore();
        ledger = new AccountLedgerService();
        validator = new TransactionValidationService();
        failureSimulator = new TransientFailureSimulator();
        processor = new IdempotentTransactionProcessor(store, ledger, validator, failureSimulator);
    }

    @Test
    @DisplayName("L2-FR-02: Should reject malformed records with negative or zero amounts")
    void testValidationFailure() {
        TransactionRequest req = new TransactionRequest(
                "TXN-ERR", "REQ-ERR", 1, "ACC-1", TransactionType.DEBIT, new BigDecimal("-10.00"), "USD");

        processor.process(req);

        assertEquals(TransactionStatus.FAILED_VALIDATION, req.getStatus());
        assertEquals(BigDecimal.ZERO, ledger.getBalance("ACC-1"));
        assertEquals(1, processor.getSummary().getCount(TransactionStatus.FAILED_VALIDATION));
    }

    @Test
    @DisplayName("L2-FR-05 & L2-FR-06: Duplicate transaction ID must not cause secondary ledger balance changes")
    void testIdempotencyDeduplication() {
        TransactionRequest req1 = new TransactionRequest(
                "TXN-101", "REQ-101", 1, "ACC-1", TransactionType.CREDIT, new BigDecimal("100.00"), "USD");
        TransactionRequest duplicate = new TransactionRequest(
                "TXN-101", "REQ-101-RETRY", 1, "ACC-1", TransactionType.CREDIT, new BigDecimal("100.00"), "USD");

        processor.process(req1);
        processor.process(duplicate);

        assertEquals(TransactionStatus.PROCESSED, req1.getStatus());
        assertEquals(TransactionStatus.DUPLICATE, duplicate.getStatus());
        assertEquals(new BigDecimal("100.00"), ledger.getBalance("ACC-1")); // Not 200.00
    }

    @Test
    @DisplayName("L2-FR-07: Out-of-order sequence (Seq 2 before Seq 1) buffers and executes automatically")
    void testOutOfOrderBufferingAndReplay() {
        TransactionRequest seq2 = new TransactionRequest(
                "TXN-2002", "REQ-2", 2, "ACC-201", TransactionType.DEBIT, new BigDecimal("40.00"), "USD");
        TransactionRequest seq1 = new TransactionRequest(
                "TXN-2001", "REQ-1", 1, "ACC-201", TransactionType.CREDIT, new BigDecimal("100.00"), "USD");

        // Process Seq 2 first
        processor.process(seq2);
        assertEquals(TransactionStatus.PENDING_OUT_OF_ORDER, seq2.getStatus());
        assertEquals(BigDecimal.ZERO, ledger.getBalance("ACC-201"));

        // Process Seq 1 -> should execute Seq 1, then drain buffered Seq 2
        processor.process(seq1);
        assertEquals(TransactionStatus.PROCESSED, seq1.getStatus());
        assertEquals(TransactionStatus.PROCESSED, seq2.getStatus());
        
        // Final balance: 100 - 40 = 60.00
        assertEquals(new BigDecimal("60.00"), ledger.getBalance("ACC-201"));
    }

    @Test
    @DisplayName("L2-FR-08 & L2-FR-09: Transient failure recovers on retry without duplication")
    void testTransientFailureWithSuccessfulRetry() {
        failureSimulator.registerTransientFailureTarget("TXN-RETRY");
        TransactionRequest txn = new TransactionRequest(
                "TXN-RETRY", "REQ-5", 1, "ACC-300", TransactionType.CREDIT, new BigDecimal("250.00"), "USD");

        processor.process(txn);

        assertEquals(TransactionStatus.PROCESSED, txn.getStatus());
        assertEquals(1, txn.getRetryCount());
        assertEquals(new BigDecimal("250.00"), ledger.getBalance("ACC-300"));
    }
}