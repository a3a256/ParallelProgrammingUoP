public class Matmul {

    public static final int N = 1024 ;

    public static void main(String [] args) {

        float [] a = new float [N * N], b = new float [N * N] ;
        float [] c = new float [N * N] ;

        for (int i = 0 ; i < N ; i++) {
            for(int j = 0 ; j < N ; j++) {
                a [N * i + j] = i + j ;
                b [N * i + j] = i - j ;
            }
        }

        long startTime = System.currentTimeMillis() ;
       
        for(int i = 0 ; i < N; i++) {
            for(int j = 0 ; j < N ; j++) {
                float sum = 0 ;
                for(int k = 0 ; k < N ; k++) {
                    sum += a [N * i + k] * b [N * k + j] ;
                }
                c [N * i + j] = sum ;
            }
        }
       
        long endTime = System.currentTimeMillis() ;
       
        long timeMs = endTime - startTime ;
       
        System.out.println("Sequential matrix multiplication completed in "
                + timeMs + " milliseconds") ;
        System.out.println("Sequential performance = " +
                ((2L * N * N * N) / (1E6 * timeMs)) + " GFLOPS") ;
    }
}
