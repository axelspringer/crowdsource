package de.asideas.crowdsource.service.statistics;

import de.asideas.crowdsource.presentation.statistics.requests.TimeRangedStatisticsRequest;
import de.asideas.crowdsource.presentation.statistics.results.LineChartStatisticsResult;
import de.asideas.crowdsource.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentSumAction {

    private final CommentRepository commentRepository;

    @Autowired
    public CommentSumAction(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public LineChartStatisticsResult getSumComments(TimeRangedStatisticsRequest request) {
        return commentRepository.sumCommentsGroupByCreatedDate(request.getStartDate(), request.getEndDate());
    }

}
