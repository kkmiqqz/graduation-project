package org.urbcomp.startdb.simplificator.DPhullCommon;

import org.urbcomp.startdb.gpsPoint;

public class Hull {
    public static final int HULL_MAX = 10002;
    public static final int TWICE_HULL_MAX = 20002;
    public static final int THRICE_HULL_MAX = 30002;

    //operation
    public static final int PUSH_OP = 0;
    public static final int TOP_OP = 1;
    public static final int BOT_OP = 2;

//    public static PathHull left, right;
//    public static double top, bot;

    public static void hullInit(PathHull h, gpsPoint e1, gpsPoint e2) {
        h.elt[HULL_MAX] = e1;
        h.elt[h.top = HULL_MAX + 1] = e2;
        h.elt[h.bot = HULL_MAX - 1] = e2;
        h.helt[h.hp = 0] = e2;
        h.op[0] = PUSH_OP;
    }

    public static void hullPush(PathHull h, gpsPoint e) {
        h.elt[++h.top] = e;
        h.elt[--h.bot] = e;
        h.helt[++h.hp] = e;
        h.op[h.hp] = PUSH_OP;
    }

    public static void hullPopTop(PathHull h) {
        h.helt[++h.hp] = h.elt[h.top--];
        h.op[h.hp] = TOP_OP;
    }

    public static void hullPopBot(PathHull h) {
        h.helt[++h.hp] = h.elt[h.bot++];
        h.op[h.hp] = BOT_OP;
    }

    //Determine if point c is left to the line of a to b
    public static boolean leftOf(gpsPoint a, gpsPoint b, gpsPoint c) {
        //叉积，正负由sin(Θ)决定，小于180为正
        //crossProduct = (b.getX() - a.getX()) * (c.getY() - a.getY()) - (b.getY() - a.getY()) * (c.getX() - a.getX());
        double crossProduct = (b.getLongitude() - a.getLongitude()) * (c.getLatitude() - a.getLatitude())
                - (c.getLongitude() - a.getLongitude()) * (b.getLatitude() - a.getLatitude());

        return crossProduct > 0;
//        return ((a.getLongitude() - c.getLongitude()) * (b.getLatitude() - c.getLatitude())
//                >= (b.getLongitude() - c.getLongitude()) * (a.getLatitude() - c.getLatitude()));
    }

    public static boolean sgn(double a) {
        return a >= 0;
    }

    public static void hullPrint(PathHull h) {
        System.out.printf(" hull has %d points: ", h.top - h.bot);
        for (int i = h.bot; i <= h.top; i++) {
            System.out.printf(" <%.3f %.3f> ", h.elt[i].getLongitude(), h.elt[i].getLatitude());
        }
        System.out.println();
    }

    /**
     * Add p to the path hull h. Implements Melkman's convex hull algorithm
     * @param h
     * @param p
     */
    public static void hullAdd(PathHull h, gpsPoint p) {
        boolean topFlag = leftOf(h.elt[h.top], h.elt[h.top - 1], p);
        boolean botFlag = leftOf(h.elt[h.bot + 1], h.elt[h.bot], p);

        if (topFlag || botFlag) {
            while (topFlag) {
                hullPopTop(h);
                if (h.top - 1 < h.bot) break;
                topFlag = leftOf(h.elt[h.top], h.elt[h.top - 1], p);
            }
            while (botFlag) {
                hullPopBot(h);
                if (h.bot + 1 > h.top) break;
                botFlag = leftOf(h.elt[h.bot + 1], h.elt[h.bot], p);
            }
            hullPush(h, p);
        }
    }

    public static void split(PathHull h, gpsPoint e) {
        gpsPoint tmpE = null;
        int tmpO;

        while (h.hp >= 0 && ((tmpO = h.op[h.hp]) != PUSH_OP || (tmpE = h.helt[h.hp]) != e)) {
            h.hp--;
            switch (tmpO) {
                case PUSH_OP:
                    h.top--;
                    h.bot++;
                    break;
                case TOP_OP:
                    h.elt[++h.top] = tmpE;
                    break;
                case BOT_OP:
                    h.elt[--h.bot] = tmpE;
                    break;
            }
        }
    }

