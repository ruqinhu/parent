package nettyrpc.codec;

public interface Serializer {
    Object deserialize(byte[] data, Class cls);
    byte[] serialize(Object obj);
}
