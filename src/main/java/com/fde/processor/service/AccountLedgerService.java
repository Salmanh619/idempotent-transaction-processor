package com.fde.processor.service;

import com.fde.processor.model.TransactionRequest;
import com.fde.processor.model.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AccountLedgerService {
    private static final Logger log = LoggerFactory.getLogger(AccountLedgerService.class);
    private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>();

    public synchronized void applyTransaction(TransactionRequest txn) {
        BigDecimal currentBalance = balances.getOrDefault(txn.getAccountId(), BigDecimal.ZERO);
        BigDecimal newBalance;

        if (txn.getTransactionType() == TransactionType.CREDIT) {
            newBalance = currentBalance.add(txn.getAmount());
        } else if (txn.getTransactionType() == TransactionType.DEBIT) {
            newBalance = currentBalance.subtract(txn.getAmount());
        } else if (txn.getTransactionType() == TransactionType.REVERSAL) {
            newBalance = currentBalance.subtract(txn.getAmount());
        } else {
            throw new IllegalArgumentException("Unsupported transaction type: " + txn.getTransactionType());
        }

        balances.put(txn.getAccountId(), newBalance);
        log.info("Ledger updated for Account: {} | Previous: {} | New: {} | Txn: {}", 
                txn.getAccountId(), currentBalance, newBalance, txn.getTransactionId());
    }

    public BigDecimal getBalance(String accountId) {
        return balances.getOrDefault(accountId, BigDecimal.ZERO);
    }
}