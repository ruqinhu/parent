package nettyrpc.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import nettyrpc.codec.json.JsonUtil;
import nettyrpc.codec.protostuff.ProtostuffSerializer;

import java.util.List;

/**
 * @author Zhaorunze
 */
public class RpcDecoder extends ByteToMessageDecoder {

    private Class<?> genericClass;

    public RpcDecoder(Class genericClass){
        this.genericClass = genericClass;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        if (buf.readableBytes() < 4){
            return;
        }
        buf.markReaderIndex();
        int dataLength = buf.readInt();
        if (buf.readableBytes() < dataLength){
            buf.resetReaderIndex();
            return;
        }
        byte[] data = new byte[dataLength];
        buf.readBytes(data);

        Object obj = ProtostuffSerializer.deserialize(data, genericClass);

        out.add(obj);
    }
}
