package de.asideas.crowdsource.repository;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import de.asideas.crowdsource.presentation.statistics.results.LineChartStatisticsResult;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapreduce.MapReduceResults;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Iterator;

import static de.asideas.crowdsource.domain.model.CommentEntity.COLLECTION_NAME;
import static de.asideas.crowdsource.repository.CommentRepositoryImpl.CHART_NAME_SUM_COMMENTS;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CommentRepositoryImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private CommentRepositoryImpl commentRepository;

    private DateTime startDate;
    private DateTime endDate;

    @Test
    public void sumCommentsGroupByCreatedDate_should_return_empty_linechart_instance_although_no_data_from_db() throws Exception {
        DateTime startDate = DateTime.now().minusDays(1);
        DateTime endDate = DateTime.now();

        MapReduceResults<CommentRepositoryImpl.KeyValuePair> mockedMapReduceResult = mock(MapReduceResults.class);

        when(mongoTemplate.mapReduce(any(Query.class), eq(COLLECTION_NAME), anyString(), anyString(), eq(CommentRepositoryImpl.KeyValuePair.class)))
                .thenReturn(mockedMapReduceResult);

        Iterator mockIterator = mock(Iterator.class);
        when(mockedMapReduceResult.iterator()).thenReturn(mockIterator);

        LineChartStatisticsResult result = commentRepository.sumCommentsGroupByCreatedDate(startDate, endDate);

        // Verify empty result generation for empty data from db.
        verify(mockIterator).hasNext();

        assertThat(result.getName(), is(CHART_NAME_SUM_COMMENTS));
        assertThat(result.getData().size(), is(0));
    }

    @Test
    public void sumCommentsGroupByCreatedDate_should_call_mongo_with_timerange_query() throws Exception {
        DateTime startDate = DateTime.now().minusDays(1);
        DateTime endDate = DateTime.now();

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);

        MapReduceResults<CommentRepositoryImpl.KeyValuePair> mockedMapReduceResult = mock(MapReduceResults.class);

        when(mongoTemplate.mapReduce(captor.capture(), eq(COLLECTION_NAME), anyString(), anyString(), eq(CommentRepositoryImpl.KeyValuePair.class)))
                .thenReturn(mockedMapReduceResult);

        Iterator mockIterator = mock(Iterator.class);
        when(mockedMapReduceResult.iterator()).thenReturn(mockIterator);

        commentRepository.sumCommentsGroupByCreatedDate(startDate, endDate);

        Query capturedTimeFilterParam = captor.getValue();

        DBObject capturedQuery = capturedTimeFilterParam.getQueryObject();
        assertThat(capturedQuery.get("createdDate"), instanceOf(BasicDBObject.class));
        BasicDBObject createdDateFromQuery = (BasicDBObject) capturedQuery.get("createdDate");
        assertThat(createdDateFromQuery.get("$gte"), instanceOf(DateTime.class));
        assertThat(createdDateFromQuery.get("$lte"), instanceOf(DateTime.class));
        assertThat(createdDateFromQuery.get("$gte"), is(startDate));
        assertThat(createdDateFromQuery.get("$lte"), is(endDate));
    }

    @Test
    public void sumCommentsGroupByCreatedDate_should_map_results_into_linechart_representation() {
        DateTime startDate = DateTime.now().minusDays(1);
        DateTime endDate = DateTime.now();

        MapReduceResults<CommentRepositoryImpl.KeyValuePair> mockedMapReduceResult = mock(MapReduceResults.class);

        when(mongoTemplate.mapReduce(any(Query.class), eq(COLLECTION_NAME), anyString(), anyString(), eq(CommentRepositoryImpl.KeyValuePair.class)))
                .thenReturn(mockedMapReduceResult);

        Iterator mockIterator = mock(Iterator.class);
        when(mockedMapReduceResult.iterator()).thenReturn(mockIterator);
        CommentRepositoryImpl.KeyValuePair mockResult1 = new CommentRepositoryImpl.KeyValuePair("1453037072969", 3L);
        CommentRepositoryImpl.KeyValuePair mockResult2 = new CommentRepositoryImpl.KeyValuePair("1452898800000", 1L);

        when(mockIterator.hasNext()).thenReturn(true, true, false);
        when(mockIterator.next()).thenReturn(mockResult1, mockResult2);

        LineChartStatisticsResult result = commentRepository.sumCommentsGroupByCreatedDate(startDate, endDate);

        verify(mockIterator, times(3)).hasNext();
        verify(mockIterator, times(2)).next();

        assertThat(result.getName(), is(CHART_NAME_SUM_COMMENTS));
        assertThat(result.getData().size(), is(2));
        assertThat(result.getData().get(0).getLabel(), is("2016-01-17"));
        assertThat(result.getData().get(0).getData(), is(3L));
        assertThat(result.getData().get(1).getLabel(), is("2016-01-16"));
        assertThat(result.getData().get(1).getData(), is(1L));
    }

    @Test(expected = IllegalStateException.class)
    public void sumCommentsGroupByCreatedDate_should_throw_exception_on_unparseable_data_from_db() {
        startDate = DateTime.now().minusDays(1);
        endDate = DateTime.now();

        MapReduceResults<CommentRepositoryImpl.KeyValuePair> mockedMapReduceResult = mock(MapReduceResults.class);

        when(mongoTemplate.mapReduce(any(Query.class), eq(COLLECTION_NAME), anyString(), anyString(), eq(CommentRepositoryImpl.KeyValuePair.class)))
                .thenReturn(mockedMapReduceResult);

        Iterator mockIterator = mock(Iterator.class);
        when(mockedMapReduceResult.iterator()).thenReturn(mockIterator);
        CommentRepositoryImpl.KeyValuePair mockResult = new CommentRepositoryImpl.KeyValuePair("test_abc", 1L);

        when(mockIterator.hasNext()).thenReturn(true, false);
        when(mockIterator.next()).thenReturn(mockResult);

        commentRepository.sumCommentsGroupByCreatedDate(startDate, endDate);
    }
//
//    @Test
//    public void countCommentsGroupByProject_should_sort_result() {
//
//    }
}