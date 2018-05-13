package com.yiran;

import com.yiran.agent.AgentServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AgentApp {
    // agent会作为sidecar，部署在每一个Provider和Consumer机器上
    // 在Provider端启动agent时，添加JVM参数-Dtype=provider -Dserver.port=30000 -Ddubbo.protocol.port=20889
    // 在Consumer端启动agent时，添加JVM参数-Dtype=consumer -Dserver.port=20000
    // 添加日志保存目录: -Dlogs.dir=/path/to/your/logs/dir。请安装自己的环境来设置日志目录。
    private static Logger logger = LoggerFactory.getLogger(AgentApp.class);


    /**
     * provider-agent没必要用web服务器
     * 两个agent之间使用基于TCP的自定义协议通信，通过Netty实现
    * */
    public static void main(String[] args) {
        String type = System.getProperty("type");
        if ("consumer".equals(type)) {
//        if (true) {
            /*consumer-agent需要受理consumer发来的请求*/
            SpringApplication.run(AgentApp.class,args);
        } else if("provider".equals(type)) {
            /*provider-agent不需要启动web服务器*/
            //new ProviderAgentBootstrap().boot();
            try {
                new AgentServer(Integer.valueOf(System.getProperty("server.port"))).run();
            } catch (Exception e) {
                logger.error(e.getLocalizedMessage());
            }
        } else {
            logger.error("Environment variable type is needed to set to provider or consumer.");
        }

    }
}
