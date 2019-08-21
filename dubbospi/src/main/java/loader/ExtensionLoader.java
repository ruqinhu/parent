package loader;

import annotation.SPI;
import util.Holder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 *  1.nullPointException要早点抛出，不然后面使用到前面的结果，获取到null，如果调用链很长，不好找null的位置
 *  2.double checked locking 即单例模式的双重检查。 静态变量在存入数据时putIfAbsent，非静态的要dcl
 *       (2.1 不是非静态的要dcl，而是对象实例化的时候要dcl，单个对象（Holder）没有使用锁（currentHashMap有锁，所以不用）
 *  3.可以缓存的尽量缓存，比如静态的：新建extension实例时缓存、获取Class对应ExtensionLoader时缓存
 *                          非静态的：加载extensionClasses时缓存、实例化class时缓存
 *
 *  为什么不把 cachedClasses作为静态的变量，也即为什么要同时存在 EXTENSION_INSTANCE 和 cachedClasses
 *  猜测：不是启动时加载，而是实时加载，可能是为了防止运行时替换配置文件等操作，所以做成实时加载。
 */
public class ExtensionLoader<T> {


    public static ConcurrentHashMap<Class,ExtensionLoader> EXTENSION_LOADER = new ConcurrentHashMap();

    public static ConcurrentHashMap<String,Object> EXTIONSION_INSTANCES = new ConcurrentHashMap<String, Object>();

    private static final Pattern NAME_SEPARATOR = Pattern.compile("\\s*[,]+\\s*");
    private static final String DUBBO_DIRECTORY = "META-INF/dubbo/";


    // ==================================================================================================

    private final Class<T> type ;

    //缓存 实例名称字符串  对应  实例持有者Holder，holder中存放的是 extension instance
    private final ConcurrentHashMap<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<String, Holder<Object>>();

    //缓存 实例名称字符串  对应  的实现类
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder();

    //记录loadFile时的异常，在createExtension时抛出来
    //从这里学到，null 要早点抛出，不然后面使用到当前函数时，会不知道在哪个方法中抛出的
    private ConcurrentHashMap<String,IllegalStateException> exceptions = new ConcurrentHashMap();

    private String cachedDefaultName;

    public ExtensionLoader(Class clazz){
        type = clazz;
    }

    /**
     * 静态的方法如果想使用类泛型 ，则必须加三处<T>
     */
    public static<T> ExtensionLoader<T> getExtensionLoader(Class<T> clazz){
        if (clazz == null){
            throw new IllegalArgumentException("参数为空");
        }
        if (!clazz.isInterface()){
            throw new IllegalArgumentException("参数只能是接口");
        }
        if (!withSPIAnnotation(clazz)){
            throw new IllegalArgumentException("必须是@SPI修饰的类");
        }
        ExtensionLoader extensionLoader = EXTENSION_LOADER.get(clazz);
        if (extensionLoader == null){
            EXTENSION_LOADER.putIfAbsent(clazz,new ExtensionLoader(clazz));
            extensionLoader = EXTENSION_LOADER.get(clazz);
        }
        return extensionLoader;
    }

    public T getExtension(String name){
        if (name == null || name.length()==0){
            throw new IllegalArgumentException("参数不能为空");
        }
        //instance在实例化后 在这个getOrCreateHolder方法中放入 cachedInstances 非静态变量中缓存
        Holder<Object> holder = getOrCreateHolder(name);
        Object instance = holder.get();
        if (instance == null){
            synchronized (holder){
                instance = holder.get();
                if (instance == null){
                    //实例化后，在静态变量EXTIONSION_INSTANCES中缓存
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return (T)instance;
    }

    public T getDefaultExtension(){
        getExtensionClasses();
        if (cachedDefaultName == null || cachedDefaultName.length()==0
                || "true".equals(cachedDefaultName)){
            return null;
        }
        return getExtension(cachedDefaultName);
    }

    @SuppressWarnings("unchecked")
    private T createExtension(String name){
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null){
            throw findException(name);
        }
        try {
            T instance = (T)EXTIONSION_INSTANCES.get(name);
            if (instance == null){
                EXTIONSION_INSTANCES.putIfAbsent(name ,clazz.newInstance());
                instance = (T)EXTIONSION_INSTANCES.get(name);
            }
            return instance;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private Map<String,Class<?>> getExtensionClasses(){
        Map<String, Class<?>> classes = cachedClasses.get();
        if (classes == null){
            synchronized (cachedClasses){
                classes = cachedClasses.get();
                if (classes == null){
                    classes = loadExtensionClasses();
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    private Map<String ,Class<?>> loadExtensionClasses(){
        cacheDefaultExtensionName();

        Map<String, Class<?>> extensionClasses = new HashMap();
        loadFile(extensionClasses, DUBBO_DIRECTORY);
        return extensionClasses;
    }

    //当注解上有多个实现类时，选择第一个作为默认实现类
    private void cacheDefaultExtensionName() {
        final SPI defaultAnnotation = type.getAnnotation(SPI.class);
        if (defaultAnnotation != null) {
            String value = defaultAnnotation.value();
            if ((value = value.trim()).length() > 0) {
                String[] names = NAME_SEPARATOR.split(value);
                if (names.length > 1) {
                    throw new IllegalStateException("More than 1 default extension name on extension " + type.getName()
                            + ": " + Arrays.toString(names));
                }
                if (names.length == 1) {
                    cachedDefaultName = names[0];
                }
            }
        }
    }

    private Holder<Object> getOrCreateHolder(String name){
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null){
            cachedInstances.putIfAbsent(name,new Holder<Object>());
            holder = cachedInstances.get(name);
        }
        return holder;
    }

    private static Boolean withSPIAnnotation(Class clazz){
//        for (Annotation annotation:clazz.getAnnotations()){
//            if (annotation.equals(SPI.class)){
//                return true;
//            }
//        }
        if(clazz.isAnnotationPresent(SPI.class)){
            return true;
        }
        return false;
    }


    /**
     * 加载解析spi配置文件,然后加入缓存
     * 配置文件的文件名，其实是加载的路径，即 dir=Spi接口 所在目录
     */
    public void loadFile(Map<String, Class<?>> extensionClasses, String dir) {
        String fileName = dir + type.getName();
        try {
            Enumeration<URL> urls;
            ClassLoader classLoader = findClassLoader();
            if (classLoader != null) {
                urls = classLoader.getResources(fileName);
            } else {
                urls = ClassLoader.getSystemResources(fileName);
            }
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    java.net.URL url = urls.nextElement();
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                        try {
                            String line = null;
                            while ((line = reader.readLine()) != null) {
                                final int ci = line.indexOf('#');
                                if (ci >= 0) {line = line.substring(0, ci);};
                                line = line.trim();
                                if (line.length() > 0) {
                                    try {
                                        String name = null;
                                        int i = line.indexOf('=');
                                        if (i > 0) {
                                            name = line.substring(0, i).trim();
                                            line = line.substring(i + 1).trim();
                                        }
                                        if (line.length() > 0) {
                                            Class<?> clazz = Class.forName(line, true, classLoader);
                                            if (!type.isAssignableFrom(clazz)) {
                                                throw new IllegalStateException("Error when load extension class(interface: " +
                                                        type + ", class line: " + clazz.getName() + "), class "
                                                        + clazz.getName() + "is not subtype of interface.");
                                            }
                                            extensionClasses.put(name, clazz);//加入缓存
                                        }//源码中还有其他的判断,这个版本暂不实现
                                    } catch (Throwable t) {
                                        IllegalStateException e = new IllegalStateException("Failed to load extension class(interface: " + type + ", class line: " + line + ") in " + url + ", cause: " + t.getMessage(), t);
                                        exceptions.put(line, e);
                                    }
                                }
                            } // end of while read lines
                        } finally {
                            reader.close();
                        }
                    } catch (Throwable t) {
                        //logger.error("Exception when load extension class(interface: " +
                        //        type + ", class file: " + url + ") in " + url, t);
                    }
                } // end of while urls
            }
        } catch (Throwable e) {
            //logger.error("Exception when load extension class(interface: " + type + ", description file: " + fileName + ").", e);
        }
    }

    //获取类加载器
    private static ClassLoader findClassLoader() {
        return ExtensionLoader.class.getClassLoader();
    }

    //异常提示
    private IllegalStateException findException(String name) {
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (entry.getKey().toLowerCase().contains(name.toLowerCase())) {
                return entry.getValue();
            }
        }
        StringBuilder buf = new StringBuilder("No such extension " + type.getName() + " by name " + name);

        int i = 1;
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (i == 1) {
                buf.append(", possible causes: ");
            }

            buf.append("\r\n(");
            buf.append(i++);
            buf.append(") ");
            buf.append(entry.getKey());
            buf.append(":\r\n");
            buf.append(entry.getValue().toString());
        }
        return new IllegalStateException(buf.toString());
    }
}
