package nettyrpc.server.handler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import nettyrpc.protocol.RpcRequest;
import nettyrpc.protocol.RpcResponse;
import nettyrpc.server.RpcServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private static final Logger logger = LoggerFactory.getLogger(RpcServerHandler.class);

    private final Map<String, Object> classNameInstanceMap;

    public RpcServerHandler(Map<String, Object> classNameInstanceMap){
        this.classNameInstanceMap = classNameInstanceMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {
        RpcServer.submit(() -> {
            RpcResponse response = new RpcResponse();
            response.setRequestId(request.getRequestId());
            Object result = null;
            try {
                result = handle(request);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            response.setResult(result);
            ctx.writeAndFlush(response).addListener((ChannelFutureListener) future -> logger.info("收到rpc调用并处理:  "+ response.getResult()));
        });

    }

    /**
     * 根据 clazzName加载Class可能会classNotFound 所以保存一个clazzInterfaceMap
     * @param request
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    private Object handle(RpcRequest request) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
//        logger.info("rpcRequest 请求："  + JsonUtil.objectToJson(request));
        String clazzName = request.getClassName();
        Object  instance = classNameInstanceMap.get(clazzName);

        String methodName = request.getMethodName();
        Class<?>[] classTypes =request.getParameterTypes();
        Object[] parameters = request.getParameters();

        Method method = instance.getClass().getMethod(methodName,classTypes);
        return method.invoke(instance,parameters);

//        FastClass serviceFastClass = FastClass.create(serviceClass);
////        FastMethod serviceFastMethod = serviceFastClass.getMethod(methodName, parameterTypes);
////        return serviceFastMethod.invoke(serviceBean, parameters);
//        // for higher-performance
//        int methodIndex = serviceFastClass.getIndex(methodName, parameterTypes);
//        return serviceFastClass.invoke(methodIndex, serviceBean, parameters);
    }
}
