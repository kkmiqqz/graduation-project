package org.urbcomp.startdb.simplificator;

import org.urbcomp.startdb.gpsPoint;
import org.urbcomp.startdb.simplificator.DPhullCommon.PathHull;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static com.sun.deploy.util.SessionState.init;
import static org.urbcomp.startdb.simplificator.DPhullCommon.Hull.hullAdd;
import static org.urbcomp.startdb.simplificator.DPhullCommon.Hull.hullInit;

public class DPhull {
    static PathHull left, right;
    static gpsPoint PHtag;
    static String filename;
    static double top, bot;
    static int n;
    static double EPSILON, EPSILON_SQ;
    static int n_split;
    static gpsPoint[] splits = new gpsPoint[100]; // assuming splits size, you may need to change this
    static gpsPoint[] V; // This needs to be initialized properly elsewhere in the code

    public static void addSplit(gpsPoint split) {
        int i;
        for (i = n_split; splits[i - 1].getLongitude() > split.getLongitude(); i--)
            splits[i] = splits[i - 1];
        splits[i] = split;
        n_split++;
    }

    public static void build(gpsPoint i, gpsPoint j) {
        gpsPoint k;
        PHtag = new gpsPoint(i.getLongitude() + (j.getLongitude() - i.getLongitude()) / 2, i.getLatitude() + (j.getLatitude() - i.getLatitude()) / 2);

        hullInit(left, PHtag, new gpsPoint(PHtag.getLongitude() - 1, PHtag.getLatitude()));
        for (k = new gpsPoint(PHtag.getLongitude() - 2, PHtag.getLatitude()); k.getLongitude() >= i.getLongitude(); k = new gpsPoint(k.getLongitude() - 1, k.getLatitude()))
            hullAdd(left, k);

        hullInit(right, PHtag, new gpsPoint(PHtag.getLongitude() + 1, PHtag.getLatitude()));
        for (k = new gpsPoint(PHtag.getLongitude() + 2, PHtag.getLatitude()); k.getLongitude() <= j.getLongitude(); k = new gpsPoint(k.getLongitude() + 1, k.getLatitude()))
            hullAdd(right, k);
    }

    public static void dp(gpsPoint i, gpsPoint j) {
        double ld = 0, rd = 0;
        gpsPoint le = null, re = null;

        if (j.getLongitude() - i.getLongitude() > 1) {
            double[] l = crossProd2cch(i, j);

            findExtreme(left, l, le, ld, i, j);
            findExtreme(right, l, re, rd, i, j);

            if (ld < rd) {
                if (rd > EPSILON) {
                    if (PHtag.equals(re))
                        build(i, re);
                    else
                        split(right, re);

                    addSplit(re);
                    dp(i, re);
                    build(re, j);
                    dp(re, j);
                }
            } else if (ld > EPSILON) {
                split(left, le);
                addSplit(le);
                dp(le, j);
                build(i, le);
                dp(i, le);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("<infile> <lines> <EPSILON> <result_filename>");
        } else {
            n = Integer.parseInt(args[2]);
            EPSILON = Double.parseDouble(args[3]);
            EPSILON_SQ = EPSILON * EPSILON;
            filename = args[1];
            String resultFilename = args[4];

            try (PrintWriter s_fp = new PrintWriter(new FileWriter(resultFilename))) {
                init("DPhull");
                double b = 0.0, t = 0.0;

                for (int i = 0; i < n; i++) {
                    if (t < V[i].getLatitude()) t = V[i].getLatitude();
                    if (t < V[i].getLongitude()) t = V[i].getLongitude();
                    if (b > V[i].getLatitude()) b = V[i].getLatitude();
                    if (b > V[i].getLongitude()) b = V[i].getLongitude();
                }

                top = t + 3.5 + t * 0.05;
                bot = b - 10.5 - b * 0.05;
                n_split = 2;
                splits[0] = V[0];
                splits[1] = V[n - 1];

                left = new PathHull();
                right = new PathHull();

                getPoints(filename); //List<gpsPoint>

                long startTime = System.nanoTime();
                build(V[0], V[n - 1]);
                dp(V[0], V[n - 1]);
                long endTime = System.nanoTime();
                long timeUse = (endTime - startTime) / 1000;

                for (int i = 0; i < n_split; i++) {
                    s_fp.printf("%f %f\n", splits[i].getLongitude(), splits[i].getLatitude());
                }
                s_fp.printf("%f\n", timeUse / 1000000.0);
            } catch (IOException e) {
                System.out.println("open result_file error!");
                e.printStackTrace();
            }
        }
    }

    // You need to implement these methods:
    // - hullInit
    // - hullAdd
    // - crossProd2cch
    // - findExtreme
    // - split
    // - init
    // - getPoints
}

