package de.asideas.crowdsource.repository;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@Ignore // FIXME: 18/11/16
@RunWith(MockitoJUnitRunner.class)
public class ProjectRepositoryImplTest {
//
//    @Mock
//    private MongoTemplate mongoTemplate;
//
//    @InjectMocks
//    private ProjectRepositoryImpl projectRepository;
//
//    @Test
//    public void sumProjectsGroupedByStatus_return_empty_list_on_empty_results() {
//
//        ArgumentCaptor<TypedAggregation> aggregationParamCaptor = ArgumentCaptor.forClass(TypedAggregation.class);
//
//        AggregationResults resultMock = mock(AggregationResults.class);
//        when(resultMock.getMappedResults()).thenReturn(Collections.emptyList());
//
//        when(mongoTemplate.aggregate(aggregationParamCaptor.capture(), eq(ProjectPerStatusResult.class))).thenReturn(resultMock);
//
//        List<BarChartStatisticsResult> results = projectRepository.sumProjectsGroupedByStatus();
//
//        TypedAggregation aggregationParam = aggregationParamCaptor.getValue();
//        assertThat("Aggregation input type should be a ProjectEntity", aggregationParam.getInputType() == ProjectEntity.class);
//
//        assertThat(results.size(), is(0));
//    }
//
//    @Test
//    public void sumProjectsGroupedByStatus_return_mapped_results_on_aggregation_results() {
//
//        ArgumentCaptor<TypedAggregation> aggregationParamCaptor = ArgumentCaptor.forClass(TypedAggregation.class);
//
//        AggregationResults resultMock = mock(AggregationResults.class);
//        when(resultMock.getMappedResults()).thenReturn(Arrays.asList (
//            new ProjectRepositoryImpl.ProjectPerStatusResult(ProjectStatus.PUBLISHED, 3L)
//        ));
//
//        when(mongoTemplate.aggregate(aggregationParamCaptor.capture(), eq(ProjectPerStatusResult.class))).thenReturn(resultMock);
//
//        List<BarChartStatisticsResult> results = projectRepository.sumProjectsGroupedByStatus();
//
//        TypedAggregation aggregationParam = aggregationParamCaptor.getValue();
//        assertThat("Aggregation input type should be a ProjectEntity", aggregationParam.getInputType() == ProjectEntity.class);
//
//        assertThat(results.size(), is(1));
//
//        assertThat(results.get(0).getId(), is(ProjectStatus.PUBLISHED.name()));
//        assertThat(results.get(0).getName(), is(ProjectStatus.PUBLISHED.getDisplayName()));
//        assertThat(results.get(0).getCount(), is(3L));
//    }
}