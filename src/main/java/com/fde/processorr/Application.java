package com.fde.processorr;

import com.fde.processor.model.*;
import com.fde.processor.service.*;
import com.fde.processor.simulator.TransientFailureSimulator;
import com.fde.processor.store.InMemoryTransactionStore;
import com.fde.processor.store.TransactionStore;

import java.math.BigDecimal;
import java.util.List;

public class Application {
    public static void main(String[] args) {
        TransactionStore store = new InMemoryTransactionStore();
        AccountLedgerService ledger = new AccountLedgerService();
        TransactionValidationService validator = new TransactionValidationService();
        TransientFailureSimulator failureSimulator = new TransientFailureSimulator();

        // Simulate transient network fault on TXN-2005 (retry scenario)
        failureSimulator.registerTransientFailureTarget("TXN-2005");

        IdempotentTransactionProcessor processor = 
            new IdempotentTransactionProcessor(store, ledger, validator, failureSimulator);

        // Official assessment batch (Section 9.8)
        List<TransactionRequest> batch = List.of(
            // 1. Arrives early (Seq 2 before Seq 1) -> PENDING_OUT_OF_ORDER
            new TransactionRequest("TXN-2002", "REQ-2", 2, "ACC-201", TransactionType.DEBIT, new BigDecimal("40.00"), "USD"),
            
            // 2. Seq 1 arrives -> PROCESSED, then triggers drain on TXN-2002 -> PROCESSED
            new TransactionRequest("TXN-2001", "REQ-1", 1, "ACC-201", TransactionType.CREDIT, new BigDecimal("100.00"), "USD"),
            
            // 3. Duplicate of TXN-2002 -> DUPLICATE
            new TransactionRequest("TXN-2002", "REQ-2-DUP", 2, "ACC-201", TransactionType.DEBIT, new BigDecimal("40.00"), "USD"),
            
            // 4. In order -> PROCESSED
            new TransactionRequest("TXN-2003", "REQ-3", 3, "ACC-201", TransactionType.CREDIT, new BigDecimal("25.00"), "USD"),
            
            // 5. Negative amount -> FAILED_VALIDATION
            new TransactionRequest("TXN-2004", "REQ-4", 4, "ACC-202", TransactionType.DEBIT, new BigDecimal("-10.00"), "USD"),
            
            // 6. Transient failure trigger -> RETRY_PENDING -> PROCESSED on retry
            new TransactionRequest("TXN-2005", "REQ-5", 5, "ACC-203", TransactionType.CREDIT, new BigDecimal("500.00"), "USD")
        );

        System.out.println("Starting batch ingestion...\n");
        batch.forEach(processor::process);

        // Print final status and metrics
        System.out.println(processor.getSummary());
        System.out.println("Final Ledger Balance ACC-201: $" + ledger.getBalance("ACC-201")); // Expected: 100 - 40 + 25 = 85.00
        System.out.println("Final Ledger Balance ACC-203: $" + ledger.getBalance("ACC-203")); // Expected: 500.00
    }
}
