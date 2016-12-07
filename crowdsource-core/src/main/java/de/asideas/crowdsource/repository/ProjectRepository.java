package de.asideas.crowdsource.repository;

import de.asideas.crowdsource.domain.model.FinancingRoundEntity;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.shared.ProjectStatus;
import org.joda.time.DateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

    List<ProjectEntity> findByStatusOrderByCreatedDateDesc(ProjectStatus projectStatus);

    List<ProjectEntity> findByFinancingRound(FinancingRoundEntity financingRound);

    List<ProjectEntity> findByCreatedDateBetween(DateTime startDate, DateTime endDate);

    @Query("select p.status, count(p) from ProjectEntity p group by p.status")
    List<Object[]> findProjectGroupByStatus();

    @Query("select p from ProjectEntity p left outer join p.comments c group by p.id order by count(c) DESC")
    List<ProjectEntity> countCommentsGroupingByProject(Pageable pageable);
}
