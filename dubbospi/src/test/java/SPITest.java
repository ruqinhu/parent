
import loader.ExtensionLoader;
import org.junit.Test;
import service.Spi;

public class SPITest {

    @Test
    public void test() throws Exception {
        //获取默认实现类
        Spi defaultExtension = ExtensionLoader.
                getExtensionLoader(Spi.class).
                getDefaultExtension();
        defaultExtension.say();

        //指定特定的实现类,例如配置的tobyLog
        Spi dubboSpi = ExtensionLoader.
                getExtensionLoader(Spi.class).
                getExtension("dubboSpi");
        dubboSpi.say();

    }

}
