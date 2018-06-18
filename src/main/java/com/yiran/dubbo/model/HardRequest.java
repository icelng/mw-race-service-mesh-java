package com.yiran.dubbo.model;

import io.netty.buffer.ByteBuf;
import io.netty.util.Recycler;

public class HardRequest {
    private static final Recycler<HardRequest> RECYCLER = new Recycler<HardRequest>() {
        @Override
        protected HardRequest newObject(Handle<HardRequest> handle) {
            return new HardRequest(handle);
        }
    };

    private Recycler.Handle<HardRequest> recyclerHandle;

    private long reqId;
    private ByteBuf parameter;

    public HardRequest(Recycler.Handle<HardRequest> handle) {
        this.recyclerHandle = handle;
    }

    public static HardRequest get() {
        return RECYCLER.get();
    }

    public void release() {
        recyclerHandle.recycle(this);
    }

    public long getReqId() {
        return reqId;
    }

    public void setReqId(long reqId) {
        this.reqId = reqId;
    }

    public ByteBuf getParameter() {
        return parameter;
    }

    public void setParameter(ByteBuf parameter) {
        this.parameter = parameter;
    }
}
