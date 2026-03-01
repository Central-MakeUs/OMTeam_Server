package com.omteam.omt.report.dto;

import java.util.List;

public record BatchProcessResult(int totalCount, int successCount, int failedCount, List<Long> failedUserIds) {

    public static BatchProcessResult of(int totalCount, int successCount, List<Long> failedUserIds) {
        return new BatchProcessResult(totalCount, successCount, totalCount - successCount, failedUserIds);
    }
}
