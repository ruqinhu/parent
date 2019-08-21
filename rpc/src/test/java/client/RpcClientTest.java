package client;

import nettyrpc.client.RpcClient;

public class RpcClientTest {

    public static void main(String[] args) throws InterruptedException {
        String host = "127.0.0.1";
        Integer port = 2181;
        RpcClient rpcClient = new RpcClient(host, port);
        rpcClient.start();

        int threadNum = 10;
        final int requestNum = 100;
        Thread[] threads = new Thread[threadNum];

        long startTime = System.currentTimeMillis();
        //benchmark for sync call
        for (int i = 0; i < threadNum; ++i) {
            threads[i] = new Thread(() -> {
                for (int i1 = 0; i1 < requestNum; i1++) {
                    final HelloService syncClient = RpcClient.createProxy(HelloService.class,rpcClient);
                    String result = syncClient.hello(Integer.toString(i1));
                    if (!result.equals("Hello! " + i1))
                        System.out.print("error = " + result);
                }
            });
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        long timeCost = (System.currentTimeMillis() - startTime);
        String msg = String.format("Sync call total-time-cost:%sms, req/s=%s", timeCost, ((double) (requestNum * threadNum)) / timeCost * 1000);
        System.out.println(msg);
    }
//    public static void main(String[] args) throws InterruptedException {
//        String host = "127.0.0.1";
//        Integer port = 2181;
//        RpcClient rpcClient = new RpcClient(host,port);
//        rpcClient.start();
//
//        HelloService helloService = RpcClient.createProxy(HelloService.class,rpcClient);
//        String helloString = helloService.hello("zhaorunze");
//        System.out.println("method result：" + helloString);
//    }

//    public static void main(String[] args) throws InterruptedException {
//        String host = "127.0.0.1";
//        Integer port = 2181;
//        RpcClient rpcClient = new RpcClient(host,port);
//        ((Runnable) () -> {
//            try {
//                rpcClient.start();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }).run();
//        RpcRequest rpcRequest = new RpcRequest();
//        rpcRequest.setRequestId("1");
//        rpcRequest.setClassName(HelloService.class.getName());
//        rpcRequest.setMethodName("hello");
//        Class[] parameterTypes = new Class[1];
//        parameterTypes[0] = String.class;
//        rpcRequest.setParameterTypes(parameterTypes);
//        Object[] obj = new Object[1];
//        obj[0] = "world";
//        rpcRequest.setParameters(obj);
//        rpcClient.sendRequest(rpcRequest);
//    }


}
