package nettyrpc.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import nettyrpc.protocol.RpcDecoder;
import nettyrpc.protocol.RpcEncoder;
import nettyrpc.protocol.RpcRequest;
import nettyrpc.protocol.RpcResponse;
import nettyrpc.registry.register.ZooKeeperRegister;
import nettyrpc.server.handler.RpcServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhaorunze
 */
public class RpcServer {

    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    private EventLoopGroup bossGroup;

    private EventLoopGroup workGroup;

    private String host;

    private Integer port;

    private Map<String, Object> classNameInstanceMap = new HashMap<>();

    private static ThreadPoolExecutor threadPoolExecutor;

    private ZooKeeperRegister zkRegister;

    private static String threadName = "rpcserver";

    public RpcServer(String host, Integer port){
        this.host = host;
        this.port = port;
    }

    public RpcServer(String host, Integer port,ZooKeeperRegister zkRegister){
        this.zkRegister = zkRegister;
        this.host = host;
        this.port = port;
    }

    public void start() throws InterruptedException {
        if (bossGroup == null && workGroup == null){
            bossGroup = new NioEventLoopGroup();
            workGroup = new NioEventLoopGroup();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup,workGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel sc) throws Exception {
                            sc.pipeline()
                                    .addLast(new LengthFieldBasedFrameDecoder(65536,0,4,0,0))
                                    .addLast(new RpcDecoder(RpcRequest.class))
                                    .addLast(new RpcEncoder(RpcResponse.class))
                                    .addLast(new RpcServerHandler(classNameInstanceMap));

                        }
                    })
                    .option(ChannelOption.SO_BACKLOG,128)
                    .childOption(ChannelOption.SO_KEEPALIVE,true);
            ChannelFuture future = bootstrap.bind(host,port).sync();
            logger.info("Server started on port {}",port);

            if (zkRegister != null){
                zkRegister.registry(host + ":" + port);
            }
        }
    }

    public RpcServer addService (String classPath , Object instance){
        logger.info("加载类： {}" + classPath);
        classNameInstanceMap.put(classPath,instance);
        return this;
    }

    public static void submit(Runnable task) {
        if (threadPoolExecutor == null) {
            synchronized (RpcServer.class) {
                if (threadPoolExecutor == null) {
                    threadPoolExecutor = new ThreadPoolExecutor(16, 16, 600L,
                            TimeUnit.SECONDS, new ArrayBlockingQueue<>(65536), r -> {
                                final AtomicInteger threadNumber = new AtomicInteger(1);
                                Thread t = new Thread( r,threadName + threadNumber.getAndIncrement());
                                if (t.isDaemon()){
                                    t.setDaemon(true);
                                }
                                if (t.getPriority() != Thread.NORM_PRIORITY){
                                    t.setPriority(Thread.NORM_PRIORITY);
                                }
                                return t;
                            });
                }
            }
        }
        threadPoolExecutor.submit(task);
    }
}
