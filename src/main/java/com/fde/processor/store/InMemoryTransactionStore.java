package com.fde.processor.store;

import com.fde.processor.model.TransactionRequest;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTransactionStore implements TransactionStore {
    // Deduplication sets: transactionId (business key) and requestId (payload key)
    private final Set<String> processedTransactionIds = ConcurrentHashMap.newKeySet();
    private final Set<String> seenRequestIds = ConcurrentHashMap.newKeySet();
    
    // Account sequence tracker: accountId -> highest consecutive sequence number processed
    private final Map<String, Long> accountSequences = new ConcurrentHashMap<>();

    // Buffer for out-of-order items: accountId -> SortedSet of pending transactions by sequenceNumber
    private final Map<String, TreeSet<TransactionRequest>> pendingBuffers = new ConcurrentHashMap<>();

    @Override
    public boolean isProcessedOrDuplicate(String transactionId, String requestId) {
        return processedTransactionIds.contains(transactionId) || seenRequestIds.contains(requestId);
    }

    @Override
    public void recordProcessed(TransactionRequest txn) {
        processedTransactionIds.add(txn.getTransactionId());
        seenRequestIds.add(txn.getRequestId());
    }

    @Override
    public synchronized void bufferPending(TransactionRequest txn) {
        pendingBuffers.computeIfAbsent(txn.getAccountId(), 
            k -> new TreeSet<>(Comparator.comparingLong(TransactionRequest::getSequenceNumber)))
            .add(txn);
    }

    @Override
    public synchronized List<TransactionRequest> getPendingForAccount(String accountId) {
        TreeSet<TransactionRequest> pending = pendingBuffers.get(accountId);
        if (pending == null || pending.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(pending);
    }

    @Override
    public synchronized void removePending(String accountId, TransactionRequest txn) {
        TreeSet<TransactionRequest> pending = pendingBuffers.get(accountId);
        if (pending != null) {
            pending.remove(txn);
        }
    }

    @Override
    public long getLastProcessedSequence(String accountId) {
        return accountSequences.getOrDefault(accountId, 0L);
    }

    @Override
    public void setLastProcessedSequence(String accountId, long seq) {
        accountSequences.put(accountId, seq);
    }
}