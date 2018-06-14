package com.yiran.agent;

import io.netty.buffer.ByteBuf;

public class AgentServiceBaseMsg {
    private long requestId;
    private ByteBuf data;

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public ByteBuf getData() {
        return data;
    }

    public void setData(ByteBuf data) {
        this.data = data;
    }
}
