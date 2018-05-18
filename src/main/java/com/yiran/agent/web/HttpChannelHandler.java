package com.yiran.agent.web;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HttpChannelHandler extends SimpleChannelInboundHandler<Object> {
    private static Logger logger = LoggerFactory.getLogger(HttpChannelHandler.class);
    private static Executor executor = Executors.newFixedThreadPool(256);

    private ByteBuf contentBuf = ByteBufAllocator.DEFAULT.buffer(2048);
    private int contentLength = 0;

    private HttpRequest request = null;
    private FormDataParser formDataParser = new FormDataParser(2048);

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        if (msg instanceof HttpRequest) {
            contentLength = HttpUtil.getContentLength((HttpMessage) msg, 0);
        }
        if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;
            ByteBuf buf = content.content();
            if (buf.isReadable()) {
                contentBuf.writeBytes(buf);
            }
            if (msg instanceof LastHttpContent) {
                try{
                    Map<String, String> parameterMap = formDataParser.parse(contentBuf);
                    if(parameterMap == null) {
                        logger.error("Failed to parse form data!{}.", buf.toString(Charset.forName("utf-8")));
                        ctx.close();
                        return;
                    }

                    Channel channel = ctx.channel();
                    executor.execute(() -> {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            logger.error("", e);
                        }
                        String parameter = parameterMap.getOrDefault("parameter", null);
                        if (parameter == null) {
                            logger.error("Failed to get parameter!please check the FormDataParser! Content length:{}", this.contentLength);
                            logger.error("Content:{}", contentBuf.toString(Charset.forName("utf-8")));
                            contentBuf.release();
                            ctx.close();
                            return;
                        }
                        String res = String.valueOf(parameterMap.get("parameter").hashCode());
                        FullHttpResponse response;
                        try {
                            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK, Unpooled.wrappedBuffer(res.getBytes("UTF-8")));
                            setHeaders(response);
                            contentBuf.release();
                            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                        } catch (UnsupportedEncodingException e) {
                            logger.error("", e);
                        }

                    });

                } catch (Exception e) {
                    logger.error("", e);
                }
            }
        }
    }
    /**
     * 设置HTTP返回头信息
     */
    private void setHeaders(FullHttpResponse response) {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
//        if (HttpUtil.isKeepAlive(request)) {
//            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
//        }
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();//刷新后才将数据发出到SocketChannel
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}
