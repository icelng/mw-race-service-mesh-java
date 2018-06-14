package com.yiran.agent;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ConsumerAgentClientHandler extends SimpleChannelInboundHandler<AgentServiceBaseMsg> {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.WRITER_IDLE) {
                ctx.flush();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    protected void channelRead0(ChannelHandlerContext ctx, AgentServiceBaseMsg msg) throws Exception {
        AgentServiceRequestFuture future = AgentServiceRequestHolder.get(String.valueOf(msg.getRequestId()));
        if (future != null) {
            AgentServiceRequestHolder.remove(String.valueOf(msg.getRequestId()));
            future.done(msg);
        }
    }
}
