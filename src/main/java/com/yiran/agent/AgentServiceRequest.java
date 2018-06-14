package com.yiran.agent;

import io.netty.channel.Channel;
import io.netty.util.Recycler;

import java.util.Map;


public class AgentServiceRequest extends AgentServiceBaseMsg{

    /*Netty用的成员变量*/
    private Channel channel;

    private Map<String, String> formDataMap;

    public AgentServiceRequest(){
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Map<String, String> getFormDataMap() {
        return formDataMap;
    }

    public void setFormDataMap(Map<String, String> formDataMap) {
        this.formDataMap = formDataMap;
    }
}
