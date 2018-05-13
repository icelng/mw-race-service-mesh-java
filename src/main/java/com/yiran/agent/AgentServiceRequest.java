package com.yiran.agent;

import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.List;

public class AgentServiceRequest {

    /*Netty用的成员变量*/
    private Channel channel;

    private int serviceId;
    private byte methodId;
    private byte tableType;
    private boolean twoWay;
    private long requestId;
    private List<Integer> parameterTypes;
    private List<byte[]> parameters;

    public AgentServiceRequest() {
        parameterTypes = new ArrayList<Integer>();
        parameters = new ArrayList<byte[]>();
    }

    public AgentServiceRequest(List<Integer> parameterTypes, List<byte[]> parameters){
        this.parameterTypes = parameterTypes;
        this.parameters = parameters;
    }

    public int getServiceId() {
        return serviceId;
    }

    public void setServiceId(int serviceId) {
        this.serviceId = serviceId;
    }

    public byte getMethodId() {
        return methodId;
    }

    public void setMethodId(byte methodId) {
        this.methodId = methodId;
    }

    public boolean isTwoWay() {
        return twoWay;
    }

    public void setTwoWay(boolean twoWay) {
        this.twoWay = twoWay;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public List<Integer> getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(List<Integer> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public List<byte[]> getParameters() {
        return parameters;
    }

    public void setParameters(List<byte[]> parameters) {
        this.parameters = parameters;
    }

    public byte getTableType() {
        return tableType;
    }

    public void setTableType(byte tableType) {
        this.tableType = tableType;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
