package de.asideas.crowdsource.presentation.statistics.results;

public class CurrentStatisticsResult {
    private final long sumOfUser;
    private final long sumOfProject;

    public CurrentStatisticsResult(long sumOfUser, long sumOfProject) {
        this.sumOfUser = sumOfUser;
        this.sumOfProject = sumOfProject;
    }

    public long getSumOfUser() {
        return sumOfUser;
    }

    public long getSumOfProject() {
        return sumOfProject;
    }
}
