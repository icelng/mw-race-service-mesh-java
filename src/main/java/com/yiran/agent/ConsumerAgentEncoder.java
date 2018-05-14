package com.yiran.agent;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ConsumerAgentEncoder extends MessageToByteEncoder<AgentServiceRequest> {
    private static Logger logger = LoggerFactory.getLogger(ConsumerAgentEncoder.class);

    private static final int PARAMETER_TABLE_TYPE_1 = 0;
    private static final int PARAMETER_TABLE_TYPE_2 = 1;
    private static final int PARAMETER_TABLE_TYPE_4 = 2;
    private static final int PARAMETER_SIZE_ALIGN_BIT = 2;  // 参数大小对齐

    private static byte[] bytesTemp = new byte[4];



    protected void encode(ChannelHandlerContext ctx, AgentServiceRequest msg, ByteBuf out) throws Exception {
        byte byteOutTemp;


        /*写入头*/
        out.writeInt(msg.getServiceId());  // 服务id
        out.writeByte(msg.getMethodId());  // 方法id
        int tableType = msg.getTableType() & 0x03;
        byteOutTemp = (byte) (0x80 | ((msg.isTwoWay()? 1 : 0) << 6) | (tableType << 4));
        out.writeByte(byteOutTemp);
        /*参数表4字节对齐*/
        int tableSize = (msg.getParameters().size() + ~(0xFFFFFFFF << (PARAMETER_SIZE_ALIGN_BIT - tableType))) & (0xFFFFFFFF << (PARAMETER_SIZE_ALIGN_BIT - tableType));
        out.writeByte(tableSize);  // tableSize 参数表长
        out.writeByte(0x00);  // status字段
        out.writeLong(msg.getRequestId());

        List<byte[]> parameters = msg.getParameters();
        List<Integer> parameterTypes = msg.getParameterTypes();

        /*对参数表进行编码*/
        int totalParameterSize = 0;
        switch (tableType) {
            case PARAMETER_TABLE_TYPE_1:
                /*类型4bit 参数长度4bit*/
                for(int i = 0;i < parameters.size();i++){
                    int parameterSize = parameters.get(i).length;
                    totalParameterSize += parameterSize;
                    bytesTemp[0] = (byte) (((parameterTypes.get(i) & 0x0F) << 4) | ((parameterSize) & 0x0F));
                    out.writeByte(bytesTemp[0]);
                }
                break;
            case PARAMETER_TABLE_TYPE_2:
                /*类型4bit 参数长度12bit*/
                for(int i = 0;i < parameters.size();i++){
                    int parameterSize = parameters.get(i).length;
                    totalParameterSize += parameterSize;
                    bytesTemp[0] = (byte) (((parameterTypes.get(i) & 0x0F) << 4) | ((parameterSize >>> 8) & 0x0F));
                    bytesTemp[1] = (byte) (parameterSize & 0xFF);
                    out.writeBytes(bytesTemp, 0, 2);
                }
                break;
            case PARAMETER_TABLE_TYPE_4:
                /*类型4bit 参数长度28bit*/
                for(int i = 0;i < parameters.size();i++){
                    int parameterSize = parameters.get(i).length;

                    totalParameterSize += parameterSize;
                    bytesTemp[0] = (byte) (((parameterTypes.get(i) & 0x0F) << 4) | ((parameterSize >>> 24) & 0x0F));
                    bytesTemp[1] = (byte) ((parameterSize >>> 16) & 0xFF);
                    bytesTemp[2] = (byte) ((parameterSize >>> 8) & 0xFF);
                    bytesTemp[3] = (byte) (parameterSize & 0xFF);
                    out.writeBytes(bytesTemp, 0, 4);
                }
                break;

            default:
                /*抛异常才对*/
                /*或者报告错误*/
                System.out.println("没有此类型的参数表类型:" + tableType);

        }
        /*计算填充大小*/
        int paddingSize = (tableSize << tableType) - (parameters.size() << tableType);
        for(int i = 0;i < paddingSize;i++){
            out.writeByte(0x00);
        }

        /*对参数进行编码*/
        paddingSize = ((totalParameterSize + ~(0xFFFFFFFF << PARAMETER_SIZE_ALIGN_BIT)) & (0xFFFFFFFF << PARAMETER_SIZE_ALIGN_BIT)) - totalParameterSize;
        logger.info("-----{}:decoder------>requestId:{}  paramSize:{}  paddingSize:{}", msg.getRequestId(), totalParameterSize, paddingSize);
        for (int i = 0;i < parameters.size();i++){
            out.writeBytes(parameters.get(i));
        }
        for(int i = 0;i < paddingSize;i++){
            out.writeByte(0);
        }

    }
}
