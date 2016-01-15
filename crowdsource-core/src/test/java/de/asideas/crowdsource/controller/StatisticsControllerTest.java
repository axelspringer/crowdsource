package de.asideas.crowdsource.controller;

import de.asideas.crowdsource.presentation.statistics.requests.TimeRangedStatisticsRequest;
import de.asideas.crowdsource.service.StatisticsService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = StatisticsControllerTest.Config.class)
public class StatisticsControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private StatisticsService statisticsService;

    private MockMvc mockMvc;

    @Before
    public void init() {

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        reset(statisticsService);
    }

    @Test
    public void getCurrentStatisticsResult_should_call_statistics_service() throws Exception {
        ArgumentCaptor<TimeRangedStatisticsRequest> argumentCaptor = ArgumentCaptor.forClass(TimeRangedStatisticsRequest.class);
        when(statisticsService.getCurrentStatistics(argumentCaptor.capture())).thenReturn(Collections.emptyList());

        final String dateTimeString = "2016-01-12T22:59:59.000Z";
        mockMvc.perform(get("/statistics/current")
                .contentType(MediaType.APPLICATION_JSON)
                .param("startDate", dateTimeString)
                .param("endDate", dateTimeString))
            .andDo(print())
            .andExpect(status().is(200));

        DateTime dateTime = DateTime.parse(dateTimeString).withZone(DateTimeZone.getDefault());
        assertThat(argumentCaptor.getValue().getStartDate(), is(dateTime.withTimeAtStartOfDay()));
        assertThat(argumentCaptor.getValue().getEndDate(), is(dateTime.plusDays(1).withTimeAtStartOfDay()));

    }

    @Test
    public void getProjectsPerStatus_should_call_statistics_service() throws Exception {
        when(statisticsService.getProjectsPerStatus()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/statistics/projects_per_status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200));

    }

    @EnableWebMvc
    @Configuration
    static class Config {

        @Bean
        public StatisticsController statisticsController() {
            return new StatisticsController(statisticsService());
        }

        @Bean
        public StatisticsService statisticsService() {
            return mock(StatisticsService.class);
        }
    }
}