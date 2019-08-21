package nettyrpc.codec.protostuff;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import nettyrpc.codec.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtostuffSerializer {

    public static final Logger logger = LoggerFactory.getLogger(ProtostuffSerializer.class);

    public static <T> byte[] serialize(T obj) {
        Schema<T> schema = (Schema<T>) RuntimeSchema.getSchema(obj.getClass());
        byte[] protostuff = null;
        LinkedBuffer buffer = LinkedBuffer.allocate(4096);
        try {
            protostuff = ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } finally {
            buffer.clear();
        }
        return protostuff;
    }

    public static <T> T deserialize(byte[] bytes, Class<T> clazz){
        if(bytes == null || bytes.length <= 0) {
            return null;
        }
        T obj = null;
        try {
            obj = clazz.newInstance();
        } catch (InstantiationException e) {
            logger.info(e.getStackTrace().toString());
        } catch (IllegalAccessException e) {
            logger.info(e.getStackTrace().toString());
        }
        Schema<T> schema = RuntimeSchema.getSchema(clazz);
        ProtostuffIOUtil.mergeFrom(bytes,obj,schema);
        return obj;
    }

    public static void main(String[] args){
        Person person = new Person("zhao","runze");
        System.out.println(person.toString());
        byte[] bytes = ProtostuffSerializer.serialize(person);
        Object obj = ProtostuffSerializer.deserialize(bytes, Person.class);
        System.out.println(obj.toString());
    }
}
