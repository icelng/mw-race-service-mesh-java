package com.yiran.agent;

import com.yiran.agent.web.FormDataParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义协议解码
 */
public class ProviderAgentDecoder extends ByteToMessageDecoder {
    private static Logger logger = LoggerFactory.getLogger(ProviderAgentDecoder.class);
    private ByteBufAllocator byteBufAllocator = PooledByteBufAllocator.DEFAULT;

    private static final int HEADER_LENGTH = 12;

    private boolean isHeader = true;
    private AgentServiceRequest agentServiceRequest;
    private ByteBuf parseTempBuf = UnpooledByteBufAllocator.DEFAULT.buffer(2048);
    private FormDataParser formDataParser = new FormDataParser(parseTempBuf, 2048);

    private int dataLength;



    /*对客户端发来的请求进行解码*/
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (isHeader) {
            /*表示正在接收头部*/
            if (in.readableBytes() < HEADER_LENGTH){
                return;
            } else {
                /*解析头部*/
                agentServiceRequest = new AgentServiceRequest();
                /*获取requestId*/
                agentServiceRequest.setRequestId(in.readLong());
                /*获取数据长度*/
                dataLength = in.readInt();
                agentServiceRequest.setData(byteBufAllocator.buffer(dataLength));
                isHeader = false;
            }
        }

        /*接收数据*/
        if (in.readableBytes() < dataLength) {
            return;
        }
        int writerIndexSave = in.writerIndex();
        in.writerIndex(in.readerIndex() + dataLength);
        agentServiceRequest.setFormDataMap(formDataParser.parse(in));
        in.writerIndex(writerIndexSave);
        in.readerIndex(in.readerIndex() + dataLength);
        in.readBytes(agentServiceRequest.getData(), dataLength);

        out.add(agentServiceRequest);

        /*去掉引用*/
        agentServiceRequest = null;

        isHeader = true;
    }

}
