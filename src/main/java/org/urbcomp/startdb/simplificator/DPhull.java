package org.urbcomp.startdb.simplificator;

import org.urbcomp.startdb.gpsPoint;
import org.urbcomp.startdb.simplificator.DPhullCommon.ExtremeResult;
import org.urbcomp.startdb.simplificator.DPhullCommon.Homog;
import org.urbcomp.startdb.simplificator.DPhullCommon.Hull;
import org.urbcomp.startdb.simplificator.DPhullCommon.PathHull;

import java.util.ArrayList;
import java.util.List;



public class DPhull implements ISimplificator{
    //private final Hull hull;
    private PathHull leftPathHull, rightPathHull;
    private gpsPoint phTag;
    private Homog line;
    private List<gpsPoint> traj;
    private double top, bot;
    private double epsilon = 10;
    private double EPSILON_SQ;
    private int MAX_POINTS = 1001;
    private int TWICE_MAX_PPINTS = 2002;
    private int MAXPTS = 500002;
    private int n_split = 2;
    private gpsPoint[] splits = new gpsPoint[MAX_POINTS]; // assuming splits size, you may need to change this
                                                          // 存储简化后的关键点

    public DPhull(double epsi) {
        epsilon = epsi;
        //hull = new Hull();
        line = new Homog();
    }

    public void addSplit(gpsPoint split) { //测试：使用时间戳
        int i;
        for (i = n_split; i > 0 && splits[i - 1] != null && splits[i - 1].getTimestamp() > split.getTimestamp(); i--)
            splits[i] = splits[i - 1];
        splits[i] = split;
        n_split++;
    }

    public void build(gpsPoint i, gpsPoint j) {
        phTag =  traj.get(traj.indexOf(i) + (traj.indexOf(j) - traj.indexOf(i)) / 2);
        int index = traj.indexOf(phTag);

        if(index > 0 && index < traj.size() - 1) {
            Hull.hullInit(leftPathHull, phTag, traj.get(index - 1));
            for (int k = index - 2; k >= traj.indexOf(i); k--)
                Hull.hullAdd(leftPathHull, traj.get(k));

            Hull.hullInit(rightPathHull, phTag, traj.get(index + 1));
            for (int k = index + 2; k <= traj.indexOf(j); k++)
                Hull.hullAdd(rightPathHull, traj.get(k));
        }
    }

    public void dp(gpsPoint i, gpsPoint j) {
        double leftDist = 0, rightDist = 0;
        gpsPoint leftExtremePoint = null;
        gpsPoint rightExtremePoint = null;

        if(i == null || j == null) return;

        if (j.getTimestamp() >= i.getTimestamp()) {
            line.crossProd2cch(i, j);

            ExtremeResult leftExtremeResult = Hull.findExtreme(leftPathHull, line, i, j);
            leftDist = leftExtremeResult.getDist();
            leftExtremePoint = leftExtremeResult.getExtremePoint();

            ExtremeResult rightExtremeResult = Hull.findExtreme(rightPathHull, line, i, j);
            rightDist = rightExtremeResult.getDist();
            rightExtremePoint = rightExtremeResult.getExtremePoint();

            if (leftDist < rightDist) {
                if (rightDist > epsilon) {
                    if (phTag.equals(rightExtremePoint))
                        build(i, rightExtremePoint);
                    else
                        Hull.split(rightPathHull, rightExtremePoint);

                    addSplit(rightExtremePoint);
                    dp(i, rightExtremePoint);
                    build(rightExtremePoint, j);
                    dp(rightExtremePoint, j);
                }
            } else {
                if (leftDist > epsilon) {
                    Hull.split(leftPathHull, leftExtremePoint);
                    addSplit(leftExtremePoint);
                    dp(leftExtremePoint, j);
                    build(i, leftExtremePoint);
                    dp(i, leftExtremePoint);
                }
            }
        }
    }

    @Override
    public List<gpsPoint> simplify(List<gpsPoint> trajactory) {
        List<gpsPoint> res = new ArrayList<>();
        traj = trajactory;

//        double b = 0.0, t = 0.0;
//
//        for (int i = 0; i < n; i++) {
//            if (t < traj.get(i).getLatitude()) t = traj.get(i).getLatitude();
//            if (t < traj.get(i).getLongitude()) t = traj.get(i).getLongitude();
//            if (b > traj.get(i).getLatitude()) b = traj.get(i).getLatitude();
//            if (b > traj.get(i).getLongitude()) b = traj.get(i).getLongitude();
//        }
//
//        top = t + 3.5 + t * 0.05;
//        bot = b - 10.5 - b * 0.05;

        n_split = 2;
        splits[0] = traj.get(0);
        splits[1] = traj.get(traj.size() - 1);

        leftPathHull = new PathHull();
        rightPathHull = new PathHull();

        build(traj.get(0), traj.get(traj.size() - 1));
        Hull.hullPrint(leftPathHull);
        dp(traj.get(0), traj.get(traj.size() - 1));

        for(gpsPoint point : splits) {
            if(point == null) break;
            res.add(point);
        }

        return res;
    }

//    public static void main(String[] args) {
//        if (args.length < 4) {
//            System.out.println("<infile> <lines> <EPSILON> <result_filename>");
//        } else {
//            n = Integer.parseInt(args[2]);
//            epsilon = Double.parseDouble(args[3]);
//            EPSILON_SQ = epsilon * epsilon;
//            filename = args[1];
//            String resultFilename = args[4];
//
//            try (PrintWriter s_fp = new PrintWriter(new FileWriter(resultFilename))) {
//                init("DPhull");
//                double b = 0.0, t = 0.0;
//
//                for (int i = 0; i < n; i++) {
//                    if (t < traj.indexOf(i).getLatitude()) t = V[i].getLatitude();
//                    if (t < V[i].getLongitude()) t = V[i].getLongitude();
//                    if (b > V[i].getLatitude()) b = V[i].getLatitude();
//                    if (b > V[i].getLongitude()) b = V[i].getLongitude();
//                }
//
//                top = t + 3.5 + t * 0.05;
//                bot = b - 10.5 - b * 0.05;
//                n_split = 2;
//                splits[0] = V[0];
//                splits[1] = V[n - 1];
//
//                left = new PathHull();
//                right = new PathHull();
//
//                getPoints(filename); //List<gpsPoint>
//
//                long startTime = System.nanoTime();
//                build(V[0], V[n - 1]);
//                dp(V[0], V[n - 1]);
//                long endTime = System.nanoTime();
//                long timeUse = (endTime - startTime) / 1000;
//
//                for (int i = 0; i < n_split; i++) {
//                    s_fp.printf("%f %f\n", splits[i].getLongitude(), splits[i].getLatitude());
//                }
//                s_fp.printf("%f\n", timeUse / 1000000.0);
//            } catch (IOException e) {
//                System.out.println("open result_file error!");
//                e.printStackTrace();
//            }
//        }
//    }

    // You need to implement these methods:
    // - hullInit
    // - hullAdd
    // - crossProd2cch
    // - findExtreme
    // - split
    // - init
    // - getPoints
}

