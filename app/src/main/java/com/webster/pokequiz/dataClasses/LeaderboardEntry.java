package com.webster.pokequiz.dataClasses;

public class LeaderboardEntry implements Comparable<LeaderboardEntry> {
    private String name;
    private int score;

    public LeaderboardEntry(String name, int score) {
        this.name = name;
        this.score = score;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    @Override
    public int compareTo(LeaderboardEntry o) {
        return score - o.getScore();
    }
}
