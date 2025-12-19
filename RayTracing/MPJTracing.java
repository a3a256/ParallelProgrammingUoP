
/*
 * Simple Java Raytracer adapted from C++ code at:
 *
 *   http://www.scratchapixel.com/lessons/3d-basic-lessons/lesson-1-writing-a-simple-raytracer/
 *
 * which included the copyright notice below.
 */

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// A very basic raytracer example.
//
// Compile with the following command: c++ -o raytracer -O3 -Wall raytracer.cpp
//
// Copyright (c) 2011 Scratchapixel, all rights reserved
//
// * To report a bug, contact webmaster@scratchapixel.com
// * Redistribution and use in source and binary forms, with or without modification, are permitted
// * You use this software at your own risk and are responsible for any damage you cause by using it
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import mpi.* ;

import java.awt.* ;
import javax.swing.* ;

import java.util.ArrayList ;


public class MPJTracing {

    final static int IMAGE_WIDTH = 640, IMAGE_HEIGHT = 480;

    final static int N = IMAGE_WIDTH;

    final static int BLOCK_SIZE = 4 ;  // rows in block of work

    final static int NUM_BLOCKS  =  N / BLOCK_SIZE ;
    final static int BUFFER_SIZE = 1 + BLOCK_SIZE * IMAGE_WIDTH ;

    //final static int IMAGE_WIDTH = 1280, IMAGE_HEIGHT = 960;

    final static int MAX_RAY_DEPTH = 4 ;

    static RGB [] [] image = new RGB [IMAGE_WIDTH] [IMAGE_HEIGHT] ;

    final static int TAG_HELLO   = 0 ;
    final static int TAG_RESULT  = 1 ;
    final static int TAG_TASK    = 2 ;
    final static int TAG_GOODBYE = 3 ;

