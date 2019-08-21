package client;

import nettyrpc.client.RpcClient;
import nettyrpc.registry.discovery.ZookeeperDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZookeeperClientTest {

    private static Logger logger = LoggerFactory.getLogger(ZookeeperClientTest.class);

    public static void main(String[] args) throws InterruptedException {

        ZookeeperDiscovery zkRegister = new ZookeeperDiscovery("127.0.0.1:2181");

        RpcClient rpcClient = new RpcClient(zkRegister);

        int threadNum = 10;
        final int requestNum = 1000;
        Thread[] threads = new Thread[threadNum];

        Thread.sleep(3000);
        long startTime = System.currentTimeMillis();
        //benchmark for sync call
        for(int i = 0;i < threadNum;i ++){
            threads[i] = new Thread(() -> {
                for(int j = 0;j<requestNum;j++){
                    final HelloService syncClient = rpcClient.createZookeeperProxy(HelloService.class);
                    String result = syncClient.hello(Integer.toString(j));
                    if (result.equals("Hello! " + j)){
                        System.out.print("success = " + result);
                    }
                }
            });
            threads[i].start();
        }
        for (int i =0;i <threads.length;i++){
            threads[i].join();
        }
        long timeCost = (System.currentTimeMillis() - startTime);
        String msg = String.format("Sync call total-time-cost:%sms, req/s=%s", timeCost, ((double) (requestNum * threadNum)) / timeCost * 1000);
        logger.info(msg);
//        System.out.println(msg);
    }
}
