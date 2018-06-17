package com.yiran.agent;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class AgentServiceDecoder extends ChannelInboundHandlerAdapter {
    private static final int HEADER_LENGTH = 12;

    private ByteBufAllocator byteBufAllocator = PooledByteBufAllocator.DEFAULT;
    private boolean isHeader = true;
    private int decodeIndex = 0;
    private byte[] header = new byte[HEADER_LENGTH];

    /*报文成员*/
    private long requestId;
    private int dataLength;
    private ByteBuf data;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf in = (ByteBuf) msg;
            while (true) {
                if (isHeader) {
                    while (decodeIndex < HEADER_LENGTH) {
                        if (in.readableBytes() == 0) {
                            in.release();
                            return;
                        }
                        header[decodeIndex++] = in.readByte();
                    }

                    /*接收到一个头*/
                    requestId = Bytes.bytes2long(header, 0);
                    dataLength = Bytes.bytes2int(header, 8);
                    data = ctx.alloc().directBuffer(dataLength);
                    isHeader = false;
                }

                /*接收data*/
                int needReadLen = dataLength + HEADER_LENGTH - decodeIndex;
                if (in.readableBytes() >= needReadLen) {
                    /*一个报文接收完毕*/
                    decodeIndex += needReadLen;
                    in.readBytes(data, needReadLen);
                    AgentServiceBaseMsg agentServiceBaseMsg = new AgentServiceBaseMsg();
                    agentServiceBaseMsg.setRequestId(requestId);
                    agentServiceBaseMsg.setData(data);
                    isHeader = true;
                    decodeIndex = 0;
                    ctx.fireChannelRead(agentServiceBaseMsg);
                } else {
                    decodeIndex += in.readableBytes();
                    in.readBytes(data, in.readableBytes());
                    in.release();
                    return;
                }
            }

        }
    }
}