    public static void main(String [] args) {

        MPI.Init(args) ;
        
        int me = MPI.COMM_WORLD.Rank() ;
        int P = MPI.COMM_WORLD.Size() ;

        int numWorkers = P - 1 ;

        double [] buffer_r = new double[BUFFER_SIZE];
        double [] buffer_g = new double[BUFFER_SIZE];
        double [] buffer_b = new double[BUFFER_SIZE];
            
        if(me == 0) {  // master process - sends out work and displays results
    
            Display display = new Display() ;

            RGB vals = new RGB(0, 0, 0);

            for(int y = 0 ; y < IMAGE_HEIGHT ; y++) {
                for(int x = 0 ; x < BLOCK_SIZE ; x++) {
                    image [x] [y] = vals ;
                }
            }

            display.repaint();

            // Calculate set

            long startTime = System.currentTimeMillis();

            int nextBlockStart = 0 ;
            int numHellos = 0 ;
            int numBlocksReceived = 0 ;
            
            while(numBlocksReceived < NUM_BLOCKS || numHellos < numWorkers) {

                // Receive hello or results from any worker

                Status status = MPI.COMM_WORLD.Recv(buffer_r, 0, BUFFER_SIZE, MPI.DOUBLE,
                                            MPI.ANY_SOURCE, MPI.ANY_TAG) ;

                MPI.COMM_WORLD.Recv(buffer_g, 0, BUFFER_SIZE, MPI.DOUBLE, status.source, MPI.ANY_TAG) ;
                MPI.COMM_WORLD.Recv(buffer_b, 0, BUFFER_SIZE, MPI.DOUBLE, status.source, MPI.ANY_TAG) ;

                if(status.tag == TAG_RESULT) {  

                    // Save returned results to `set' and display

                    int resultBlockStart = (int)buffer_r [0] ;
                    for(int y = 0 ; y < IMAGE_HEIGHT ; y++) {
                        for(int x = 0 ; x < BLOCK_SIZE ; x++) {
                            // System.out.print("Val " + (1 + (IMAGE_WIDTH * x + y)) + " ");
                            vals.r = buffer_r [1 + (IMAGE_WIDTH * x + y)];
                            vals.g = buffer_g [1 + (IMAGE_WIDTH * x + y)];
                            vals.b = buffer_b [1 + (IMAGE_WIDTH * x + y)];
                            image [resultBlockStart + x] [y] = vals ;
                        }
                        System.out.print("\n");
                    }
                    numBlocksReceived++ ;
                    display.repaint() ; 
                }
                else {  // tag is TAG_HELLO
                    numHellos++ ;
                }
                
                // Send next block of work or finish tag to same worker                  
                if(nextBlockStart < N) {
                    buffer_r [0] = (double)nextBlockStart ;
                    buffer_g [0] = (double)nextBlockStart ;
                    buffer_b [0] = (double)nextBlockStart ;
                    MPI.COMM_WORLD.Send(buffer_r, 0, 1, MPI.DOUBLE, status.source, TAG_TASK) ;
                    MPI.COMM_WORLD.Send(buffer_g, 0, 1, MPI.DOUBLE, status.source, TAG_TASK) ;
                    MPI.COMM_WORLD.Send(buffer_b, 0, 1, MPI.DOUBLE, status.source, TAG_TASK) ;
                    nextBlockStart += BLOCK_SIZE ;
                    System.out.println("Sending work to " + status.source) ;
                }
                else {
                    MPI.COMM_WORLD.Send(buffer_r, 0, 0, MPI.DOUBLE, status.source, TAG_GOODBYE) ;  
                    MPI.COMM_WORLD.Send(buffer_g, 0, 0, MPI.DOUBLE, status.source, TAG_GOODBYE) ; 
                    MPI.COMM_WORLD.Send(buffer_b, 0, 0, MPI.DOUBLE, status.source, TAG_GOODBYE) ; 
                    System.out.println("Shutting down " + status.source) ;
                }                  
            }
            
            long endTime = System.currentTimeMillis();

            System.out.println("Calculation completed in " +
                               (endTime - startTime) + " milliseconds");                    
        }
        else {  // worker process
        
            // Send request to master for a first block of work

            MPI.COMM_WORLD.Send(buffer_r, 0, 0, MPI.DOUBLE, 0, TAG_HELLO) ;
            MPI.COMM_WORLD.Send(buffer_g, 0, 0, MPI.DOUBLE, 0, TAG_HELLO) ;
            MPI.COMM_WORLD.Send(buffer_b, 0, 0, MPI.DOUBLE, 0, TAG_HELLO) ;
            
            boolean done = false ;
            while(!done) {
                Status status = MPI.COMM_WORLD.Recv(buffer_r, 0, 1, MPI.DOUBLE, 0, MPI.ANY_TAG) ;
                MPI.COMM_WORLD.Recv(buffer_g, 0, 1, MPI.DOUBLE, 0, MPI.ANY_TAG) ;
                MPI.COMM_WORLD.Recv(buffer_b, 0, 1, MPI.DOUBLE, 0, MPI.ANY_TAG) ;
                
                if(status.tag == TAG_TASK) {
                    int blockStart = (int)buffer_r [0] ;

                    ArrayList<Sphere> spheres = new ArrayList<Sphere>();

                    // position, radius, surface color, reflectivity, transparency, emission color
                    spheres.add(new Sphere(new Vec3(0, -10004, -20), 10000,
                                        new RGB(0.2, 0.2, 0.2), 0, 0.0));
                    spheres.add(new Sphere(new Vec3(0, 0, -20), 4,
                                        new RGB(1.00, 0.32, 0.36), 1, 0.5));
                    spheres.add(new Sphere(new Vec3(5, -1, -15), 2,
                                        new RGB(0.90, 0.76, 0.46), 1, 0.0));
                    spheres.add(new Sphere(new Vec3(5, 0, -25), 3,
                                        new RGB(0.65, 0.77, 0.97), 1, 0.0));
                    spheres.add(new Sphere(new Vec3(-5.5, 0, -15), 3,
                                        new RGB(0.90, 0.90, 0.90), 1, 0.0));

                    // light
                    spheres.add(new Sphere(new Vec3(0, 20, -30), 3,
                    new RGB(0, 0, 0), 0, 0, new RGB(3, 3, 3)));

                    // Main rendering function. We compute a camera ray for each pixel
                    // of the image trace it and return a color. If the ray hits a sphere,
                    // we return the color of the sphere at the intersection point, else we
                    // return the background color.
                    double invWidth = 1.0 / IMAGE_WIDTH, invHeight = 1.0 / IMAGE_HEIGHT;
                    double fov = 30, aspectratio = IMAGE_WIDTH * invHeight ;
                    double angle = Math.tan(Math.PI * 0.5 * fov / 180);

                    RGB vals = new RGB(0, 0, 0);
                    // Trace rays
                    for (int y = 0; y < IMAGE_HEIGHT; ++y) {
                        for (int x = 0; x < BLOCK_SIZE; ++x) {
                            double xx = (2 * ((((double)blockStart + x) + 0.5) * invWidth) - 1) * angle * aspectratio;
                            double yy = (1 - 2 * ((y + 0.5) * invHeight)) * angle;
                            Vec3 raydir = new Vec3(xx, yy, -1.0);
                            raydir.normalize();
                            vals =
                                    trace(new Vec3(0, 0, 0), raydir, spheres, 0);

                            // System.out.println("R: " + vals.r + " G: " + vals.g + " B: " + vals.b);

                            // System.out.println("Val " + (1 + (IMAGE_WIDTH * x + y)));
                            buffer_r [1 + (IMAGE_WIDTH * x + y)] = vals.r ;
                            buffer_g [1 + (IMAGE_WIDTH * x + y)] = vals.g ;
                            buffer_b [1 + (IMAGE_WIDTH * x + y)] = vals.b ;
                        }
                        // display.repaint() ;
                    }

                    buffer_r [0] = (double)blockStart ;
                    buffer_g [0] = (double)blockStart ;
                    buffer_b [0] = (double)blockStart ;
                    MPI.COMM_WORLD.Send(buffer_r, 0, BUFFER_SIZE, MPI.DOUBLE, 0, TAG_RESULT) ;
                    MPI.COMM_WORLD.Send(buffer_g, 0, BUFFER_SIZE, MPI.DOUBLE, 0, TAG_RESULT) ;
                    MPI.COMM_WORLD.Send(buffer_b, 0, BUFFER_SIZE, MPI.DOUBLE, 0, TAG_RESULT) ;
                }
                else {  // tag is TAG_GOODBYE
                    done = true ;
                }
            }       
        }

        MPI.Finalize() ;
    }


