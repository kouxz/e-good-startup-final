package com.projeto.egoodapp.data.model;

public final class DealerRatingSummary {
    public final double average;
    public final int count;
    public final Integer userScore;

    public DealerRatingSummary(double average, int count, Integer userScore) {
        this.average = average;
        this.count = count;
        this.userScore = userScore;
    }
}
