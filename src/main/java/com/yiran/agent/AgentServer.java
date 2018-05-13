package com.yiran.agent;

import com.yiran.ServiceSwitcher;
import com.yiran.dubbo.DubboConnectManager;
import com.yiran.registry.EtcdRegistry;
import com.yiran.registry.IRegistry;
import com.yiran.registry.ServiceInfo;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AgentServer {
    private static Logger logger = LoggerFactory.getLogger(AgentServer.class);
    private IRegistry registry;

    private int port;

    public AgentServer(int port){
        this.port = port;
    }

    public void run() throws Exception {
        /*先与Dubbo进行连接*/
        logger.info("Connecting to Dubbo..");
        DubboConnectManager dubboConnectManager = new DubboConnectManager();
        ServiceSwitcher.setRpcClientChannel(dubboConnectManager.getChannel());

        /*启动netty服务*/
        logger.info("String netty server for agent...");
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try{
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new ProviderAgentDecoder());
                            ch.pipeline().addLast(new ProviderAgentEncoder());
                            ch.pipeline().addLast(new ProviderAgentServerHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }

        /*向etcd注册服务*/
        logger.info("Registry service!");
        registry = new EtcdRegistry(System.getProperty("etcd.url"));
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setServiceName("com.alibaba.performance.dubbomesh.provider.IHelloService");
        serviceInfo.setServiceId(1);
        serviceInfo.setParameterType(2, "Ljava/lang/String;");
        serviceInfo.setMethod(3, "hash");
        int loadLevel = Integer.valueOf(System.getProperty("load.level"));
        registry.register(serviceInfo, this.port, loadLevel);

    }

    public static void main(String[] args) throws Exception {
        try {
            logger.info("starting server");
            new AgentServer(2334).run();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}