    // This is the main trace function. It takes a ray as argument
    // (defined by its origin and direction). We test if this ray intersects
    // any of the geometry in the scene.
    // If the ray intersects an object, we compute the intersection
    // point, the normal at the intersection point, and shade this point
    // using this information.
    // Shading depends on the surface property (is it transparent,
    // reflective, diffuse).
    // The function returns a color for the ray. If the ray intersects
    // an object that is the color of the object at the intersection point,
    // otherwise it returns the background color.

    static RGB trace(Vec3 rayorig, Vec3 raydir,
                     ArrayList<Sphere> spheres, int depth) {
    
        double tnear = Double.MAX_VALUE;
        Sphere sphere = null;
        // find intersection of this ray with the sphere in the scene
        for (int i = 0; i < spheres.size(); i++) {
            Sphere s = spheres.get(i) ;
            Intersections t = s.intersect(rayorig, raydir) ;
            if (t != null) {
                double t0 = t.t0 < 0 ? t.t1 : t.t0;
                if (t0 < tnear) {
                    tnear = t0;
                    sphere = s ;
                }
            }
        }
        // if there's no intersection return black or background color
        if (sphere == null) return new RGB(2, 2, 2);
        RGB surfaceColor = new RGB(0, 0, 0) ;
                // color of the ray/surfaceof the object intersected by the ray
        Vec3 phit = new Vec3(rayorig, raydir, tnear) ;
                // point of intersection
        Vec3 nhit = new Vec3(phit, sphere.center, -1.0);
                // normal at the intersection point
        // if the normal and the view direction are not opposite to each other 
        // reverse the normal direction
        if (raydir.dot(nhit) > 0) {
            nhit.x = -nhit.x;
            nhit.y = -nhit.y;
            nhit.z = -nhit.z;
        }
        // normalize normal direction
        nhit.normalize() ;
        double bias = 1e-5;
                // add some bias to the point from which we will be tracing
        if ((sphere.transparency > 0 || sphere.reflection > 0) &&
            depth < MAX_RAY_DEPTH) {
            double IdotN = raydir.dot(nhit); // raydir.normal
            // I and N are not pointing in the same direction, so take
            // the invert
            double facingratio = Math.max(0.0, -IdotN);
            // change the mix value to tweak the effect
            double fresneleffect = mix(Math.pow(1 - facingratio, 3), 1, 0.1);
            // compute reflection direction (not need to normalize because
            // all vectors are already normalized)
            Vec3 refldir = new Vec3(raydir, nhit, -2 * raydir.dot(nhit)) ;
            RGB reflection = trace(new Vec3(phit, nhit, bias),
                                   refldir, spheres, depth + 1);
            RGB refraction = new RGB(0, 0, 0);
            // if the sphere is also transparent compute refraction
            // ray (transmission)
            if (sphere.transparency != 0) {
                double ior = 1.2, eta = 1 / ior;
                double k = 1 - eta * eta * (1 - IdotN * IdotN);
                Vec3 refrdir = new Vec3(raydir, eta,
                                        nhit, -(eta * IdotN + Math.sqrt(k)));
                refraction = trace(new Vec3(phit, nhit, -bias), refrdir,
                                   spheres, depth + 1);
            }
            // the result is a mix of reflection and refraction
            // (if the sphere is transparent)
            double fac = (1 - fresneleffect) * sphere.transparency ;
            surfaceColor.r = (reflection.r * fresneleffect + 
                              refraction.r * fac) * sphere.surfaceColor.r;
            surfaceColor.g = (reflection.g * fresneleffect + 
                              refraction.g * fac) * sphere.surfaceColor.g;
            surfaceColor.b = (reflection.b * fresneleffect + 
                              refraction.b * fac) * sphere.surfaceColor.b;
        }
        else {
            // it's a diffuse object, no need to raytrace any further
            for (int i = 0; i < spheres.size(); i++) {
                Sphere light = spheres.get(i) ;
                if (light.emissionColor != null) {
                    // this is a light
                    double transmission = 1 ;
                    Vec3 lightDirection = new Vec3(light.center, phit, -1.0);
                    lightDirection.normalize();
                    for (int j = 0; j < spheres.size(); ++j) {
                        if (i != j) {
                            Sphere sj = spheres.get(j);
                            if (sj.intersect(new Vec3(phit, nhit, bias),                                                     lightDirection) != null) {
                                transmission = 0;
                                break;
                            }
                        }
                    }
                    double fac = transmission *
                                 Math.max(0.0, nhit.dot(lightDirection)) ;
                    surfaceColor.r += sphere.surfaceColor.r *
                                      fac * light.emissionColor.r;
                    surfaceColor.g += sphere.surfaceColor.g *
                                      fac * light.emissionColor.g;
                    surfaceColor.b += sphere.surfaceColor.b *
                                      fac * light.emissionColor.b;
                }
            }
        }
        if(sphere.emissionColor != null) {
            return new RGB(surfaceColor.r + sphere.emissionColor.r,
                           surfaceColor.g + sphere.emissionColor.g,
                           surfaceColor.b + sphere.emissionColor.b);
        }
        else {
            return surfaceColor ;
        }
    }


