public class ParallelMatmulA extends Thread{

    public static final int N = 1024 ;

    static float[] a = new float[N*N];
    static float[] b = new float[N*N];
    static float[] c = new float[N*N];

    final static int P = 8;

    public static void main(String [] args) throws Exception{

        int i, j;

        for (i = 0 ; i < N ; i++) {
            for(j = 0 ; j < N ; j++) {
                a [N * i + j] = i + j ;
                b [N * i + j] = i - j ;
            }
        }

        long startTime = System.currentTimeMillis() ;

        ParallelMatmulA [] threads = new ParallelMatmulA [P] ;

        for(i=0; i<P; i++){
            threads[i] = new ParallelMatmulA(i);
            threads[i].start();
        }

        for(i=0; i<P; i++){
            threads[i].join();
        }
       
        long endTime = System.currentTimeMillis() ;
       
        long timeMs = endTime - startTime ;
       
        System.out.println("Sequential matrix multiplication completed in "
                + timeMs + " milliseconds") ;
        System.out.println("Sequential performance = " +
                ((2L * N * N * N) / (1E6 * timeMs)) + " GFLOPS") ;
    }

    int row;

    public ParallelMatmulA(int row){
        this.row = row;
    }

    public void run(){
        int step = N/P;
        int start_row = row*step;
        int end_row = start_row+step;
        int i, j;
        for(i = start_row ; i < end_row; i++) {
            for(j = 0 ; j < N ; j++) {
                float sum = 0 ;
                for(int k = 0 ; k < N ; k++) {
                    sum += a [N * i + k] * b [N * k + j] ;
                }
                c [N * i + j] = sum ;
            }
        }
    }
}
