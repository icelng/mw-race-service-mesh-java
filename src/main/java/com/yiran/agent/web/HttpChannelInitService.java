package com.yiran.agent.web;

import com.yiran.LoadBalance;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;

import java.util.concurrent.TimeUnit;

public class HttpChannelInitService extends ChannelInitializer<SocketChannel> {
    private LoadBalance loadBalance;

    public HttpChannelInitService (LoadBalance loadBalance) {
        this.loadBalance = loadBalance;
    }

    @Override
    protected void initChannel(SocketChannel sc)
            throws Exception {
        GlobalTrafficShapingHandler globalTrafficShapingHandler = new GlobalTrafficShapingHandler(sc.eventLoop().parent(), 2 * 512, 1024*1024);
        globalTrafficShapingHandler.configure(0);

//        sc.pipeline().addLast(new IdleStateHandler(100, 0, 0, TimeUnit.MILLISECONDS));

        sc.pipeline().addLast(globalTrafficShapingHandler);

        sc.pipeline().addLast(new HttpResponseEncoder());

        sc.pipeline().addLast(new HttpWriteStreamController());
//        sc.pipeline().addLast(new HttpRequestDecoder());

        sc.pipeline().addLast(new HttpAdvanceRequestDecoder());

        sc.pipeline().addLast(new HttpChannelHandler(loadBalance));
    }

}