    static double mix(double a, double b, double mix) {
        return b * mix + a * (1.0 - mix);
    }

    static class Vec3 {

        double x, y, z;
    
        Vec3(double x, double y, double z) {
            this.x = x ;
            this.y = y ;
            this.z = z ;
        }
    
        Vec3(Vec3 u, Vec3 v, double fac) {
            this.x = u.x + fac * v.x ;
            this.y = u.y + fac * v.y ;
            this.z = u.z + fac * v.z ;
        }
    
        Vec3(Vec3 u, double fac1, Vec3 v, double fac2) {
            this.x = fac1 * u.x + fac2 * v.x ;
            this.y = fac1 * u.y + fac2 * v.y ;
            this.z = fac1 * u.z + fac2 * v.z ;
        }
    
        void normalize()
        {
            double nor = x * x + y * y + z * z;
            if (nor > 0) {
                double invNor = 1 / Math.sqrt(nor);
                x *= invNor ;
                y *= invNor ;
                z *= invNor ;
            }
        }
    
        double dot(Vec3 v) {
            return x * v.x + y * v.y + z * v.z;
        }
    }
    
    static class RGB {
        double r, g, b ;
        RGB(double r, double g, double b) {
            this.r = r ;
            this.g = g ;
            this.b = b ;
        }
    }

