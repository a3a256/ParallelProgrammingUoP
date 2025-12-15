
import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import java.io.File;

public class MandelBrot_divide extends Thread {

    final static int N = 4096;
    final static int CUTOFF = 100;
    final static int P = 4;

    static int[][] set = new int[N][N];

    public static void main(String[] args) throws Exception {

        // Calculate set
        long startTime = System.currentTimeMillis();

        MandelBrot_divide[] threads = new MandelBrot_divide[P];

        for (int i = 0; i < P; i++) {
            threads[i] = new MandelBrot_divide(i);
            threads[i].start();
        }

        for (int i = 0; i < P; i++) {
            threads[i].join();
        }

        long endTime = System.currentTimeMillis();

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
        ImageIO.write(img, "PNG", new File("Mandelbrot_divided.png"));
    }

    int me;

    public MandelBrot_divide(int me) {
        this.me = me;
    }

    public void run() {

        int begin, end, block_size;

        block_size = N / P;
        begin = me * block_size;
        end = begin + block_size;

        for (int i = begin; i < end; i++) {
            for (int j = 0; j < N; j++) {

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
    }

}
