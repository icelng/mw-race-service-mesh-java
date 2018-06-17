package com.yiran.dubbo;

import com.yiran.ServiceSwitcher;
import com.yiran.dubbo.model.RpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.UnsupportedEncodingException;

public class RpcClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ServiceSwitcher.responseFromDubbo((ByteBuf) msg);
        }
    }

}
