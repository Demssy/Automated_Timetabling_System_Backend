package com.timetable.backend.integration;

import com.timetable.backend.domain.model.ScheduleMetadata;
import com.timetable.backend.domain.model.ScheduleStatus;
import com.timetable.backend.domain.repository.ScheduleMetadataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ScheduleRowLockIntegrationTest {
    @Autowired private ScheduleMetadataRepository schedules;
    @Autowired private TransactionTemplate transactions;

    @Test
    void concurrentMutationsWaitForScheduleLock() throws Exception {
        Long id = transactions.execute(status -> schedules.save(ScheduleMetadata.builder()
                .name("Row lock test").validFrom(LocalDate.of(2026, 1, 1))
                .validTo(LocalDate.of(2026, 1, 7)).status(ScheduleStatus.DRAFT).build()).getId());
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch secondLocked = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> transactions.executeWithoutResult(status -> {
                schedules.findByIdForUpdate(id).orElseThrow();
                firstLocked.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to release lock");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }));
            assertThat(firstLocked.await(10, TimeUnit.SECONDS)).isTrue();
            Future<?> second = executor.submit(() -> transactions.executeWithoutResult(status -> {
                secondStarted.countDown();
                schedules.findByIdForUpdate(id).orElseThrow();
                secondLocked.countDown();
            }));
            assertThat(secondStarted.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(secondLocked.await(500, TimeUnit.MILLISECONDS)).isFalse();
            release.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
            assertThat(secondLocked.getCount()).isZero();
        } finally {
            release.countDown();
            executor.shutdownNow();
            transactions.executeWithoutResult(status -> schedules.deleteById(id));
        }
    }
}
