import mpi.* ;

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

        a = new float[block_size*N];
        b = new float[block_size*N];
        c = new float[block_size*block_size];

        int i, j, k;

        float sum = 0.0f;

        for(i=0; i<block_size; i++){
            for(j=0; j<N; j++){
                a[block_size*i + j] = (float)Math.random();
                b[block_size*i + j] = (float)Math.random();
            }
        }

        for(i=0; i<block_size; i++){
            for(j=0; j<block_size; j++){
                c[block_size*i + j] = 0.0f;
            }
        }

        long startTime = System.currentTimeMillis() ;

        for(i=0; i<block_size; i++){
            for(j=0; j<block_size; j++){
                sum = 0.0f;
                for(k=0; k<N; k++){
                    sum += a[block_size*i + k]*b[block_size*k + j];
                }
                c[block_size * i + j] += sum;
            }
        }

        if(me>0){
            MPI.COMM_WORLD.Send(c, 0, block_size*block_size, MPI.FLOAT, 0, 0) ;
        }else{
            float [] rec_c = new float[N*N];
            for(i=0; i<block_size; i++){
                for(j=0; j<block_size; j++){
                    rec_c[block_size*i + j] = c[block_size*i + j];
                }
            }
            int row_start, row_end, col_start, col_end;
            for(int src = 1; src<P; src++){
                System.out.println(src);
                MPI.COMM_WORLD.Recv(c, 0, block_size*block_size, MPI.FLOAT, src, 0) ;
                for(i=0; i<block_size; i++){
                    for(j=0; j<block_size; j++){
                        if(i == 0 && j == 0){
                            System.out.print("Row " + (block_size*i + block_size*(src/2)) + " Col " + (j+block_size*(src%2)) + " ");
                        }else if(i == 0 && j == block_size-1){
                            System.out.println("Row " + (block_size*i + block_size*(src/2)) + " Col " + (j+block_size*(src%2)));
                        }else if(i == block_size - 1 && j == 0){
                            System.out.print("Row " + (block_size*i + block_size*(src/2)) + " Col " + (j+block_size*(src%2)) + " ");
                        }else if(i == block_size-1 && j == block_size-1){
                            System.out.println("Row " + (block_size*i + block_size*(src/2)) + " Col " + (j+block_size*(src%2)));
                        }
                        rec_c[(block_size*i + block_size*(src/2)) + (j+block_size*(src%2))] = c[block_size*i + j];
                    }
                }
            }
        }

        long endTime = System.currentTimeMillis() ;
       
        long timeMs = endTime - startTime ;

        if(me == 0){

            System.out.println("MPJ matrix multiplication completed in "
                    + timeMs + " milliseconds") ;
        }

        MPI.Finalize() ;

    }
}



// in order to compile

// run: javac -cp .:$MPJ_HOME/lib/mpj.jar MPJMatmul.java
