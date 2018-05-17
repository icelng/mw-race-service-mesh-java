package com.yiran.agent.web;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HttpChannelHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(HttpChannelHandler.class);
    private static ScheduledExecutorService scheduleExecutor = Executors.newScheduledThreadPool(256);

    private HttpRequest request = null;
    private FullHttpResponse response = null;
    private FormDataParser formDataParser = new FormDataParser(2048);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        if (msg instanceof HttpContent) {
            try{
                HttpContent content = (HttpContent) msg;
                ByteBuf buf = content.content();
                Map<String, String> parameterMap = formDataParser.parse(buf);
                if(parameterMap == null) {
                    logger.error("Failed to parse form data!{}", buf.toString(Charset.forName("utf-8")));
                    ctx.close();
                    return;
                }
                buf.release();

                Thread.sleep(50);
                String res = String.valueOf(parameterMap.get("parameter").hashCode());
                try {
                    response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK, Unpooled.wrappedBuffer(res.getBytes("UTF-8")));
                } catch (UnsupportedEncodingException e) {
                }
                setHeaders(response);
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 设置HTTP返回头信息
     */
    private void setHeaders(FullHttpResponse response) {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
        response.headers().set(HttpHeaderNames.CONTENT_LANGUAGE, response.content().readableBytes());
        if (HttpUtil.isKeepAlive(request)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();//刷新后才将数据发出到SocketChannel
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        ctx.close();
    }

}
