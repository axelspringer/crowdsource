package de.asideas.crowdsource.service.statistics;

import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.presentation.statistics.requests.TimeRangedStatisticsRequest;
import de.asideas.crowdsource.presentation.statistics.results.LineChartStatisticsResult;
import de.asideas.crowdsource.repository.UserRepository;
import org.joda.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static de.asideas.crowdsource.domain.shared.StatisticsTypes.SUM_REGISTERED_USER;

@Async
@Service
public class RegisteredUserSumAction {

    private final UserRepository userRepository;

    @Autowired
    public RegisteredUserSumAction(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Future<LineChartStatisticsResult> getCountOfRegisteredUsersByTimeRange(TimeRangedStatisticsRequest request) {

        final List<UserEntity> userEntityList = userRepository.findByCreatedDateBetween(request.getStartDate(), request.getEndDate());

        final Map<Instant, Long> map = userEntityList.stream().collect(Collectors.groupingBy(
                p -> p.getCreatedDate().withTimeAtStartOfDay().toInstant(),
                Collectors.reducing(
                        0L,
                        t -> 1L,
                        Long::sum
                )
        ));

        final LineChartStatisticsResult result = new LineChartStatisticsResult(SUM_REGISTERED_USER.getDisplayName(), new ArrayList<>(map.values()));


        return new AsyncResult<>(result);
    }
}
