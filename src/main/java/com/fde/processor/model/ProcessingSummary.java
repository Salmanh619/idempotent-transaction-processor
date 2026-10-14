package com.fde.processor.model;

import java.util.EnumMap;
import java.util.Map;

public class ProcessingSummary {

	private int totalRecords = 0;
    private final Map<TransactionStatus, Integer> counts = new EnumMap<>(TransactionStatus.class);

    public ProcessingSummary() {
        for (TransactionStatus status : TransactionStatus.values()) {
            counts.put(status, 0);
        }
    }

    public synchronized void recordStatus(TransactionStatus status) {
        totalRecords++;
        counts.put(status, counts.get(status) + 1);
    }

    public int getTotalRecords() { return totalRecords; }
    public int getCount(TransactionStatus status) { return counts.getOrDefault(status, 0); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("\n================ PROCESSING SUMMARY ================\n");
        sb.append(String.format("Total Ingested: %d\n", totalRecords));
        for (Map.Entry<TransactionStatus, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 0) {
                sb.append(String.format(" - %-22s: %d\n", entry.getKey(), entry.getValue()));
            }
        }
        sb.append("====================================================\n");
        return sb.toString();
    }
}
