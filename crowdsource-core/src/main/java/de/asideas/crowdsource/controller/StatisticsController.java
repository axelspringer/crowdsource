package de.asideas.crowdsource.controller;

import de.asideas.crowdsource.presentation.statistics.requests.TimeRangedStatisticsRequest;
import de.asideas.crowdsource.presentation.statistics.results.BarChartStatisticsResult;
import de.asideas.crowdsource.presentation.statistics.results.LineChartStatisticsResult;
import de.asideas.crowdsource.service.StatisticsService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Autowired
    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @RequestMapping(value = "/current", method = RequestMethod.GET)
    public List<LineChartStatisticsResult> getCurrentStatisticsResult(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  DateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime endDate) {

        return statisticsService.getCurrentStatistics(new TimeRangedStatisticsRequest(startDate, endDate));
    }

    @RequestMapping(value = "/projects_per_status")
    public List<BarChartStatisticsResult> getProjectsPerStatus () {
        return statisticsService.getProjectsPerStatus();
    }

}
