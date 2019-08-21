package nettyrpc.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import nettyrpc.client.handler.RpcClientHandler;
import nettyrpc.client.handler.RpcClientInitializer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Zhaorunze
 */
public class ConnectManage {

    private volatile static ConnectManage connectManage;

    private static NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup(4);

    private CopyOnWriteArrayList<RpcClientHandler> connectedHandel = new CopyOnWriteArrayList<>();

    private Map<InetSocketAddress,RpcClientHandler> connectedServerNode = new ConcurrentHashMap<>();

    private AtomicInteger roundRobin = new AtomicInteger(0);

    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16, 600L,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(65535), r -> {
                final AtomicInteger threadNumber = new AtomicInteger(1);
                Thread t = new Thread( r,"connectmanage" + threadNumber.getAndIncrement());
                if (t.isDaemon()){
                    t.setDaemon(true);
                }
                if (t.getPriority() != Thread.NORM_PRIORITY){
                    t.setPriority(Thread.NORM_PRIORITY);
                }
                return t;
            });

    private ConnectManage(){

    }

    public static ConnectManage getInstance(){
        if (connectManage == null){
            synchronized (ConnectManage.class){
                if (connectManage == null){
                    connectManage = new ConnectManage();
                }
            }
        }
        return connectManage;
    }

    public RpcClientHandler chooseHandler(){
        int size = connectedHandel.size();
        while (size==0){
            //TODO
        }
        int index = (roundRobin.addAndGet(1)+size)%size;
        System.out.println("chooseHandler : " +  size + " index : "  + index);
        return connectedHandel.get(index);
    }

    public void updateConnectServer(List<String> allServerAddress){
        if (allServerAddress != null){
            if (allServerAddress.size() != 0){
                //更新节点缓存
                HashSet<InetSocketAddress> newAllServerAddress = new HashSet<>();
                for(String str:allServerAddress){
                    String[] strs = str.split(":");
                    if (strs.length == 2){
                        String host = strs[0];
                        Integer port = Integer.parseInt(strs[1]);
                        InetSocketAddress socketAddress = new InetSocketAddress(host,port);
                        newAllServerAddress.add(socketAddress);
                    }
                }

                //添加新的服务节点
                for(InetSocketAddress inetSocketAddress:newAllServerAddress){
                    if (!connectedServerNode.containsKey(inetSocketAddress)){
                        connectServerNode(inetSocketAddress);
                    }
                }

                //删除下线节点
                for(RpcClientHandler oldRpcClientHandler:connectedHandel){
                    SocketAddress socketAddress = oldRpcClientHandler.getRemotePeer();
                    if (!newAllServerAddress.contains(socketAddress)){
                        System.out.println("删除下线节点");
                        RpcClientHandler handler = connectedServerNode.get(socketAddress);
                        if (handler != null){
                            handler.close();
                        }
                        connectedHandel.remove(handler);
                        connectedServerNode.remove(socketAddress);
                    }
                }
            }else {
                System.out.println("没有可用节点");
                for (RpcClientHandler rpcClientHandler:connectedHandel){
                    SocketAddress socketAddress = rpcClientHandler.getRemotePeer();
                    RpcClientHandler handler = connectedServerNode.get(socketAddress);
                    handler.close();
                    connectedServerNode.remove(handler);
                }
                connectedHandel.clear();
            }
        }
    }

    private void connectServerNode(final InetSocketAddress inetSocketAddress){
        threadPoolExecutor.submit(() -> {
            Bootstrap b = new Bootstrap();
            b.group(nioEventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new RpcClientInitializer());
            ChannelFuture channelFuture = b.connect(inetSocketAddress);
            channelFuture.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()){
                    RpcClientHandler rpcClientHandler = future.channel().pipeline().get(RpcClientHandler.class);
                    addHandler(rpcClientHandler);
                }
            });
        });
    }


    private void addHandler(RpcClientHandler rpcClientHandler){
        connectedHandel.add(rpcClientHandler);
        InetSocketAddress inetSocketAddress = (InetSocketAddress) rpcClientHandler.getChannel().remoteAddress();
        connectedServerNode.put(inetSocketAddress,rpcClientHandler);
    }
}
