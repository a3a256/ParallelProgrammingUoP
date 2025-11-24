
import java.awt.*;
import javax.swing.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.BrokenBarrierException;

public class ParallelLife extends Thread {

    final static int N = 512;
    final static int CELL_SIZE = 2;
    final static int DELAY = 0;

    static int[][] state = new int[N][N];

    static int[][] sums = new int[N][N];

    static Display display = new Display();

    public static CyclicBarrier newBarrier = new CyclicBarrier(2);

    public static void main(String args[]) throws Exception {

        // Define initial state of Life board
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                state[i][j] = Math.random() > 0.5 ? 1 : 0;
            }
        }

        display.repaint();
        pause();

        // Main update loop.
        ParallelLife thread1 = new ParallelLife(0);
        ParallelLife thread2 = new ParallelLife(1);

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        display.repaint();
        pause();
    }

    int me;

    public ParallelLife(int thread) {
        this.me = thread;
    }

    public void run() {
        int start, end;
        if (me == 0) {
            start = 0;
            end = N / 2;
        } else {
            start = N / 2;
            end = N;
        }
        int iter = 0;
        while (true) {
            if (me == 0) {
                System.out.println("iter = " + iter++);
            }

            for (int i = start; i < end; i++) {
                for (int j = 0; j < N; j++) {

                    // find neighbours...
                    int ip = (i + 1) % N;
                    int im = (i - 1 + N) % N;
                    int jp = (j + 1) % N;
                    int jm = (j - 1 + N) % N;

                    sums[i][j]
                            = state[im][jm] + state[im][j] + state[im][jp]
                            + state[i][jm] + state[i][jp]
                            + state[ip][jm] + state[ip][j] + state[ip][jp];
                }
            }

            try {
                newBarrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }

            // Update state of board values.
            for (int i = start; i < end; i++) {
                for (int j = 0; j < N; j++) {
                    switch (sums[i][j]) {
                        case 2:
                            break;
                        case 3:
                            state[i][j] = 1;
                            break;
                        default:
                            state[i][j] = 0;
                            break;
                    }
                }
            }

            try {
                newBarrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
            if (me == 0) {
                display.repaint();
                pause();
            }
        }

    }

    static class Display extends JPanel {

        final static int WINDOW_SIZE = N * CELL_SIZE;

        Display() {

            setPreferredSize(new Dimension(WINDOW_SIZE, WINDOW_SIZE));

            JFrame frame = new JFrame("Life");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(this);
            frame.pack();
            frame.setVisible(true);
        }

        public void paintComponent(Graphics g) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, WINDOW_SIZE, WINDOW_SIZE);
            g.setColor(Color.WHITE);
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    if (state[i][j] == 1) {
                        g.fillRect(CELL_SIZE * i, CELL_SIZE * j,
                                CELL_SIZE, CELL_SIZE);
                    }
                }
            }
        }
    }

    static void pause() {
        try {
            Thread.sleep(DELAY);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
