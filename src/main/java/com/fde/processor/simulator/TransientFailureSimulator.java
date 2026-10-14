package com.fde.processor.simulator;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulates a transient downstream network or database failure once per configured transactionId.
 */
public class TransientFailureSimulator {
    private final Set<String> failedOnce = ConcurrentHashMap.newKeySet();
    private final Set<String> targetFailureTxns = ConcurrentHashMap.newKeySet();

    public void registerTransientFailureTarget(String transactionId) {
        targetFailureTxns.add(transactionId);
    }

    public void simulate(String transactionId) throws RuntimeException {
        if (targetFailureTxns.contains(transactionId) && !failedOnce.contains(transactionId)) {
            failedOnce.add(transactionId);
            throw new RuntimeException("Simulated transient network timeout for transaction: " + transactionId);
        }
    }
}