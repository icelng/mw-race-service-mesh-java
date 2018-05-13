package com.yiran.agent;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class ConsumerAgentDecoder extends ByteToMessageDecoder {
    private static final int HEADER_LENGTH = 20;
    private static final int PARAMETER_SIZE_ALIGN_BIT = 2;  // 参数大小对齐


    private AgentServiceResponse agentServiceResponse;
    private int returnValueSize;
    private int remainSize;
    private boolean isHeader = true;

    /*接收响应解码*/
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        /*接收头部*/
        if (isHeader) {
            if(in.readableBytes() <  HEADER_LENGTH){
                return;
            }
            /*解析头部*/
            agentServiceResponse = new AgentServiceResponse();
            agentServiceResponse.setServiceId(in.readInt());  // 获取服务id
            agentServiceResponse.setMethodId(in.readByte());  // 获取方法id
            in.readByte();  // 不关心req/res  toway  tableType
            in.readByte();  // 不关心tableSize
            agentServiceResponse.setStatus(in.readByte());  // 获取相应状态
            agentServiceResponse.setRequestId(in.readLong());  // 获取请求id
            int returnValueInfo = in.readInt();  // 返回值信息
            agentServiceResponse.setReturnType((byte) ((returnValueInfo >> 28) & 0x0F));
            returnValueSize = returnValueInfo & 0x0FFFFFFF;
            /*对返回值大小进行对齐，其为剩下需要接收的字节数*/
            remainSize = (returnValueSize + ~(0xFFFFFFFF << PARAMETER_SIZE_ALIGN_BIT)) & (0xFFFFFFFF << PARAMETER_SIZE_ALIGN_BIT);

            isHeader = false;
        }

        /*接收返回值*/
        if (in.readableBytes() < remainSize) {
            return;
        }
        byte[] returnValue = new byte[returnValueSize];
        in.readBytes(returnValue, 0 ,returnValueSize);
        agentServiceResponse.setReturnValue(returnValue);

        /*计算对齐用的填充大小,并且discard*/
        int paddingSize = remainSize - returnValueSize;
        in.readBytes(paddingSize);

        /*添加到out，解码得到response*/
        out.add(agentServiceResponse);
        /*释放引用*/
        agentServiceResponse = null;

        /*下一个字节开始是头*/
        isHeader = true;
    }
}
