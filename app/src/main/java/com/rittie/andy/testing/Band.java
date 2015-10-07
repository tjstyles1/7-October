package com.rittie.andy.testing;

import android.os.Handler;

import java.util.Random;

/**
 * Created by Andy on 24/08/2015.
 */
public class Band {
    private double heartRate;
    private String typeBand;

    public String getTypeBand() {
        return typeBand;
    }

    public Band() {
        this.heartRate = 0;
        this.typeBand = "Dummy BAND";
    }

    public double getAvgHeartRate() {

                Random rand = new Random();
                this.heartRate = 85 + (5*rand.nextDouble());
                return heartRate;

    }

    public boolean connect() {
        return true;
    }
}
