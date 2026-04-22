package com.library.service;

import com.library.dto.RaceConditionDemoResultDto;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class RaceConditionDemoService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long TERMINATION_TIMEOUT_SECONDS = 5L;

    public RaceConditionDemoResultDto runDemo(
            int threadCount,
            int incrementsPerThread
    ) {
        CounterSnapshot snapshot = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            snapshot = runSingleDemo(threadCount, incrementsPerThread);
            if (snapshot.unsafeCounterValue() < snapshot.expectedValue()) {
                break;
            }
        }

        int lostUpdates = snapshot.expectedValue() - snapshot.unsafeCounterValue();
        return new RaceConditionDemoResultDto(
                threadCount,
                incrementsPerThread,
                snapshot.expectedValue(),
                snapshot.unsafeCounterValue(),
                snapshot.synchronizedCounterValue(),
                snapshot.atomicCounterValue(),
                lostUpdates > 0,
                lostUpdates
        );
    }

    private CounterSnapshot runSingleDemo(
            int threadCount,
            int incrementsPerThread
    ) {
        UnsafeCounter unsafeCounter = new UnsafeCounter();
        SynchronizedCounter synchronizedCounter = new SynchronizedCounter();
        AtomicInteger atomicCounter = new AtomicInteger();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < threadCount; index++) {
                futures.add(executorService.submit(() -> {
                    awaitStart(startLatch);
                    for (int increment = 0; increment < incrementsPerThread; increment++) {
                        unsafeCounter.increment();
                        synchronizedCounter.increment();
                        atomicCounter.incrementAndGet();
                    }
                }));
            }

            startLatch.countDown();
            waitForCompletion(futures);

            int expectedValue = threadCount * incrementsPerThread;
            return new CounterSnapshot(
                    expectedValue,
                    unsafeCounter.getValue(),
                    synchronizedCounter.getValue(),
                    atomicCounter.get()
            );
        } finally {
            shutdownExecutor(executorService);
        }
    }

    private void awaitStart(CountDownLatch startLatch) {
        try {
            startLatch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Race condition demo was interrupted", exception);
        }
    }

    private void waitForCompletion(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Race condition demo was interrupted", exception);
            } catch (ExecutionException exception) {
                throw new IllegalStateException(
                        "Race condition demo failed",
                        exception.getCause()
                );
            }
        }
    }

    private void shutdownExecutor(ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(
                    TERMINATION_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            )) {
                List<Runnable> droppedTasks = executorService.shutdownNow();
                throw new IllegalStateException(
                        "Race condition demo did not finish in time. Cancelled tasks: "
                                + droppedTasks.size()
                );
            }
        } catch (InterruptedException exception) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Race condition demo shutdown was interrupted",
                    exception
            );
        }
    }

    private static final class UnsafeCounter {

        private int value;

        private void increment() {
            value++;
        }

        private int getValue() {
            return value;
        }
    }

    private static final class SynchronizedCounter {

        private int value;

        private synchronized void increment() {
            value++;
        }

        private synchronized int getValue() {
            return value;
        }
    }

    private record CounterSnapshot(
            int expectedValue,
            int unsafeCounterValue,
            int synchronizedCounterValue,
            int atomicCounterValue
    ) {
    }
}
