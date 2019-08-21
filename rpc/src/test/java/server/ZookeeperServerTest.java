package server;

import client.HelloService;
import nettyrpc.registry.register.ZooKeeperRegister;
import nettyrpc.server.RpcServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZookeeperServerTest {

    private static final Logger logger = LoggerFactory.getLogger(RpcServerTest.class);

    public static void main(String[] args) {
        String host = "127.0.0.1";
        Integer port = 18866;

        ZooKeeperRegister zooKeeperRegister = new ZooKeeperRegister("127.0.0.1:2181");

        RpcServer rpcServer = new RpcServer(host, port,zooKeeperRegister);
        HelloService helloService = new HelloServiceImpl();
        rpcServer.addService("client.HelloService", helloService);
        try {
            rpcServer.start();
        } catch (Exception ex) {
            logger.error("Exception: ", ex);
        }
    }
}
