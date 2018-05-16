package com.yiran.agent.web;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.*;
import io.netty.util.ByteProcessor;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.AppendableCharSequence;

import java.util.HashMap;
import java.util.Map;

public class HttpChannelHandler extends ChannelInboundHandlerAdapter {

    private HttpRequest request = null;
    private FullHttpResponse response = null;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;
            ByteBuf buf = content.content();
            System.out.println(buf.toString(CharsetUtil.UTF_8));
            buf.release();

            String res = "666";
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK, Unpooled.wrappedBuffer(res.getBytes("UTF-8")));
            setHeaders(response);

            ctx.write(response);
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
        System.out.println("server channelReadComplete..");
        ctx.flush();//刷新后才将数据发出到SocketChannel
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        System.out.println("server exceptionCaught..");
        ctx.close();
    }

    private static class ParameterLineParser implements ByteProcessor {
        private final byte CHAR_AND = 38;  // &符号
        private final AppendableCharSequence seq;
        private final int maxLength;
        private int size;

        public ParameterLineParser(AppendableCharSequence seq, int maxLength){
            this.seq = seq;
            this.maxLength = maxLength;
        }

        public AppendableCharSequence parse(ByteBuf buffer){
            final int oldSize = size;
            seq.reset();
            int i = buffer.forEachByte(this);
            if (i == -1) {
                size = oldSize;
                return null;
            }
            buffer.readerIndex(i + 1);
            return seq;
        }

        @Override
        public boolean process(byte value) throws Exception {
            char nextByte = (char) (value & 0xFF);
            if (nextByte == CHAR_AND) {
                return true;
            }
            if (nextByte == HttpConstants.CR) {
                return true;
            }
            if (nextByte == HttpConstants.LF) {
                return false;
            }

            if (++ size > maxLength) {
                throw newException(maxLength);
            }

            seq.append(nextByte);
            return true;
        }

        protected TooLongFrameException newException(int maxLength) {
            return new TooLongFrameException("HTTP content is larger than " + maxLength + " bytes.");
        }
    }


    private Map<String, String> parseFormData(ByteBuf content){
        AppendableCharSequence parameterSequence = new AppendableCharSequence(content.readableBytes());
        ParameterLineParser parameterLineParser = new ParameterLineParser(parameterSequence, content.readableBytes());


        AppendableCharSequence parameter = parameterLineParser.parse(content);
        if (parameter == null) {
            return null;
        }

        Map<String, String> parameterMap = new HashMap<>();

        while(parameter != null && parameter.length() > 0) {
            String[] nameValue = splitParameter(parameter);
            parameterMap.put(nameValue[0], nameValue[1]);
            parameter = parameterLineParser.parse(content);
        }

        return null;
    }

    private String[] splitParameter(AppendableCharSequence parameter) {
        int nameStart = 0;
        int nameEnd = 0;
        int valueStart = parameter.length();
        int valueEnd = parameter.length();

        for (nameEnd = nameStart;nameEnd < valueEnd;nameEnd++) {
            char ch = parameter.charAt(nameEnd);
            if (ch == '=') {
                valueStart = nameEnd + 1;
                nameEnd--;
                break;
            }
        }

        return new String[] {
                parameter.substring(nameStart, nameEnd),
                parameter.substring(valueStart, valueEnd)
        };

    }

}
