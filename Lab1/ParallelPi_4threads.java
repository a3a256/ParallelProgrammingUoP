
public class ParallelPi_4threads extends Thread {

    public static void main(String[] args) throws Exception {

        long startTime = System.currentTimeMillis();

        ParallelPi_4threads thread1 = new ParallelPi_4threads();
        thread1.begin = 0;
        thread1.end = numSteps / 4;

        ParallelPi_4threads thread2 = new ParallelPi_4threads();
        thread1.begin = numSteps / 4;
        thread1.end = numSteps / 2;

        ParallelPi_4threads thread3 = new ParallelPi_4threads();
        thread1.begin = numSteps / 2;
        thread1.end = (3 * numSteps) / 4;

        ParallelPi_4threads thread4 = new ParallelPi_4threads();
        thread2.begin = (3 * numSteps) / 4;
        thread2.end = numSteps;

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();

        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();

        long endTime = System.currentTimeMillis();

        double pi = step * (thread1.sum + thread2.sum + thread3.sum + thread4.sum);

        System.out.println("Value of pi: " + pi);

        System.out.println("Calculated in "
                + (endTime - startTime) + " milliseconds");
    }

    static int numSteps = 100000000;

    static double step = 1.0 / (double) numSteps;

    double sum;
    int begin, end;

    public void run() {

        sum = 0.0;

        for (int i = begin; i < end; i++) {
            double x = (i + 0.5) * step;
            sum += 4.0 / (1.0 + x * x);
        }
    }
}
