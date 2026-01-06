import mpi.* ;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.sun.nio.sctp.SendFailedNotification;

public class MPJMatmulA {
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

        for(i=0; i<block_size; i++){
            for(j=0; j<N; j++){
                c[block_size*i + j] = 0.0f;
            }
        }

        for(i=0; i<block_size; i++){
            int globalRow = me * block_size + i;
            for(j=0; j<N; j++){
                sum = 0.0f;
                for(k=0; k<N; k++){
                    sum += a[globalRow*N + k]*b[k * N + j];
                }
                c[N * i + j] += sum;
            }
        }

        MPI.COMM_WORLD.Gather(c, 0, block_size*N, MPI.FLOAT,
            rec_c, block_size*me, block_size*N, MPI.FLOAT, 0
        );

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

// run: javac -cp .:$MPJ_HOME/lib/mpj.jar MPJMatmulA.java