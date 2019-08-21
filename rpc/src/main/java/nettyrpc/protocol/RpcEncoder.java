package nettyrpc.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import nettyrpc.codec.json.JsonUtil;
import nettyrpc.codec.protostuff.ProtostuffSerializer;

/**
 * @author zhaorunze
 */
public class RpcEncoder extends MessageToByteEncoder {

    private Class<?> genericClass;

    public RpcEncoder(Class genericClass){
        this.genericClass = genericClass;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (genericClass.isInstance(msg)){
            byte[] bytes = ProtostuffSerializer.serialize(msg);
            out.writeInt(bytes.length);
            out.writeBytes(bytes);
        }
    }


}
