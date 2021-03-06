package com.wuma.zookeeper;

import org.apache.log4j.Logger;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.KeeperException.Code;
import java.util.Arrays;

/**
 * Created by zhang on 2017/1/24.
 * A simple class that monitors the data and existence of a ZooKeeper
 * node. It uses asynchronous ZooKeeper APIs.
 */
public class DataMonitor implements Watcher,AsyncCallback.StatCallback {
    ZooKeeper zk;
    String znode;
    Watcher chainedWatcher;
    boolean dead;
    DataMonitorListener listener;
    byte prevData[];
    private final static Logger logger=Logger.getLogger(DataMonitor.class);

    /**
     * Other classes use the DataMonitor by implementing this method
     */
    public interface DataMonitorListener{
        /**
         * The existence status of the node has changed.
         */
        void exists(byte[] data);
        /**
         * The ZooKeeper session is no longer valid.
         *
         * @param rc
         * the ZooKeeper reason code
         */
        void closing(int rc);
    }

    public DataMonitor(ZooKeeper zk,String znode,Watcher chainedWatcher,
                       DataMonitorListener listener){
        this.zk=zk;
        this.znode=znode;
        this.chainedWatcher=chainedWatcher;
        this.listener=listener;
        //get things started by checking if the node exists. We are going
        // to be completely event driven
        zk.exists(znode, true, this, null);

    }
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        boolean exists;
        switch (rc) {
            case Code.Ok:
                exists = true;
                logger.info(znode+" exists OK is "+exists);
                break;
            case Code.NoNode:
                exists = false;
                logger.info(znode+" exists NoNode is "+exists);
                try {
                    zk.create(znode,"hellozookeeperlidage".getBytes(),null,CreateMode.EPHEMERAL);
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {

                }
                break;
            case Code.SessionExpired:
                logger.info(znode+" exists SessionExpired is ");
            case Code.NoAuth:
                dead = true;
                logger.info(znode+" NoAuth");
                listener.closing(rc);
                return;
            default:
                // Retry errors
                zk.exists(znode, true, this, null);
                return;
        }

        byte b[] = null;
        if (exists) {
            try {
                b = zk.getData(znode, false, null);
            } catch (KeeperException e) {
                // We don't need to worry about recovering now. The watch
                // callbacks will kick off any exception handling
                e.printStackTrace();
            } catch (InterruptedException e) {
                return;
            }
        }else {
            logger.info(znode+" not exist,please take it easey.");
        }
        if ((b == null && b != prevData)
                || (b != null && !Arrays.equals(prevData, b))) {
            listener.exists(b);
            prevData = b;
        }
    }

    public void process(WatchedEvent event) {
        String path=event.getPath();
        if (event.getType()==Event.EventType.None){
            //We are being told that the state of the
            //connecting has changed
            switch (event.getState()){
                case SyncConnected:
                    // In this particular example we don't need to do anything
                    // here - watches are automatically re-registered with
                    // server and any watches triggered while the client was
                    // disconnected will be delivered (in order of course)
                    break;
                case Expired:
                    // It's all over
                    dead = true;
                    listener.closing(KeeperException.Code.SessionExpired);
                    break;
            }
        }else {
            if (path != null && path.equals(znode)) {
                // Something has changed on the node, let's find out
                zk.exists(znode, true, this, null);
            }
        }
        if (chainedWatcher != null) {
            chainedWatcher.process(event);
        }
    }
}
