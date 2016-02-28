package com.wuma.zookeeper;

import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * Created by liwujun on 16/2/28.
 */
public class ServiceRegistry {
    private static final Logger LOGGER= LoggerFactory.getLogger(ServiceRegistry.class);
    private CountDownLatch latch=new CountDownLatch(1);
    private String registryAddress;
    public ServiceRegistry(String registryAddress){
        this.registryAddress=registryAddress;
    }
    public void registry(String data){
        if (data!=null){
            ZooKeeper zk=connectServer();
            if (zk!=null){
                createNode(zk,data);
            }
        }
    }
    private ZooKeeper connectServer() {
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, new Watcher() {
                public void process(WatchedEvent event) {
                    if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                        latch.countDown();
                    }
                }
            });
            latch.await();
        } catch (Exception e ) {
            LOGGER.error("", e);
        }
        return zk;
    }
    private void createNode(ZooKeeper zk, String data) {
        try {
            byte[] bytes = data.getBytes();
            String path = zk.create(Constant.ZK_DATA_PATH, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            LOGGER.debug("create zookeeper node ({} => {})", path, data);
        } catch (KeeperException e ) {
            LOGGER.error("", e);
        }catch (InterruptedException e){
            LOGGER.error("", e);
        }
    }
}
