
import java.awt.*;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Life {

    final static int N = 512;
    final static int CELL_SIZE = 2;
    final static int DELAY = 0;

    static int[][] state = new int[N][N];

    static int[][] sums = new int[N][N];

    static Display display = new Display();

    public static void main(String args[]) throws Exception {

        // Define initial state of Life board
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                state[i][j] = Math.random() > 0.5 ? 1 : 0;
            }
        }

        display.repaint();
//display.saveFrame(0);
        pause();

        // Main update loop.
        int iter = 0;
        while (true) {

            System.out.println("iter = " + iter++);

            // Calculate neighbour sums.
            for (int i = 0; i < N; i++) {
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

            // Update state of board values.
            for (int i = 0; i < N; i++) {
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

            display.repaint();
//display.saveFrame(iter);
            pause();
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

        void saveFrame(int frameNumber) {
            BufferedImage image = new BufferedImage(WINDOW_SIZE, WINDOW_SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            this.paint(g2d); // Paint the panel into the image
            g2d.dispose();
            try {
                File output = new File(String.format("recorded_frames/frame1_%04d.png", frameNumber));
                ImageIO.write(image, "png", output);
            } catch (IOException e) {
                e.printStackTrace();
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
