package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.presentation.statistics.results.BarChartStatisticsResult;
import de.asideas.crowdsource.presentation.statistics.results.LineChartStatisticsResult;
import de.asideas.crowdsource.service.statistics.StatisticsActionUtil;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapreduce.MapReduceResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.*;
import java.util.stream.Collectors;

import static de.asideas.crowdsource.domain.model.CommentEntity.COLLECTION_NAME;

public class CommentRepositoryImpl implements CommentRepositoryCustom {

    static final String CHART_NAME_SUM_COMMENTS = "Summe Kommentare";
    private final MongoTemplate mongoTemplate;

    @Autowired
    public CommentRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<BarChartStatisticsResult> countCommentsGroupByProject(int projectCount) {

        final String map = "function(){ emit( this.project.$id , 1 ); } ";
        final String reduce = "function(key,values){ return values.length;}";

        MapReduceResults<KeyValuePair> results = mongoTemplate.mapReduce(COLLECTION_NAME, map, reduce, KeyValuePair.class);

        final List<KeyValuePair> sortedList = new ArrayList<>();
        for (KeyValuePair result : results) {
            sortedList.add(result);
        }

        Collections.sort(sortedList);

        return sortedList.stream().limit(projectCount).map(b -> new BarChartStatisticsResult(
                b.getId(),
                mongoTemplate.findOne(new Query(Criteria.where("id").is(b.getId())), ProjectEntity.class).getTitle(),
                b.getValue())).collect(Collectors.toList());
    }

    @Override
    public LineChartStatisticsResult sumCommentsGroupByCreatedDate(DateTime startDate, DateTime endDate) {
        // Emit UTC millis (only year, month, week and day are taken into account) of createdDate as key
        // Value is set to 1 to sum up later (reduce)
        String map  = "function () {  " +
                "   var day = Date.UTC(this.createdDate.getFullYear(), this.createdDate.getMonth(), this.createdDate.getDate()); " +
                "   emit(day.toString(), 1); " +
                "}";
        // Reduce all the values for one "key" (for details about key, see above) by summing up each value.
        final String reduce = "function(key,values){ return values.length;}";

        Query filter = Query.query(Criteria.where("createdDate").gte(startDate).lte(endDate));

        MapReduceResults<KeyValuePair> sumResults = mongoTemplate.mapReduce (
                filter,
                COLLECTION_NAME,
                map,
                reduce,
                KeyValuePair.class
        );

        return toLineChartStatisticsResult(sumResults);
    }

    private LineChartStatisticsResult toLineChartStatisticsResult(MapReduceResults<KeyValuePair> sumResults) {
        Map<String, Long> results = new LinkedHashMap<>();

        for (KeyValuePair vo : sumResults) {
            DateTime dateFromDbId;
            try {
                dateFromDbId = new DateTime(Long.parseLong(vo.getId()));
            } catch (NumberFormatException nfe) {
                throw new IllegalStateException("MapReduce emitted wrong format for id / key field. Expected UTC millis." , nfe);
            }

            results.put(StatisticsActionUtil.formatDate(dateFromDbId), Float.valueOf(vo.getValue()).longValue());
        }

        return new LineChartStatisticsResult(
            CHART_NAME_SUM_COMMENTS,
            results
        );
    }

    public static class KeyValuePair implements Comparable<KeyValuePair> {
        String id;
        Long value;

        public KeyValuePair(String id, Long value) {
            this.id = id;
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Long getValue() {
            return value;
        }

        public void setValue(Long value) {
            this.value = value;
        }

        @Override
        public int compareTo(KeyValuePair o) {
            if (this.value < o.value) {
                return 1;
            } else if (this.value > o.value) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}
