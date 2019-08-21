package nettyrpc.client.proxy;

import nettyrpc.client.RpcClient;
import nettyrpc.client.future.RpcFuture;
import nettyrpc.client.handler.RpcClientHandler;
import nettyrpc.protocol.RpcRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 同步调用代理方法
 * @author Zhaornze
 * @param <T>
 */
public class RpcProxy<T> implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(RpcProxy.class);

    private RpcClient rpcClient;

    public RpcProxy(RpcClient rpcClient){
        this.rpcClient = rpcClient;
    }

    /**
     *  在代理类调用自身方法时被调用
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @Override
    public T invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setRequestId(UUID.randomUUID().toString());
        rpcRequest.setClassName(method.getDeclaringClass().getName());
        rpcRequest.setMethodName(method.getName());
        rpcRequest.setParameterTypes(method.getParameterTypes());
        rpcRequest.setParameters(args);
        logger.info("invoke class:" + method.getDeclaringClass().getName() + " method : " + method.getName());

        RpcClientHandler rpcClientHandler = rpcClient.getFuture().channel().pipeline().get(RpcClientHandler.class);
        //这个方法会让 requestId和rpcFuture 建立联系
        RpcFuture rpcFuture = rpcClientHandler.sendRequestBySync(rpcRequest);
        return (T)rpcFuture.get();
    }
}
