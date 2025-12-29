import mpi.* ;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.sun.nio.sctp.SendFailedNotification;

public class MPJMatmul {
    public static final int N = 1024 ;
    static int me, P, B;
    static float a[];
    static float b[];
    static float c[];

    public static void main(String[] args) throws Exception {

        MPI.Init(args) ;
        
        me = MPI.COMM_WORLD.Rank() ;
        P = MPI.COMM_WORLD.Size() ;

        int processes_per_matrix = (int)Math.sqrt(P);

        int block_size = N/processes_per_matrix;
        int q = processes_per_matrix;

        int blockRow = me / q;
        int blockCol = me % q;

        a = new float[N*N];
        b = new float[N*N];
        c = new float[block_size*block_size];
        float [] rec_c = new float[N*N];

        int i, j, k, prev;

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
            // for(i=1; i<P; i++){
            //     MPI.COMM_WORLD.Send(a, 0, N*N, MPI.FLOAT, i, 0);
            //     MPI.COMM_WORLD.Send(b, 0, N*N, MPI.FLOAT, i, 0);
            // }
        }

        MPI.COMM_WORLD.Bcast(a, 0, N * N, MPI.FLOAT, 0);
        MPI.COMM_WORLD.Bcast(b, 0, N * N, MPI.FLOAT, 0);

        for(i=0; i<block_size; i++){
            for(j=0; j<block_size; j++){
                c[block_size*i + j] = 0.0f;
            }
        }

        for(i=0; i<block_size; i++){
            int globalRow = blockRow * block_size + i;
            for(j=0; j<block_size; j++){
                int globalCol = blockCol * block_size + j;
                sum = 0.0f;
                for(k=0; k<N; k++){
                    // sum += a[(block_size*i + ((me/q) * block_size)) + k]*b[N*k + (j+block_size*me%q)];
                    sum += a[globalRow*N + k]*b[k * N + globalCol];
                }
                c[block_size * i + j] += sum;
            }
        }


        if(me>0){
            MPI.COMM_WORLD.Send(c, 0, block_size*block_size, MPI.FLOAT, 0, 0) ;
        }else{
            for(i=0; i<block_size; i++){
                for(j=0; j<block_size; j++){
                    rec_c[N*i + j] = c[block_size*i + j];
                    // System.out.println("Val id in rec: " + (N*i + j) + "\nReceived: " + rec_c[N*i + j] + " original: " + c[block_size*i + j]);
                }
            }
            for(int src = 1; src<P; src++){
                // System.out.println(src);
                MPI.COMM_WORLD.Recv(c, 0, block_size*block_size, MPI.FLOAT, src, 0) ;
                for(i=0; i<block_size; i++){
                    int globalRow = (src/q) * block_size + i;
                    for(j=0; j<block_size; j++){
                        int globalCol = (src%q) * block_size + j;
                        // System.out.println("Val id in rec: " + ((N*i + block_size*(src/q)) + (j+N*(src%q))));
                        rec_c[globalRow * N + globalCol] = c[block_size*i + j];
                        // System.out.println("Received: " + rec_c[(N*i + block_size*(src/q)) + (j+N*(src%q))] + " original: " + c[block_size*i + j]);
                    }
                }
            }
        }

        long endTime = System.currentTimeMillis() ;
       
        long timeMs = endTime - startTime ;

        int count_mistakes = 0;

        if(me == 0){

            // for(i=0; i<N; i++){
            //     for(j=0; j<N; j++){
            //         System.out.print((N*i + j) + " - " + rec_c[N * i + j] + ", ");
            //     }
            //     System.out.print("\n");
            // }


            for(i = 0 ; i < N; i++) {
                for(j = 0 ; j < N ; j++) {
                    sum = 0.0f;
                    for(k = 0 ; k < N ; k++) {
                        sum += a [N * i + k] * b [N * k + j] ;
                    }

                    // System.out.print((N*i + j) + " - " + sum + ", ");
                    if (Math.abs(rec_c[N*i + j] - sum) > 1e-3){
                        // if(count_mistakes % 1000 == 0){
                        //     System.out.println("Mistake found at (" + N*i + "," + j +")");
                        //     System.out.println("Expected to see " + rec_c[N * i + j] + " but found " + sum);
                        // }
                        count_mistakes += 1;
                    }
                }
                // System.out.print("\n");
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
