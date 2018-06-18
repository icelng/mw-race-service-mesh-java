package com.yiran.dubbo.model;


import io.netty.util.Recycler;

import java.util.concurrent.atomic.AtomicLong;

public class Request {
    private static final Recycler<Request> RECYCLER = new Recycler<Request>() {
        @Override
        protected Request newObject(Handle<Request> handle) {
            return new Request(handle);
        }
    };

    private Recycler.Handle<Request> recyclerHandle;

    private static AtomicLong atomicLong = new AtomicLong();
    private long id;
    private String dubboVersion = "2.6.0";
    private String version = "0.0.0";
    private Object[] args;
    private boolean twoWay = true;
    private boolean event = false;

    private Object mData;

    public Request(){

    }

    public Request(Recycler.Handle<Request> handle){
        this.recyclerHandle = handle;
    }

    public static Request get(){
        return RECYCLER.get();
    }

    public void release(){
        recyclerHandle.recycle(this);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }


    public String getDubboVersion() {
        return dubboVersion;
    }

    public void setDubboVersion(String dubboVersion) {
        this.dubboVersion = dubboVersion;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public boolean isTwoWay() {
        return twoWay;
    }

    public void setTwoWay(boolean twoWay) {
        this.twoWay = twoWay;
    }

    public boolean isEvent() {
        return event;
    }

    public void setEvent(boolean event) {
        this.event = event;
    }

    public Object getData() {
        return mData;
    }

    public void setData(Object msg) {
        mData = msg;
    }

}
