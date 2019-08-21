package nettyrpc.client.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import nettyrpc.client.future.RpcFuture;
import nettyrpc.protocol.RpcRequest;
import nettyrpc.protocol.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.HashMap;

/**
 * // 1. 加上注解标识，表明该 handler 是可以多个 channel 共享的
 * //@ChannelHandler.Sharable
 * 2.把requestId和rpcFuture放入map中 ，在channelRead0()方法中，根据requestId，调用rpcFuture的down()方法
 *
 * @author zhaorunze
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
    private static final Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);

    private volatile Channel channel;
    private SocketAddress remotePeer;

    // 在发送请求时 存入requestId 和 rpcFuture ，收到请求时， 调用future的down()方法
    private HashMap<String, RpcFuture> requestIdFutureMap = new HashMap<>();

    public Channel getChannel() {
        return channel;
    }

    public SocketAddress getRemotePeer() {
        return remotePeer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("channelActive");
        super.channelActive(ctx);
        this.remotePeer = this.channel.remoteAddress();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("channelRegistered");
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        String requestId = response.getRequestId();
        RpcFuture rpcFuture = requestIdFutureMap.get(requestId);
        if (rpcFuture != null){
            requestIdFutureMap.remove(requestId);
            rpcFuture.done(response);
        }
//        logger.info("客户端收到rpc结果： " + JsonUtil.objectToJson(response));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.info("client caught exception", cause);
        ctx.close();
    }

    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 把requestId和rpcFuture放入map中 ，在channelRead0()方法中，根据requestId，调用rpcFuture的down()方法
     * @param request
     * @return
     */
    public RpcFuture sendRequestBySync(RpcRequest request) {
        RpcFuture rpcFuture = new RpcFuture(request);
        requestIdFutureMap.put(request.getRequestId(), rpcFuture);
        channel.writeAndFlush(request);
        return rpcFuture;
    }

}