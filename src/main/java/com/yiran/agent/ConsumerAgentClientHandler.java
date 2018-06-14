package com.yiran.agent;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ConsumerAgentClientHandler extends SimpleChannelInboundHandler<AgentServiceBaseMsg> {
    protected void channelRead0(ChannelHandlerContext ctx, AgentServiceBaseMsg msg) throws Exception {
        AgentServiceRequestFuture future = AgentServiceRequestHolder.get(String.valueOf(msg.getRequestId()));
        if (future != null) {
            AgentServiceRequestHolder.remove(String.valueOf(msg.getRequestId()));
            future.done(msg);
        }
    }
}
