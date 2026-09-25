package com.edumerge.ticketsystem.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DashboardStats {
    private long totalTickets;
    private Map<String, Long> byStatus;
    private Map<String, Long> byPriority;
    private Map<String, Long> byCategory;
    private long breachedCount;
    private double avgResolutionHours;
    private long openOlderThan48h;
}
