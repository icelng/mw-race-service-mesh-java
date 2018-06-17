package com.yiran.agent.web;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpAdvanceRequestDecoder extends ChannelInboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(HttpAdvanceRequestDecoder.class);

    private ByteBuf httpContent = null;
    private ByteBuf headerParseBuf;
    private boolean isRequestLine = true;
    private boolean isHeader = false;
    private boolean isContent = false;
    private boolean isKey = true;
    private boolean isContentLen = false;
    private int remainContentSize = 0;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf in = (ByteBuf) msg;
            byte c;

            if (isRequestLine) {
                /*不关注请求行*/
                while (in.readableBytes() > 0) {
                    if (in.readByte() == '\n') {
                        isHeader = true;
                        isRequestLine = false;
                        headerParseBuf = ctx.alloc().directBuffer(1024);
                        break;
                    }
                }
            }

            if (isHeader) {
                while (in.readableBytes() > 0) {
                    c = in.readByte();
                    if (c == ':') {
                        isKey = false;
                        if ("content-length".equals(headerParseBuf.toString(CharsetUtil.UTF_8))) {
                            isContentLen = true;
                        }
                        in.readByte();  // 多读一个空格
                        headerParseBuf.clear();
                        continue;
                    }

                    if (c == '\r') {
                        continue;
                    }

                    if (c == '\n') {
                        if (isKey) {
                            /*结束头*/
                            isHeader = false;
                            isContent = true;
                            break;
                        }
                        /*为value*/
                        if (isContentLen) {
                            remainContentSize = Integer.valueOf(headerParseBuf.toString(CharsetUtil.UTF_8));
                            httpContent = ctx.alloc().directBuffer(remainContentSize);
                            isContentLen = false;
                        }
                        headerParseBuf.clear();
                        isKey = true;
                    }

                    headerParseBuf.writeByte(c);

                }
            }

            if (isContent) {
                remainContentSize -= in.readableBytes();
                in.readBytes(httpContent, in.readableBytes());
                if (remainContentSize == 0) {
                    ctx.fireChannelRead(httpContent);
                }
            }
            in.release();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if (httpContent != null) {
            httpContent.release();
        }
        headerParseBuf.release();
    }
}
