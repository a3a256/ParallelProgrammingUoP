import mpi.* ;


public class MPJMatmulB {
    public static final int N = 1024 ;
    static int me, P, B;
    static float a[];
    static float b[];
    static float c[];

    public static void main(String[] args) throws Exception {

        MPI.Init(args) ;
        
        me = MPI.COMM_WORLD.Rank() ;
        P = MPI.COMM_WORLD.Size() ;

        int block_size = N/P;

        a = new float[N*N];
        b = new float[N*N];
        c = new float[N*block_size];
        float [] rec_c = new float[N*N];

        int i, j, k;

        float sum = 0.0f;

        long startTime = System.currentTimeMillis() ;

        if(me == 0){

            for(i=0; i<N; i++){
                for(j=0; j<N; j++){
                    a[N*i + j] = (float)Math.random();
                    b[N*i + j] = (float)Math.random();
                    rec_c[N*i + j] = (float)(N*i + j);
                }
            }
        }

        MPI.COMM_WORLD.Bcast(a, 0, N * N, MPI.FLOAT, 0);
        MPI.COMM_WORLD.Bcast(b, 0, N * N, MPI.FLOAT, 0);

        for(i=0; i<N*block_size; i++){
            c[i] = 0.0f;
        }

        for(i=0; i<N; i++){
            for(j=0; j<block_size; j++){
                int globalCol = me * block_size + j;
                sum = 0.0f;
                for(k=0; k<N; k++){
                    sum += a[i*N + k]*b[k * N + globalCol];
                }
                c[i * block_size + j] += sum;
            }
        }

        if(me>0){
            MPI.COMM_WORLD.Send(c, 0, N*block_size, MPI.FLOAT, 0, 0) ;
        }else{
            for(i=0; i<N; i++){
                for(j=0; j<block_size; j++){
                    rec_c[N*i + j] = c[block_size*i + j];
                    // System.out.println("Val id in rec: " + (N*i + j) + "\nReceived: " + rec_c[N*i + j] + " original: " + c[block_size*i + j]);
                }
            }
            for(int src = 1; src<P; src++){
                // System.out.println(src);
                MPI.COMM_WORLD.Recv(c, 0, N*block_size, MPI.FLOAT, src, 0) ;
                for(i=0; i<N; i++){
                    for(j=0; j<block_size; j++){
                        int globalCol = src * block_size + j;
                        // System.out.println("Val id in rec: " + ((N*i + block_size*(src/q)) + (j+N*(src%q))));
                        rec_c[i * N + globalCol] = c[block_size*i + j];
                        // System.out.println("Received: " + rec_c[(N*i + block_size*(src/q)) + (j+N*(src%q))] + " original: " + c[block_size*i + j]);
                    }
                }
            }
        }

        long endTime = System.currentTimeMillis() ;
       
        long timeMs = endTime - startTime ;

        int count_mistakes = 0;

        if(me == 0){


            for(i = 0 ; i < N; i++) {
                for(j = 0 ; j < N ; j++) {
                    sum = 0.0f;
                    for(k = 0 ; k < N ; k++) {
                        sum += a [N * i + k] * b [N * k + j] ;
                    }
                    if (Math.abs(rec_c[N*i + j] - sum) > 1e-3){
                        count_mistakes += 1;
                    }
                }
            }
        }

        if(me == 0){

            System.out.println("MPJ matrix multiplication completed in "
                    + timeMs + " milliseconds") ;

            System.out.println("Number of mistakes: " + count_mistakes);
        }

        MPI.Finalize() ;

    }
}



// in order to compile

// run: javac -cp .:$MPJ_HOME/lib/mpj.jar MPJMatmulB.java