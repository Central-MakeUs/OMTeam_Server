package com.omteam.omt.report.dto;

public record BatchProcessResult(int totalCount, int successCount, int failedCount) {

    public static BatchProcessResult of(int totalCount, int successCount) {
        return new BatchProcessResult(totalCount, successCount, totalCount - successCount);
    }
}
