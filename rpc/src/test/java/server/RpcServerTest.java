package server;

import client.HelloService;
import nettyrpc.server.RpcServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcServerTest {
    private static final Logger logger = LoggerFactory.getLogger(RpcServerTest.class);

    public static void main(String[] args) {
//        String serverAddress = "127.0.0.1:18866";
//        ServiceRegistry serviceRegistry = new ServiceRegistry("127.0.0.1:2181");
        String host = "127.0.0.1";
        Integer port = 2181;

        RpcServer rpcServer = new RpcServer(host, port);
        HelloService helloService = new HelloServiceImpl();
        rpcServer.addService("client.HelloService", helloService);
        try {
            rpcServer.start();
        } catch (Exception ex) {
            logger.error("Exception: ", ex);
        }
    }
}
