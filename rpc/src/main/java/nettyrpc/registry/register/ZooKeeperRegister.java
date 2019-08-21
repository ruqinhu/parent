package nettyrpc.registry.register;

import nettyrpc.registry.constant.RegistryConstant;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * @author Zhaorunze
 */
public class ZooKeeperRegister {

    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperRegister.class);

    private String registryAddress;

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    public ZooKeeperRegister(String registryAddress){
        this.registryAddress = registryAddress;
    }

    public void registry(String data){
        if (data != null){
            ZooKeeper zk = connectServer();
            if (zk != null){
                AddRootNode(zk);
                createNode(zk,data);
            }
        }
    }

    private ZooKeeper connectServer(){
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(registryAddress, RegistryConstant.ZK_SESSION_TIMEOUT, watchedEvent -> {
                if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected){
                    countDownLatch.countDown();
                }
            });
            countDownLatch.await();
        }catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return zk;
    }

    private void AddRootNode(ZooKeeper zk){
        try {
            Stat stat = zk.exists(RegistryConstant.ZK_REGISTRY_PATH,false);
            if (stat == null){
                zk.create(RegistryConstant.ZK_REGISTRY_PATH,new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createNode(ZooKeeper zk,String data){
        byte[] bytes = data.getBytes();
        try {
            String path = zk.create(RegistryConstant.ZK_DATA_PATH,bytes,ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.EPHEMERAL_SEQUENTIAL);
            logger.info("创建zookeeper节点：" + path);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
