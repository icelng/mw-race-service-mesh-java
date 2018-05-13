package com.yiran.agent;

public class AgentServiceResponse {
    private int serviceId;
    private byte methodId;
    private byte status;
    private long requestId;
    private byte returnType;
    private byte[] returnValue;

    public AgentServiceResponse(){

    }

    public AgentServiceResponse(AgentServiceRequest agentServiceRequest){
        this.serviceId = agentServiceRequest.getServiceId();
        this.methodId = agentServiceRequest.getMethodId();
        this.requestId = agentServiceRequest.getRequestId();
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

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public byte getReturnType() {
        return returnType;
    }

    public void setReturnType(int returnType) {
        this.returnType = (byte) returnType;
    }

    public byte[] getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(byte[] returnValue) {
        this.returnValue = returnValue;
    }
}
