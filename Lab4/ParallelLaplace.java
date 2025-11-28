
import java.awt.*;
import javax.swing.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.BrokenBarrierException;

public class ParallelLaplace extends Thread {

    final static int N = 256;
    final static int P = 4; // experiment with the amount of cores 2, 4, 8
    final static int CELL_SIZE = 2;
    final static int NITER = 100000;
    final static int OUTPUT_FREQ = 1000; // experiment with 100, 1000, 10000

    static float[][] phi = new float[N][N];
    static float[][] newPhi = new float[N][N];

    public static CyclicBarrier newBarrier = new CyclicBarrier(P);

    static Display display = new Display();
    public static void main(String args[]) throws Exception {

        // Make voltage non-zero on left and right edges
        for (int j = 0; j < N; j++) {
            phi[0][j] = 1.0F;
            phi[N - 1][j] = 1.0F;
        }

        display.repaint();
        ParallelLaplace[] threads = new ParallelLaplace[P];

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < P; i++) {
            threads[i] = new ParallelLaplace(i);
            threads[i].start();
        }

        for (int i = 0; i < P; i++) {
            threads[i].join();
        }

        long endTime = System.currentTimeMillis();

        System.out.println("Calculation completed in "
                + (endTime - startTime) + " milliseconds");

        display.repaint();
    }

    int me;

    public ParallelLaplace(int thread) {
        this.me = thread;
    }

    public void run() {
        int step = N / P;
        int start = me * step;
        int end = start + step;
        if (start == 0) {
            start += 1;
        }
        if (end == N) {
            end -= 1;
        }
        int loop_step = NITER / P;
        int loop_start = loop_step * me;
        int loop_end = loop_start + loop_step;
        for (int iter = loop_start; iter < loop_end; iter++) {
            for (int i = start; i < end; i++) {
                for (int j = 1; j < N - 1; j++) {

                    newPhi[i][j]
                            = 0.25F * (phi[i][j - 1] + phi[i][j + 1]
                            + phi[i - 1][j] + phi[i + 1][j]);
                }
            }
            synch();
            // Update all phi values
            for (int i = start; i < end; i++) {
                for (int j = 1; j < N - 1; j++) {
                    phi[i][j] = newPhi[i][j];
                }
            }
            synch();
            if (iter % OUTPUT_FREQ == 0) {
                System.out.println("iter = " + iter);
                display.repaint();
            }
        }
    }

    public void synch() {
        try {
            newBarrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }

    static class Display extends JPanel {

        final static int WINDOW_SIZE = N * CELL_SIZE;

        Display() {

            setPreferredSize(new Dimension(WINDOW_SIZE, WINDOW_SIZE));

            JFrame frame = new JFrame("Laplace");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(this);
            frame.pack();
            frame.setVisible(true);
        }

        public void paintComponent(Graphics g) {
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    float f = phi[i][j];
                    Color c = new Color(f, 0.0F, 1.0F - f);
                    g.setColor(c);
                    g.fillRect(CELL_SIZE * i, CELL_SIZE * j,
                            CELL_SIZE, CELL_SIZE);
                }
            }
        }
    }
}
