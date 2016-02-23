package Datastructures;


import org.javatuples.Pair;
import org.la4j.Vector;

import java.util.*;

import org.apache.commons.collections.map.DefaultedMap;
import org.la4j.vector.dense.BasicVector;
import org.la4j.vector.functor.VectorFunction;

public class Rasterization {

    Vector zeroVector;
    Vector colors;
    public static final int rasterSize = 128;
    double rasterStep;
    double cellLength;
    Map<Vector, Pair<double[], Integer>> rasterHash;

    public Rasterization(Vector center, double cellLength){
        this.zeroVector = center.subtract(cellLength / 2);
        this.rasterStep = cellLength / rasterSize;
        this.cellLength = cellLength;
        rasterHash = new HashMap<>();
        this.rasterHash = DefaultedMap.decorate(rasterHash, new Pair<>(new double[3], 0));
    }

    public void addToRaster(Point3DRGB prgb){
        Vector p = prgb.position;
        Vector n = p.subtract(this.zeroVector);
        n.update(new VectorFunction() {
            @Override
            public double evaluate(int i, double value) {
                return Math.floor(value / rasterStep);
            }
        });

        if (n.get(0) > rasterSize || n.get(1) > rasterSize || n.get(2) > rasterSize) {
            throw new IllegalArgumentException("Vector is outside raster");
        }
        Pair<double[], Integer> value = rasterHash.get(n);
        double[] color = new double[]{prgb.color.get(0), prgb.color.get(1), prgb.color.get(2)};
        rasterHash.put(n, new Pair<>(color ,value.getValue1() + 1));
    }

    public Map<Vector, Pair<double[], Integer>> getDownSampledRaster() {
        Set<Vector> keys = rasterHash.keySet();
        Map<Vector, Pair<double[], Integer>> newRasterHash = DefaultedMap.decorate(new HashMap<>(), new Integer(0));

        for (Vector key: keys){
            Vector keyStart = key.copy();
            keyStart.update(new VectorFunction() {
                @Override
                public double evaluate(int i, double value) {
                    return value - value % 2 ;
                }
            });
            int sum = 0;
            double[] color = new double[3];
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    for (int k = 0; k < 2 ; k++) {
                        Vector keyBase = keyStart.add(new BasicVector(new double[]{i,j,k}));
                        if ( rasterHash.containsKey(keyBase)) {
                            Pair<double[], Integer> v = rasterHash.get(keyBase);
                            color = v.getValue0();
                            sum += rasterHash.get(keyBase).getValue1();
                        }
                    }
                }
            }
            if (sum > 0)
                newRasterHash.put(keyStart.divide(2), new Pair<>(color, sum));
        }
        return newRasterHash;
    }

    public Map<Vector, Pair<double[], Integer>> getRaster() {
        return rasterHash;
    }

    /**
     * @return String
     * Returns rasterized samples as String: x z y r g b weight
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Vector, Pair<double [], Integer>> entry : rasterHash.entrySet()) {
            Vector position = entry.getKey().multiply(rasterStep).add(zeroVector);
            sb.append(position.toString());
            sb.append(' ');
            for (double d : entry.getValue().getValue0()){
                sb.append(Double.toString(d));
                sb.append(' ');
            }
            sb.append(entry.getValue().getValue1().toString());
            sb.append('\n');
        }
        return sb.toString();
    }

    public Vector getColors() {
        return colors;
    }

    public void setColors(Vector colors) {
        this.colors = colors;
    }
}
