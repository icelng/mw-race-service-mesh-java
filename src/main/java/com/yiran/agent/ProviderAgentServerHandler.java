package com.yiran.agent;

import com.yiran.ServiceSwitcher;
import com.yiran.agent.web.FormDataParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.buffer.UnpooledDirectByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;


/**
 * 对发给Provider-Agent的服务请求报文的处理
 */
public class ProviderAgentServerHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(ProviderAgentServerHandler.class);
    private Channel dubboChannel;

    public ProviderAgentServerHandler(Channel dubboChannel) {
        this.dubboChannel = dubboChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        dubboChannel.writeAndFlush(msg);
    }
}
