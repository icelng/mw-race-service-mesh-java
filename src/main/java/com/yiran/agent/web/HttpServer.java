package com.yiran.agent.web;

import com.yiran.LoadBalance;
import com.yiran.agent.AgentClientManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
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
        EventLoopGroup bossGroup = new EpollEventLoopGroup(1);
        EventLoopGroup workerGroup = new EpollEventLoopGroup(16);
        EventLoopGroup agentClientWorkerGroup = new EpollEventLoopGroup(16);

        AgentClientManager agentClientManager = new AgentClientManager(agentClientWorkerGroup);
        LoadBalance loadBalance = new LoadBalance(System.getProperty("etcd.url"), agentClientManager);

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(EpollServerSocketChannel.class)
                .childHandler(new HttpChannelInitService(loadBalance))
                .option(ChannelOption.SO_BACKLOG, 512)
                .option(EpollChannelOption.SO_REUSEPORT, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.TCP_NODELAY, true);
        b.bind(this.port).sync();
    }

}
