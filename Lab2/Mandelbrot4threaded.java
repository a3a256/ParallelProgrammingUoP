
import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import java.io.File;

public class Mandelbrot4threaded extends Thread {

    final static int N = 4096;
    final static int CUTOFF = 100;

    static int[][] set = new int[N][N];
    static long[] times = new long[4];

    public static void main(String[] args) throws Exception {

        // Calculate set
        long startTime = System.currentTimeMillis();

//        Mandelbrot4threaded thread0 = new Mandelbrot4threaded(0, N / 4, -1, -1, 1);
//        Mandelbrot4threaded thread1 = new Mandelbrot4threaded(N / 4, N / 2, -1, -1, 2);
//        Mandelbrot4threaded thread2 = new Mandelbrot4threaded(N / 2, (3 * N) / 4, -1, -1, 3);
//        Mandelbrot4threaded thread3 = new Mandelbrot4threaded((3 * N) / 4, N, -1, -1, 4);
//
//        thread0.start();
//        thread1.start();
//        thread2.start();
//        thread3.start();
//
//        thread0.join();
//        thread1.join();
//        thread2.join();
//        thread3.join();

//        Mandelbrot4threaded thread0 = new Mandelbrot4threaded(-1, -1, 0, N / 4, 1);
//        Mandelbrot4threaded thread1 = new Mandelbrot4threaded(-1, -1, N / 4, N / 2, 2);
//        Mandelbrot4threaded thread2 = new Mandelbrot4threaded(-1, -1, N / 2, (3 * N) / 4, 3);
//        Mandelbrot4threaded thread3 = new Mandelbrot4threaded(-1, -1, (3 * N) / 4, N, 4);
//
//        thread0.start();
//        thread1.start();
//        thread2.start();
//        thread3.start();
//
//        thread0.join();
//        thread1.join();
//        thread2.join();
//        thread3.join();

        Mandelbrot4threaded thread0 = new Mandelbrot4threaded(0, N/2, 0, N / 2, 1);
        Mandelbrot4threaded thread1 = new Mandelbrot4threaded(N/2, N, 0, N / 2, 2);
        Mandelbrot4threaded thread2 = new Mandelbrot4threaded(0, N/2, N / 2, N, 3);
        Mandelbrot4threaded thread3 = new Mandelbrot4threaded(N/2, N, N / 2, N, 4);

        thread0.start();
        thread1.start();
        thread2.start();
        thread3.start();

        thread0.join();
        thread1.join();
        thread2.join();
        thread3.join();

        long endTime = System.currentTimeMillis();

        System.out.println("Calculation completed in "
                + (endTime - startTime) + " milliseconds");

        System.out.println("Calculation for block " + 1 + " completed in "
                + times[0] + " milliseconds\n");

        System.out.println("Calculation for block " + 2 + " completed in "
                + times[1] + " milliseconds\n");

        System.out.println("Calculation for block " + 3 + " completed in "
                + times[2] + " milliseconds\n");

        System.out.println("Calculation for block " + 4 + " completed in "
                + times[3] + " milliseconds\n");

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
        ImageIO.write(img, "PNG", new File("ParallelMandelbrot_4blocks.png"));
    }

    int x_start, x_end, y_start, y_end, block;

    public Mandelbrot4threaded(int i_start, int i_end, int j_start, int j_end, int block_number) {
        this.x_start = i_start;
        this.x_end = i_end;
        this.y_start = j_start;
        this.y_end = j_end;
        this.block = block_number;
    }

    public void run() {
        if (x_start == -1 || x_end == -1) {
            x_start = 0;
            x_end = N;
        }
        if (y_start == -1 || y_end == -1) {
            y_start = 0;
            y_end = N;
        }
        long startTime_block = System.currentTimeMillis();
        for (int i = x_start; i < x_end; i++) {
            for (int j = y_start; j < y_end; j++) {

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
        times[block - 1] = endTime_block - startTime_block;
    }

}
