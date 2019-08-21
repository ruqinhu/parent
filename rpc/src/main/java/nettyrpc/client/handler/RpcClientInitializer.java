package nettyrpc.client.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import nettyrpc.protocol.RpcDecoder;
import nettyrpc.protocol.RpcEncoder;
import nettyrpc.protocol.RpcRequest;
import nettyrpc.protocol.RpcResponse;

public class RpcClientInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
                .addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0))
                .addLast(new RpcDecoder(RpcResponse.class))
                .addLast(new RpcEncoder(RpcRequest.class))
                .addLast(new RpcClientHandler());
    }
}
