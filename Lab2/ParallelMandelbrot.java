
import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import java.io.File;

public class ParallelMandelbrot extends Thread {

    final static int N = 4096;
    final static int CUTOFF = 100;

    static int[][] set = new int[N][N];

static long [] cores = new long[2];

    public static void main(String[] args) throws Exception {

        // Calculate set
        long startTime = System.currentTimeMillis();

        ParallelMandelbrot thread0 = new ParallelMandelbrot(0);
        ParallelMandelbrot thread1 = new ParallelMandelbrot(1);

        thread0.start();
        thread1.start();

        thread0.join();
        thread1.join();

        long endTime = System.currentTimeMillis();

        System.out.println("Calculation completed in "
                + (endTime - startTime) + " milliseconds");

for(int i=0; i<2; i++){
System.out.println("Calculation for core " + i + " completed in " + cores[i] + " milliseconds\n");
}

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
        ImageIO.write(img, "PNG", new File("O:\\Desktop\\ParallelMandelbrot.png"));
    }

    int me;

    public ParallelMandelbrot(int me) {
        this.me = me;
    }

    public void run() {
        int start = 0;
        int end = 0;
        if (me == 0) {
            start = 0;
            end = N / 2;
        } else {
            start = N / 2;
            end = N;
        }
long startTime_block = System.currentTimeMillis();
        for (int i = 0; i < N; i++) {
            for (int j = start; j < end; j++) {

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
cores[me] = endTime_block - startTime_block;

    }

}
