package com.yiran.agent.web;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class HttpWriteStreamController extends ChannelDuplexHandler {
    private boolean isLaterWrite = false;
    private Object laterWriteMsg;
    private ChannelPromise laterWritePromise;

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isWritable() && isLaterWrite) {
            ctx.write(laterWriteMsg, laterWritePromise);
            isLaterWrite = false;
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (ctx.channel().isWritable()) {
            ctx.write(msg, promise);
        } else {
            laterWritePromise = promise;
            laterWriteMsg = msg;
            isLaterWrite = true;
        }
    }
}
