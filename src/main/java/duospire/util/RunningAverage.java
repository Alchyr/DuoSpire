package duospire.util;

import java.util.Arrays;

public class RunningAverage {
    private final double[] slots;
    private int pos;
    private int lim;
    private double avg;

    public RunningAverage(int slotCount) {
        this.slots = new double[slotCount];
        this.pos = 0;
        this.avg = 0;
    }

    public void init(double value) {
        while (this.pos < this.slots.length)
            this.slots[this.pos++] = value;
        this.avg = value;
        lim = this.slots.length;
    }
    public void clear(double value) {
        Arrays.fill(this.slots, value);
        pos = 0;
        lim = 0;
        this.avg = value;
    }

    public void add(double value) {
        this.slots[this.pos %= this.slots.length] = value;
        ++pos;
        if (pos > lim)
            lim = pos;
        this.avg = calcAvg();
    }

    public double avg() {
        return avg;
    }

    public double calcAvg() {
        double sum = 0;
        for (int i = 0; i < lim; ++i) {
            sum += slots[i];
        }
        return sum / this.slots.length;
    }
}