    public static boolean slopeSign(PathHull h, int p, int q, Homog line) {
        return sgn(line.getWw() * (h.elt[q].getLongitude() - h.elt[p].getLongitude())
                + line.getXx() * (h.elt[q].getLatitude() - h.elt[p].getLatitude()));
    }

    public static ExtremeResult findExtreme(PathHull h, Homog line, gpsPoint i, gpsPoint j) {
        int sbase, sbrk, mid, lo, m1, brk, m2, hi;
        double dist = 0.0, d1, d2;
        double xs = i.getLongitude(), ys = i.getLatitude();
        double xe = j.getLongitude(), ye = j.getLatitude();
        gpsPoint extremePoint = null;

        if ((h.top - h.bot) > 8) {
            lo = h.bot;
            hi = h.top;
            sbase = slopeSign(h, hi, lo, line) ? 1 : -1;
            do {
                brk = (lo + hi) / 2;
                sbrk = slopeSign(h, brk, brk + 1, line) ? 1 : -1;
                if (sbase == sbrk) {
                    if (sbase == (slopeSign(h, lo, brk + 1, line) ? 1 : -1)) {
                        lo = brk + 1;
                    } else {
                        hi = brk;
                    }
                }
            } while (sbase == sbrk);

            m1 = brk;
            while (lo < m1) {
                mid = (lo + m1) / 2;
                if (sbase == (slopeSign(h, mid, mid + 1, line) ? 1 : -1)) {
                    lo = mid + 1;
                } else {
                    m1 = mid;
                }
            }

            m2 = brk;
            while (m2 < hi) {
                mid = (m2 + hi) / 2;
                if (sbase == (slopeSign(h, mid, mid + 1, line) ? 1 : -1)) {
                    hi = mid;
                } else {
                    m2 = mid + 1;
                }
            }

            gpsPoint point1 = h.elt[lo];
            gpsPoint point2 = h.elt[m2];
            if(point1 != null && point2 != null) {
                double x1 = h.elt[lo].getLongitude(), y1 = h.elt[lo].getLatitude();
                double x2 = h.elt[m2].getLongitude(), y2 = h.elt[m2].getLatitude();
                d1 = Math.abs((ye - ys) * x1 - (xe - xs) * y1 + xe * ys - xs * ye) / Math.sqrt((ye - ys) * (ye - ys) + (xe - xs) * (xe - xs));
                d2 = Math.abs((ye - ys) * x2 - (xe - xs) * y2 + xe * ys - xs * ye) / Math.sqrt((ye - ys) * (ye - ys) + (xe - xs) * (xe - xs));

                //java8中，不能在三目运算符中进行赋值
                //dist[0] = (d1 >= d2) ? (e[0] = h.elt[lo], d1) : (e[0] = h.elt[m2], d2);
                if (d1 >= d2) {
                    extremePoint = h.elt[lo];
                    dist = d1;
                } else {
                    extremePoint = h.elt[m2];
                    dist = d2;
                }
            }
        } else {
            dist = 0.0;
            for (mid = h.bot; mid <= h.top; mid++) {
                gpsPoint curPoint = h.elt[mid];
                if(curPoint != null) {
                    double x1 = h.elt[mid].getLongitude(), y1 = h.elt[mid].getLatitude();
                    d1 = Math.abs((ye - ys) * x1 - (xe - xs) * y1 + xe * ys - xs * ye) / Math.sqrt((ye - ys) * (ye - ys) + (xe - xs) * (xe - xs));
                    if (d1 > dist) {
                        dist = d1;
                        extremePoint = h.elt[mid];
                    }
                }
            }
        }

        return new ExtremeResult(dist, extremePoint);
    }

    public static void main(String[] args) {
        // 示例代码，可根据需要进行测试
        PathHull h = new PathHull();
        gpsPoint a = new gpsPoint("0", 0, 0, 0);
        gpsPoint b = new gpsPoint("0", 1, 4, 4);
        gpsPoint c = new gpsPoint("0", 2, 2, 3);
        gpsPoint d = new gpsPoint("0", 2, 2, 2.5);
        gpsPoint e = new gpsPoint("0", 2, 1, 2);

        hullInit(h, a, b);
        hullAdd(h, c);
        hullAdd(h, d);
        hullAdd(h, e);
        hullPrint(h);
    }


}
