import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CPUStressTest {

    public static void main(String[] args) {
        int numThreads = 12;
        int numIterations = 10;

        Thread[] threads = new Thread[numThreads];
        int[] workDone = new int[numThreads];
        AtomicBoolean stopThreads = new AtomicBoolean(false);
        AtomicInteger totalVal = new AtomicInteger(0);
        AtomicLong startTime = new AtomicLong(0L);
        AtomicLong endTime = new AtomicLong(0L);

        startTime.set(System.currentTimeMillis());
        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < numIterations; j++) {
                    double[] data = generateRandomData(1024);
                    performFFT(data);
                    workDone[threadIndex]++;
                    if (stopThreads.get()) {
                        return; // Stop the thread if the flag is set
                    }
                }
                System.out.println("Thread " + threadIndex + " completed " + workDone[threadIndex] + " iterations.");
            });
            threads[i].start();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stopThreads.set(true); // Set the flag to stop threads before printing work done
            endTime.set(System.currentTimeMillis());
            System.out.println(endTime.get());
            System.out.println(startTime.get());
            double localEndTime = endTime.get();
            double localStartTime = startTime.get();
            double timeTook = (double)((localEndTime - localStartTime) / 1000);
            double itPerSecond = 0;
            for (int i = 0; i < numThreads; i++) {
                System.out.println("Thread " + i + " work done: " + workDone[i]);
                totalVal.getAndAdd(workDone[i]);
                if(i == numThreads - 1) {
                    itPerSecond = totalVal.get() / timeTook;
                    System.out.println("Total iterations done: " + totalVal.get());
                    System.out.println("Time took in milliseconds: " + timeTook * 1000);
                    System.out.println("Iterations per second: " + itPerSecond + " (" + itPerSecond / 1000 + " per millisecond)");
                }
            }
        }));

        System.out.println("Startup.");

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("All threads have exited.");
    }

    private static double[] generateRandomData(int size) {
        double[] data = new double[size];
        for (int i = 0; i < size; i++) {
            data[i] = Math.random();
        }
        return data;
    }

    private static void performFFT(double[] data) {
        int n = data.length;

        for (int m = 0; m < n; m++) {
            double sumReal = 0;
            double sumImag = 0;
            for (int k = 0; k < n; k++) {
                double angle = 2 * Math.PI * k * m / n;
                sumReal += data[k] * Math.cos(angle);
                sumImag -= data[k] * Math.sin(angle);
            }
        }
    }
}
