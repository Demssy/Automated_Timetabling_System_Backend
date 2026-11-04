package com.timetable.backend.solver;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverStatus;
import com.timetable.backend.domain.dto.SolverSettingsValues;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class SolverRuntimeRegistry {
    private final SolverRuntimeFactory runtimeFactory;
    private final List<Runtime> runtimes = new ArrayList<>();
    private final ScheduledExecutorService reaper = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "solver-runtime-reaper");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService monitors = Executors.newVirtualThreadPerTaskExecutor();
    private Runtime active;

    @PostConstruct
    void startReaper() {
        reaper.scheduleWithFixedDelay(() -> {
            try {
                cleanUp();
            } catch (RuntimeException e) {
                log.error("Failed to clean up solver runtimes", e);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public synchronized void solve(Long id, SolverSettingsValues settings,
            Function<Long, DanceSchedule> finder, Consumer<DanceSchedule> consumer) {
        cleanUp();
        if (findRunning(id) != null) {
            throw new IllegalStateException("Solver is already running for schedule " + id);
        }
        if (active == null || !active.settings.equals(settings)) {
            Runtime next = new Runtime(settings, SolverManager.create(runtimeFactory.create(settings)));
            runtimes.add(next);
            active = next;
            cleanUp();
        }
        Runtime runtime = active;
        AtomicBoolean finderStarted = new AtomicBoolean();
        runtime.ids.add(id);
        runtime.pendingConsumers.add(id);
        try {
            SolverJob<DanceSchedule, Long> job = runtime.manager.solveBuilder()
                    .withProblemId(id)
                    .withProblemFinder(problemId -> {
                        finderStarted.set(true);
                        return finder.apply(problemId);
                    })
                    .withBestSolutionConsumer(consumer)
                    .withFinalBestSolutionConsumer(solution -> runtime.pendingConsumers.remove(id))
                    .run();
            monitors.execute(() -> monitor(job, runtime, id, finderStarted));
        } catch (RuntimeException e) {
            runtime.ids.remove(id);
            runtime.pendingConsumers.remove(id);
            throw e;
        }
    }

    public synchronized SolverStatus status(Long id) {
        cleanUp();
        Runtime runtime = findRunning(id);
        SolverStatus status = runtime == null ? SolverStatus.NOT_SOLVING : runtime.manager.getSolverStatus(id);
        if (runtime != null && status == SolverStatus.NOT_SOLVING && runtime.pendingConsumers.contains(id)) {
            return SolverStatus.SOLVING_ACTIVE;
        }
        return status;
    }

    public synchronized boolean terminate(Long id) {
        cleanUp();
        Runtime runtime = findRunning(id);
        if (runtime == null) {
            cleanUp();
            return false;
        }
        runtime.manager.terminateEarly(id);
        return true;
    }

    private Runtime findRunning(Long id) {
        return runtimes.stream()
                .filter(runtime -> runtime.ids.contains(id))
                .findFirst().orElse(null);
    }

    private synchronized void cleanUp() {
        for (Runtime runtime : List.copyOf(runtimes)) {
            runtime.ids.removeIf(id -> runtime.manager.getSolverStatus(id) == SolverStatus.NOT_SOLVING
                    && !runtime.pendingConsumers.contains(id));
            if (runtime != active && runtime.ids.isEmpty()) {
                runtime.manager.close();
                runtimes.remove(runtime);
            }
        }
    }

    @PreDestroy
    public synchronized void close() {
        reaper.shutdownNow();
        for (Runtime runtime : runtimes) {
            for (Long id : List.copyOf(runtime.ids)) {
                if (runtime.manager.getSolverStatus(id) != SolverStatus.NOT_SOLVING) {
                    runtime.manager.terminateEarly(id);
                }
            }
        }
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (runtimes.stream().anyMatch(runtime -> !runtime.pendingConsumers.isEmpty())
                && System.nanoTime() < deadline) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for solver result persistence", e);
                break;
            }
        }
        if (runtimes.stream().anyMatch(runtime -> !runtime.pendingConsumers.isEmpty())) {
            log.warn("Closing solver runtimes before all final results were persisted");
        }
        for (Runtime runtime : runtimes) {
            runtime.manager.close();
        }
        monitors.shutdownNow();
        runtimes.clear();
        active = null;
    }

    private void monitor(SolverJob<DanceSchedule, Long> job, Runtime runtime, Long id, AtomicBoolean finderStarted) {
        try {
            while (job.getSolverStatus() == SolverStatus.SOLVING_SCHEDULED) {
                Thread.sleep(10);
            }
            if (job.getSolverStatus() == SolverStatus.NOT_SOLVING && !finderStarted.get()) {
                runtime.pendingConsumers.remove(id);
                return;
            }
            job.getFinalBestSolution();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while monitoring solver for schedule {}", id, e);
        } catch (ExecutionException e) {
            runtime.pendingConsumers.remove(id);
            log.error("Solver failed for schedule {}", id, e.getCause());
        }
    }

    private static final class Runtime {
        private final SolverSettingsValues settings;
        private final SolverManager<DanceSchedule, Long> manager;
        private final Set<Long> ids = new HashSet<>();
        private final Set<Long> pendingConsumers = ConcurrentHashMap.newKeySet();

        private Runtime(SolverSettingsValues settings, SolverManager<DanceSchedule, Long> manager) {
            this.settings = settings;
            this.manager = manager;
        }
    }
}
