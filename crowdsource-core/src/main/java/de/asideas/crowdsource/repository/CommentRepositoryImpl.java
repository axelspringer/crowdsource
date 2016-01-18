package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.presentation.statistics.results.LineChartStatisticsResult;
import de.asideas.crowdsource.service.statistics.StatisticsActionUtil;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapreduce.MapReduceResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.LinkedHashMap;
import java.util.Map;

import static de.asideas.crowdsource.domain.model.CommentEntity.COLLECTION_NAME;

public class CommentRepositoryImpl implements CommentRepositoryCustom {

    public static final String CHART_NAME_SUM_COMMENTS = "Summe Kommentare";
    private final MongoTemplate mongoTemplate;

    @Autowired
    public CommentRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
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
        String reduce = "function (key, values) {" +
                        "   var sum = 0, i = 0;" +
                        "   for (i; i < values.length; i++) {" +
                        "       sum += values[i];" +
                        "   }" +
                        "   return sum;" +
                        "}";
        Query filter = Query.query(Criteria.where("createdDate").gte(startDate).lte(endDate));

        MapReduceResults<ValueObject> sumResults = mongoTemplate.mapReduce (
                filter,
                COLLECTION_NAME,
                map,
                reduce,
                ValueObject.class
        );

        return toLineChartStatisticsResult(sumResults);
    }

    private LineChartStatisticsResult toLineChartStatisticsResult(MapReduceResults<ValueObject> sumResults) {
        Map<String, Long> results = new LinkedHashMap<>();

        for (ValueObject vo : sumResults) {
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

    public static class ValueObject {

        private String id;
        private float value;

        public ValueObject(String id, float value) {
            this.id = id;
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public float getValue() {
            return value;
        }
    }
}
