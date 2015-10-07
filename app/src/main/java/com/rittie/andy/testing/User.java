package com.rittie.andy.testing;

import android.os.Parcel;
import android.os.Parcelable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andy Rittie on 17/08/2015.
 */
public class User implements Parcelable {
    private long id;
    private String name;
    private String email;
    private String password;
    private double avgHR;

    public User(long id, String name, String email, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.avgHR = 0;
    }
    public User(long id, String name, String email, String password, String avgHR) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.avgHR = Double.parseDouble(avgHR);
    }
    /* public List<Double> getRestingHR() {
         return restingHR;
     }

     public void setRestingHR(double restingHR) {
         this.restingHR.add(restingHR);
     }
 */
    public double calcAvg(double[] heartRates) {
        double sum = 0;
        for (int i = 0; i < heartRates.length; i++)
            sum = sum + heartRates[i];

        //calculate average value
        avgHR = sum / heartRates.length;
        return this.avgHR;
    }

    public double getAvgHR() {
        return avgHR;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel pc, int flags) {
        pc.writeLong(id);
        pc.writeString(name);
        pc.writeString(email);
        pc.writeString(password);
        pc.writeDouble(avgHR);
    }

    public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {
        public User createFromParcel (Parcel pc) {
            return new User(pc);
        }
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public User(Parcel pc){
        id = pc.readLong();
        name = pc.readString();
        email = pc.readString();
        password = pc.readString();
        avgHR= pc.readDouble();
    }
}