package com.fde.processor.model;

import java.math.BigDecimal;
import java.time.Instant;

public class TransactionRequest {

	private String transactionId;
    private String requestId;
    private String accountId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String currency;
    private long sequenceNumber;
    private Instant eventTime;
    private TransactionStatus status;
    private int retryCount;
    private String failureReason;
    
    public TransactionRequest() {
        this.status = TransactionStatus.RECEIVED;
        this.retryCount = 0;
        this.eventTime = Instant.now();
    }

    public TransactionRequest(String transactionId, String requestId, long sequenceNumber,
                              String accountId, TransactionType transactionType,
                              BigDecimal amount, String currency) {
        this();
        this.transactionId = transactionId;
        this.requestId = requestId;
        this.sequenceNumber = sequenceNumber;
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.currency = currency;
    }

    // Getters and Setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(long sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public int getRetryCount() { return retryCount; }
    public void incrementRetryCount() { this.retryCount++; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    @Override
    public String toString() {
        return String.format("[%s | TxnId: %s | ReqId: %s | Seq: %d | Acc: %s | %s %s %s]",
                status, transactionId, requestId, sequenceNumber, accountId, transactionType, amount, currency);
    }
}

