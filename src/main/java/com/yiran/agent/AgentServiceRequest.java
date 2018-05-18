package com.yiran.agent;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.List;

public class AgentServiceRequest {

    /*Netty用的成员变量*/
    private Channel channel;

    private long requestId;
    private ByteBuf data;

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public ByteBuf getData() {
        return data;
    }

    public void setData(ByteBuf data) {
        this.data = data;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }
}
