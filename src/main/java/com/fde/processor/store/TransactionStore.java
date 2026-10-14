package com.fde.processor.store;

import com.fde.processor.model.TransactionRequest;
import java.util.List;
import java.util.Optional;

public interface TransactionStore {

	boolean isProcessedOrDuplicate(String transactionId, String requestId);
    void recordProcessed(TransactionRequest txn);
    void bufferPending(TransactionRequest txn);
    List<TransactionRequest> getPendingForAccount(String accountId);
    void removePending(String accountId, TransactionRequest txn);
    long getLastProcessedSequence(String accountId);
    void setLastProcessedSequence(String accountId, long seq);
}
