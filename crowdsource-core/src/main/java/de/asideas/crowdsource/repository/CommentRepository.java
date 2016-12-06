package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.domain.model.CommentEntity;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.presentation.statistics.results.BarChartStatisticsResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    List<CommentEntity> findByProject(ProjectEntity projectEntity);

//    List<Object[]> test(DateTime startDate, DateTime endDate);

    List<BarChartStatisticsResult> countCommentsGroupByProject(int projectCount);

}
