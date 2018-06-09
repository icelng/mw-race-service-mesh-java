package com.yiran.dubbo;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

public class DubboConnectManager {
    private static Logger logger = LoggerFactory.getLogger(DubboConnectManager.class);


    private Bootstrap bootstrap;

    private int connectionNum;
    private Channel channels[];
    //private Channel channel;
    private Object lock = new Object();

    public DubboConnectManager(int connectionNum) {
        this.connectionNum = connectionNum;
        channels = new Channel[connectionNum];
    }

    public void connect(String host, int port) throws InterruptedException {
        initBootstrap();
        for (int i = 0;i < connectionNum;i++) {
            try {
                logger.info("Connecting to Dubbo..");
                channels[i] = bootstrap.connect(host, port).sync().channel();
            } catch (Exception e) {
                logger.error("Failed to connect to dubbo! host:{}  port:{}", host, port);
                Thread.sleep(1000);
                i--;
            }
        }
    }

    public Channel getChannel(){
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return channels[random.nextInt(connectionNum)];
    }

    public void initBootstrap() {

        EventLoopGroup eventLoopGroup = new NioEventLoopGroup(16);
        bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .channel(NioSocketChannel.class)
                .handler(new RpcClientInitializer());
    }
}
