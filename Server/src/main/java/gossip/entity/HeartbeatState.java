package gossip.entity;


import gossip.manager.VersionManager;
import lombok.Data;

/**
 * 定义心跳信息
 *
 * @author Pikachudy
 * @date 2022/12/08
 */
@Data
public class HeartbeatState {
    private long heartbeatTime;
    private long version;

    public HeartbeatState() {
        this.heartbeatTime = System.currentTimeMillis();
    }

    /**
     * heartbeat一次，version+1
     *
     * @return long
     */
    public long updateVersion() {
        setHeartbeatTime(System.currentTimeMillis());
        this.version = VersionManager.getInstance().nextVersion();
        return version;
    }

    @Override
    public String toString() {
        return "HeartbeatState{" +
                "heartbeatTime=" + heartbeatTime +
                ", version=" + version +
                '}';
    }
}