    static class Sphere {
    
        Vec3 center ;             // position of the sphere
        double radius, radius2;   // sphere radius and radius^2
    
        RGB surfaceColor, emissionColor;  // surface color and emission (light)
    
        double transparency, reflection;  // surface transparency and reflectivity
    
        Sphere(Vec3 center,
               double radius, RGB surfaceColor, double reflection,
               double transparency, RGB emissionColor) {
            this.center = center ;
            this.radius = radius ;
            radius2 = radius * radius ;
            this.surfaceColor = surfaceColor ;
            this.emissionColor = emissionColor ;
            this.transparency = transparency;
            this.reflection = reflection ;
        }
    
        Sphere(Vec3 center,
               double radius, RGB surfaceColor, double reflection,
               double transparency) {
            this(center, radius, surfaceColor, reflection, transparency, null) ;
        }
    
        // compute a ray-sphere intersection using the geometric solution
        Intersections intersect(Vec3 rayorig, Vec3 raydir) {
    
            Vec3 l = new Vec3(center, rayorig, -1.0) ;
            double tca = l.dot(raydir) ;
            if (tca < 0) return null;
            double d2 = l.dot(l) - tca * tca;
            if (d2 > radius2) return null;
            double thc = Math.sqrt(radius2 - d2);
    
            return new Intersections(tca - thc, tca + thc) ;
        }
    }

    static class Intersections {

        double t0 ;
        double t1 ;

        Intersections(double t0, double t1) {
            this.t0 = t0 ;
            this.t1 = t1 ;
        }
    }


    static class Display extends JPanel {

        Display() {

            setPreferredSize(new Dimension(IMAGE_WIDTH, IMAGE_HEIGHT)) ;

            JFrame frame = new JFrame("Ray Tracing");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(this);
            frame.pack();
            frame.setVisible(true);
        }

        public void paintComponent(Graphics g) {
            for(int i = 0 ; i < IMAGE_WIDTH ; i++) {
                for(int j = 0 ; j < IMAGE_HEIGHT ; j++) {
                    RGB pixel = image [i] [j] ;
                    Color c ;

                    if(pixel.r == 0.0){
                        c = Color.WHITE;
                    }
                    else {
                        c = new Color((float) Math.min(1.0, pixel.r),
                                      (float) Math.min(1.0, pixel.g),
                                      (float) Math.min(1.0, pixel.b)) ;
                    }
                    g.setColor(c) ;
                    g.fillRect(i, j, 1, 1) ;
                }
            }
        }
    }
}


