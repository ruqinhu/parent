//package nettyrpc.codec.protobuf;
//
//import nettyrpc.codec.protobuf.entity.Person;
//
//import java.io.IOException;
//
///**
// * @author zrz
// */
//public class ProtobufSerializer {
//
////    public static <T> byte[] serialize(T obj) {
////        Person person = new Person.person().newBuilderForType();
////    }
//
//    public static void main(String[] args) throws IOException {
//        String protoPath = System.getProperty("user.dir") + "\\rpc\\src\\main\\java\\nettyrpc\\codec\\protobuf\\entity";
//        String outPath = System.getProperty("user.dir") + "\\rpc\\src\\main\\java\\nettyrpc\\codec\\protobuf\\entity";
//        String entity = outPath + "\\" + "Person.proto";
//        String strCmd = "D:/software/protobuff/protoc.exe --proto_path=" + protoPath + " --java_out=" + outPath + " " + entity;
//        System.out.println(strCmd);
//        Runtime.getRuntime().exec(strCmd);
//    }
//}
