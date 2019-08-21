package nettyrpc.registry.discovery;

import nettyrpc.client.ConnectManage;
import nettyrpc.registry.constant.RegistryConstant;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ZookeeperDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperDiscovery.class);

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    private List<String> dataList = new ArrayList<>();

    private String registryAddress;
    private ZooKeeper zk ;

    public ZookeeperDiscovery(String registryAddress){
        this.registryAddress = registryAddress;
        zk = connectServer();
        if (zk != null){
            watchNode(zk);
        }
    }

    private ZooKeeper connectServer(){
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(registryAddress, RegistryConstant.ZK_SESSION_TIMEOUT,watchedEvent -> {
                if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected){
                    countDownLatch.countDown();
                }
            });
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return zk;
    }

    private void watchNode(final ZooKeeper zk){
        try {
            List<String> nodeList = zk.getChildren(RegistryConstant.ZK_REGISTRY_PATH,watchedEvent -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged){
                    watchNode(zk);
                }
            });
            List<String> dataList = new ArrayList<>();
            for (String node: nodeList){
                byte[] bytes = zk.getData(RegistryConstant.ZK_REGISTRY_PATH +"/" + node,false,null);
                dataList.add(new String(bytes));
            }
            logger.info("node中数据信息：" + dataList);
            this.dataList = dataList;
            logger.info("discovery定时更新node中的data");
            updateConnectedServer();
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop(){
        if(zk != null){
            try {
                zk.close();
            } catch (InterruptedException e) {
                logger.error("", e);
            }
        }
    }

    private void updateConnectedServer(){
        ConnectManage.getInstance().updateConnectServer(this.dataList);
    }
}
