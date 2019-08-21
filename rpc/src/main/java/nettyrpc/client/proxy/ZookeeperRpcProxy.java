package nettyrpc.client.proxy;

import nettyrpc.client.ConnectManage;
import nettyrpc.client.future.RpcFuture;
import nettyrpc.client.handler.RpcClientHandler;
import nettyrpc.protocol.RpcRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * @author Zhaorunze
 */
public class ZookeeperRpcProxy  implements InvocationHandler {

    private static Logger logger = LoggerFactory.getLogger(ZookeeperRpcProxy.class);

    /**
     *  在代理类调用自身方法时被调用
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setRequestId(UUID.randomUUID().toString());
        rpcRequest.setClassName(method.getDeclaringClass().getName());
        rpcRequest.setMethodName(method.getName());
        rpcRequest.setParameterTypes(method.getParameterTypes());
        rpcRequest.setParameters(args);
//        System.out.println("invoke :" + method.getDeclaringClass().getName() + " method : " + method.getName());
        logger.info("invoke :" + method.getDeclaringClass().getName() + " method : " + method.getName());

        RpcClientHandler rpcClientHandler = ConnectManage.getInstance().chooseHandler();
        //这个方法会让 requestId和rpcFuture 建立联系
        RpcFuture rpcFuture = rpcClientHandler.sendRequestBySync(rpcRequest);
        return rpcFuture.get();
    }
}
