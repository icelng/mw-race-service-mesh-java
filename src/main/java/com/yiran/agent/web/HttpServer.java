package com.yiran.agent.web;

import com.yiran.LoadBalance;
import com.yiran.agent.AgentClientManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServer {
    private static Logger logger = LoggerFactory.getLogger(HttpServer.class);

    private int port;

    public HttpServer(int port){
        this.port = port;
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(16);
        EventLoopGroup agentClientWorkerGroup = new NioEventLoopGroup(16);

        AgentClientManager agentClientManager = new AgentClientManager(agentClientWorkerGroup);
        LoadBalance loadBalance = new LoadBalance(System.getProperty("etcd.url"), agentClientManager);

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new HttpChannelInitService(loadBalance))
                .option(ChannelOption.SO_BACKLOG, 512)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.TCP_NODELAY, true);
        b.bind(this.port).sync();
    }

}
