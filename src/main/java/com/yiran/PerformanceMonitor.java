package com.yiran;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;


public class PerformanceMonitor {
    private static int  availableProcessors = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
    private long lastSystemTime      = 0;
    private long lastProcessCpuTime  = 0;

    public static int getAvailableProcessors(){
        return availableProcessors;
    }

    public static double getSystemLoadAvg(){
        return ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
    }

    public static int getThreadCount(){
        return ManagementFactory.getThreadMXBean().getThreadCount();

    }

    public static void main(String args[]) throws InterruptedException {
        System.out.println("Available processors:" + getAvailableProcessors());
        while(true){
            System.out.println("Thread count:" + getThreadCount());
            Thread.sleep(50);

        }

    }

}
