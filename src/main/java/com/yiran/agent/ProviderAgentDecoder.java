package com.yiran.agent;

import io.netty.buffer.ByteBuf;
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

    private static final int TABLE_TYPE_1 = 0;
    private static final int TABLE_TYPE_2 = 1;
    private static final int TABLE_TYPE_4 = 2;
    private static final int HEADER_LENGTH = 16;
    private static final int PARAMETER_SIZE_ALIGN_BIT = 2;  // 参数大小对齐

    private boolean isHeader = true;
    private boolean isTable = true;
    private AgentServiceRequest agentServiceRequest;
    private int tableType;
    private int tableSize;
    private int tableBytesLength;

    private int totalParameterSize;
    private int remainSize;
    private List<Integer> parameterSizes = new ArrayList<Integer>();
    private byte[] tableCellBuf = new byte[4];

    private int lastTotalSize = 0;
    private int lastPaddingSize = 0;


    /*对客户端发来的请求进行解码*/
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (isHeader) {
            /*表示正在接受头部*/
            if (in.readableBytes() < HEADER_LENGTH){
                return;
            } else {
                /*收到完整的头部,开始解析头部,生成新的请求对象*/
                agentServiceRequest = new AgentServiceRequest();
                agentServiceRequest.setServiceId(in.readInt());  // 读取服务id
                agentServiceRequest.setMethodId(in.readByte());  // 读取方法id
                byte byteTemp = in.readByte();
                agentServiceRequest.setTwoWay(0x01 == ((byteTemp >>> 6) & 0x01));  // 是否等待
                tableType = (byteTemp >>> 4) & 0x03;
                tableSize = in.readByte();
                in.readByte();  // 请求不需要status
                agentServiceRequest.setRequestId(in.readLong());  // 请求的唯一标识
                tableBytesLength = (tableSize & 0xFF) << tableType;

                isHeader = false;
            }
        }


        /*接收参数表*/
        if(isTable) {
            if (in.readableBytes() < tableBytesLength) {
                return;
            } else {
                /*解析参数表*/
                /*解析参数表*/
                int tableCellSize = 1 << tableType;
                parameterSizes.clear();
                totalParameterSize = 0;
                try{
                    for(int i = 0;i < tableSize;i++){
                        in.readBytes(tableCellBuf, 0, tableCellSize);
                        int parameterSize = 0;
                        int parameterType = (tableCellBuf[0] >>> 4) & 0x0F;
                        if (parameterType == 0) {
                            /*如果参数类型是0，则认为是填充项*/
                            /*丢弃对齐用的填充字节*/
                            continue;
                        }
                        for(int j = 1;j < tableCellSize;j++){
                            parameterSize |= (tableCellBuf[j] & 0xFF) << ((tableCellSize - j - 1) * 8);
                        }
                        parameterSize |= (tableCellBuf[0] & 0x0F) << ((tableCellSize - 1) * 8);
                        agentServiceRequest.getParameterTypes().add(tableCellBuf[0] >>> 4);
                        parameterSizes.add(parameterSize);
                        totalParameterSize += parameterSize;
                    }
                } catch (Exception e) {
                    logger.info("-------->buf[0]:{}", tableCellBuf[0]);
                    logger.info("-------->buf[1]:{}", tableCellBuf[1]);
                    logger.info("-------->buf[2]:{}", tableCellBuf[2]);
                    logger.info("-------->buf[3]:{}", tableCellBuf[3]);
                    logger.info("-------->totalParameterSize:{}", totalParameterSize);
                    logger.info("-------->requestId:{}", agentServiceRequest.getRequestId());
                    logger.info("-------->serviceId:{}", agentServiceRequest.getServiceId());
                    logger.info("-------->tableType:{}", tableType);
                    logger.info("-------->tableCellSize:{}", tableCellSize);
                    logger.info("-------->lastTotal:{}", lastTotalSize);
                    logger.info("-------->lastPadding:{}", lastPaddingSize);
                    throw e;
                }
                /*4字节对齐*/
                remainSize = (totalParameterSize + ~(0xFFFFFFFF << PARAMETER_SIZE_ALIGN_BIT)) & (0xFFFFFFFF << PARAMETER_SIZE_ALIGN_BIT);


                isTable = false;
            }
        }


        /*接收参数*/
        if (in.readableBytes() < remainSize) {
            return;
        }
        /*解析参数*/
        List<Integer> parameterTypes = agentServiceRequest.getParameterTypes();
        List<byte[]> parameters = agentServiceRequest.getParameters();
        try{
            for(int i = 0;i < parameterTypes.size();i++){
                int paramSize = parameterSizes.get(i);
                byte[] parameterBytes = new byte[paramSize];
                in.readBytes(parameterBytes, 0, paramSize);
                parameters.add(parameterBytes);
            }
        } catch (Exception e){
            logger.info("-------->buf[0]:{}", tableCellBuf[0]);
            logger.info("-------->buf[1]:{}", tableCellBuf[1]);
            logger.info("-------->buf[2]:{}", tableCellBuf[2]);
            logger.info("-------->buf[3]:{}", tableCellBuf[3]);
            logger.info("-------->totalParameterSize:{}", totalParameterSize);
            logger.info("-------->remainSize:{}", remainSize);
            logger.info("-------->tableBytesLength:{}", tableBytesLength);
            logger.info("-------->tableType:{}", tableType);
            throw e;
        }

        /*丢弃对齐用的填充字节*/
        int paddingSize = remainSize - totalParameterSize;
        in.readBytes(paddingSize);

        lastTotalSize = totalParameterSize;
        lastPaddingSize =paddingSize;

        /*接收完毕，把处理流程交给下一个Handler*/
        out.add(agentServiceRequest);

        /*去掉引用*/
        agentServiceRequest = null;

        isHeader = true;
        isTable = true;
    }

    public static void main(String[] args){
        byte[] intBytes = new byte[4];
        intBytes[0] = 32;
        intBytes[1] = 0;
        intBytes[2] = 0;
        intBytes[3] = -1;
        int size;
        size = ((intBytes[0] << 24) | (intBytes[1] << 16) | (intBytes[2] << 8) | (intBytes[3] & 0xFF));
        System.out.println(size);

    }

}
