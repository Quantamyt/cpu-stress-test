import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CPUStressTest {

    private static final Logger logger = Logger.getLogger(CPUStressTest.class.getName());

    static {
        // Set up logger with a console handler for better visibility
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO);
        logger.addHandler(consoleHandler);
    }

    public static void main(String[] args) {
        int numThreads = 80; // Number of threads
        int numIterations = 30; // Number of iterations per thread

        // Executor for thread management
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicBoolean stopThreads = new AtomicBoolean(false);
        AtomicInteger totalVal = new AtomicInteger(0);
        AtomicLong startTime = new AtomicLong(0L);
        AtomicLong endTime = new AtomicLong(0L);

        // Use a CountDownLatch to wait for all threads to finish
        CountDownLatch latch = new CountDownLatch(numThreads);

        // Start the clock
        startTime.set(System.currentTimeMillis());

        // Submit tasks to the executor
        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    int workDone = 0;
                    for (int j = 0; j < numIterations; j++) {
                        double[] data = generateRandomData(1024);
                        performFFT(data);
                        workDone++;
                        if (stopThreads.get()) {
                            return; // Stop the thread if the flag is set
                        }
                    }
                    totalVal.addAndGet(workDone); // Update total work done
                    logger.info("Thread " + threadIndex + " completed " + workDone + " iterations.");
                } catch (Exception e) {
                    logger.severe("Exception in thread " + threadIndex + ": " + e.getMessage());
                } finally {
                    latch.countDown(); // Ensure latch is decremented when each thread finishes
                    logger.info("Thread " + threadIndex + " has finished.");
                }
            });
        }

        // Add shutdown hook to handle clean shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Set the flag to stop threads and finalize time
            stopThreads.set(true);
            endTime.set(System.currentTimeMillis());

            // Ensure we wait until all threads have completed before logging the summary
            try {
                latch.await(); // Wait for all threads to finish
            } catch (InterruptedException e) {
                logger.severe("Shutdown interrupted: " + e.getMessage());
            }

            logger.info("Test completed. Stopping threads...");

            double timeTook = (endTime.get() - startTime.get()) / 1000.0;
            int totalIterations = totalVal.get();
            double iterationsPerSecond = totalIterations / timeTook;

            // Print results
            logger.info("Test Summary:");
            logger.info("Total iterations done: " + totalIterations);
            logger.info("Time taken: " + timeTook + " seconds");
            logger.info("Iterations per second: " + iterationsPerSecond);
        }));

        // Gracefully shut down the executor after all tasks have been submitted
        executor.shutdown();
        try {
            // Wait for all tasks to complete
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // Forcefully shut down if tasks do not finish in time
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        try {
            // Wait for the CountDownLatch to ensure all threads complete
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("All threads have exited.");
    }

    // Generate random data for FFT simulation
    private static double[] generateRandomData(int size) {
        double[] data = new double[size];
        for (int i = 0; i < size; i++) {
            data[i] = Math.random();
        }
        return data;
    }

    /**
     * Custom Cooley-Tukey FFT Algorithm (1D)
     * @author Quantamyt
     * */
    private static void performFFT(double[] data) {
        int n = data.length;
        if (n <= 1) return;

        // Perform the FFT recursively using the Cooley-Tukey algorithm
        // Split the array into even and odd indexed elements
        double[] even = new double[n / 2];
        double[] odd = new double[n / 2];

        for (int i = 0; i < n / 2; i++) {
            even[i] = data[2 * i];
            odd[i] = data[2 * i + 1];
        }

        // Recursively compute the FFT of the even and odd parts
        performFFT(even);
        performFFT(odd);

        // Combine the results
        for (int k = 0; k < n / 2; k++) {
            double t = Math.PI * k / (n / 2);
            double cosT = Math.cos(t);
            double sinT = Math.sin(t);
            double oddReal = cosT * odd[k] - sinT * odd[k];
            double oddImag = sinT * odd[k] + cosT * odd[k];

            data[k] = even[k] + oddReal;   // Even part + transformed odd part
            data[k + n / 2] = even[k] - oddReal; // Even part - transformed odd part
        }
    }
}
