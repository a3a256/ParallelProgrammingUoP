
import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import java.io.File;

public class CyclicMandelbrot extends Thread {

    final static int N = 4096;
    final static int CUTOFF = 100;

    static int[][] set = new int[N][N];
    static long[] times = new long[4];

    public static void main(String[] args) throws Exception {

        // Calculate set
        long startTime = System.currentTimeMillis();

        CyclicMandelbrot thread0 = new CyclicMandelbrot(0, 0, 4);
        CyclicMandelbrot thread1 = new CyclicMandelbrot(1, 0, 4);
        CyclicMandelbrot thread2 = new CyclicMandelbrot(2, 0, 4);
        CyclicMandelbrot thread3 = new CyclicMandelbrot(3, 0, 4);

        thread0.start();
        thread1.start();
        thread2.start();
        thread3.start();

        thread0.join();
        thread1.join();
        thread2.join();
        thread3.join();

        long endTime = System.currentTimeMillis();

        System.out.println("Calculation for block " + 1 + " completed in "
                + times[0] + " milliseconds\n");

        System.out.println("Calculation for block " + 2 + " completed in "
                + times[1] + " milliseconds\n");

        System.out.println("Calculation for block " + 3 + " completed in "
                + times[2] + " milliseconds\n");

        System.out.println("Calculation for block " + 4 + " completed in "
                + times[3] + " milliseconds\n");

        System.out.println("Calculation completed in "
                + (endTime - startTime) + " milliseconds");

        // Plot image
        BufferedImage img = new BufferedImage(N, N,
                BufferedImage.TYPE_INT_ARGB);

        // Draw pixels
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {

                int k = set[i][j];

                float level;
                if (k < CUTOFF) {
                    level = (float) k / CUTOFF;
                } else {
                    level = 0;
                }
                Color c = new Color(0, level, 0);  // Green
                img.setRGB(i, j, c.getRGB());
            }
        }

        // Print file
        ImageIO.write(img, "PNG", new File("O:\\Desktop\\CyclicMandelbrot.png"));
    }

    int me, x, step;

    public CyclicMandelbrot(int iter, int x, int steps) {
        this.me = iter;
        this.x = x;
        this.step = steps;
    }

    public void run() {
        int x_start, x_coef, y_start, y_coef;
        if (x == 1) {
            x_start = me;
            x_coef = step;
            y_start = 0;
            y_coef = 1;
        } else {
            y_start = me;
            y_coef = step;
            x_start = 0;
            x_coef = 1;
        }
        long startTime_block = System.currentTimeMillis();
        for (int i = x_start; i < N; i += x_coef) {
            for (int j = y_start; j < N; j += y_coef) {

                double cr = (4.0 * i - 2 * N) / N;
                double ci = (4.0 * j - 2 * N) / N;

                double zr = cr, zi = ci;

                int k = 0;
                while (k < CUTOFF && zr * zr + zi * zi < 4.0) {

                    // z = c + z * z
                    double newr = cr + zr * zr - zi * zi;
                    double newi = ci + 2 * zr * zi;

                    zr = newr;
                    zi = newi;

                    k++;
                }

                set[i][j] = k;
            }
        }
        long endTime_block = System.currentTimeMillis();
        times[me] = endTime_block - startTime_block;
    }

}
