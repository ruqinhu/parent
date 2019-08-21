package nettyrpc.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import nettyrpc.client.handler.RpcClientHandler;
import nettyrpc.client.proxy.RpcProxy;
import nettyrpc.client.proxy.ZookeeperRpcProxy;
import nettyrpc.protocol.RpcDecoder;
import nettyrpc.protocol.RpcEncoder;
import nettyrpc.protocol.RpcRequest;
import nettyrpc.protocol.RpcResponse;
import nettyrpc.registry.discovery.ZookeeperDiscovery;

import java.lang.reflect.Proxy;

/**
 * @author Zhaorunze
 */
public class RpcClient {

    private String host;
    private Integer port;

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    private ChannelFuture future ;

    public RpcClient(String host, Integer port){
        this.host = host;
        this.port = port;
    }

    public RpcClient(ZookeeperDiscovery zkDiscovery){
    }

    public void start() throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel sc) throws Exception {
                        sc.pipeline()
                                .addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0))
                                .addLast(new RpcDecoder(RpcResponse.class))
                                .addLast(new RpcEncoder(RpcRequest.class))
                                .addLast(new RpcClientHandler());
                    }
                });
        future = bootstrap.connect(host,port).sync();
    }

    public ChannelFuture getFuture(){
        return future;
    }

    /**
     * 创建用于同步调用的代理对象
     *
     * @param interfaceClass
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static  <T> T createProxy(Class<T> interfaceClass,RpcClient rpcClient) {
        // 创建动态代理对象
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new RpcProxy<T>(rpcClient)
        );
    }

    /**
     * 创建用于同步调用的代理对象
     *
     * @param interfaceClass
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T createZookeeperProxy(Class<T> interfaceClass) {
        // 创建动态代理对象
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new ZookeeperRpcProxy()
        );
    }
}
