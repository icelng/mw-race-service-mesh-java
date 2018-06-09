package com.yiran.agent;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class AgentClientManager {


    private EventLoopGroup workerEventLoopGroup;
    private Object lock = new Object();
    private Bootstrap bootstrap;


    public AgentClientManager(EventLoopGroup workerEventLoopGroup) {
        this.workerEventLoopGroup = workerEventLoopGroup;
    }

    public AgentClient newClient() {

        if (bootstrap == null) {
            synchronized (lock) {
                if (bootstrap == null) {
                    initBootstrap();
                }
            }
        }

        return new AgentClient(bootstrap);
    }

    private void initBootstrap() {
        bootstrap.group(this.workerEventLoopGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new ConsumerAgentDecoder());
                ch.pipeline().addLast(new ConsumerAgentEncoder());
                ch.pipeline().addLast(new ConsumerAgentClientHandler());
            }
        });
    }

}
