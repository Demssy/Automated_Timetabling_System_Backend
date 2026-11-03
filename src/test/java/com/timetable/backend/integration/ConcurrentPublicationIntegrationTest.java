package com.timetable.backend.integration;

import com.timetable.backend.domain.model.ScheduleMetadata;
import com.timetable.backend.domain.model.ScheduleStatus;
import com.timetable.backend.domain.repository.ScheduleMetadataRepository;
import com.timetable.backend.service.ScheduleMetadataService;
import com.timetable.backend.service.ScheduleScoreEvaluationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrentPublicationIntegrationTest {
    @Autowired private ScheduleMetadataRepository schedules;
    @Autowired private ScheduleMetadataService metadataService;
    @Autowired private TransactionTemplate transactions;
    @MockitoBean private ScheduleScoreEvaluationService evaluator;

    @Test
    void onlyOneOverlappingScheduleCanPublish() throws Exception {
        List<Long> ids = transactions.execute(status -> List.of(create("Concurrent A"), create("Concurrent B")));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> publishWhenReleased(ids.get(0), start));
            Future<Boolean> second = executor.submit(() -> publishWhenReleased(ids.get(1), start));
            start.countDown();
            int successes = (first.get(20, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(20, TimeUnit.SECONDS) ? 1 : 0);
            assertThat(successes).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
            transactions.executeWithoutResult(status -> ids.forEach(schedules::deleteById));
        }
    }

    private Long create(String name) {
        return schedules.save(ScheduleMetadata.builder().name(name)
                .validFrom(LocalDate.of(2026, 1, 1)).validTo(LocalDate.of(2026, 1, 7))
                .status(ScheduleStatus.DRAFT).build()).getId();
    }

    private boolean publishWhenReleased(Long id, CountDownLatch start) throws InterruptedException {
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start publication");
        }
        try {
            metadataService.publish(id);
            return true;
        } catch (IllegalStateException e) {
            if (!e.getMessage().contains("same date range")) {
                throw e;
            }
            return false;
        }
    }
}
