package com.projeto.egoodapp.data.model;

public final class DealerPerformance {
    public final int views, contacts, sales, conversionPercent;
    public DealerPerformance(int views, int contacts, int sales, int conversionPercent) {
        this.views = views; this.contacts = contacts; this.sales = sales;
        this.conversionPercent = conversionPercent;
    }
    public static int nextMilestone(int value) {
        int[] milestones = {10, 50, 100, 500, 1000, 5000};
        for (int milestone : milestones) if (value <= milestone) return milestone;
        long blocks = ((long) value + 4999L) / 5000L;
        return (int) Math.min(Integer.MAX_VALUE, blocks * 5000L);
    }
}
