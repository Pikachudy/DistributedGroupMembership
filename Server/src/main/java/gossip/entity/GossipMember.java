package gossip.entity;

import gossip.constants.GossipState;
import lombok.Data;

import java.io.Serializable;


/**
 * 定义 Gossip成员
 *
 * @author Pikachudy
 * @date 2022/12/09
 */
@Data
public class GossipMember implements Serializable {
    private String cluster;
    private String ipAddress;
    private Integer port;
    private String id;
    private GossipState state;

    public GossipMember() {
    }

    public GossipMember(String cluster, String ipAddress, Integer port, String id, GossipState state) {
        this.cluster = cluster;
        this.ipAddress = ipAddress;
        this.port = port;
        this.id = id;
        this.state = state;
    }

    public String getId() {
        if (id == null) {
            // 若集群尚未初始化其id，则id为ip+port
            setId(ipSplicePort());
        }
        return id;
    }

    @Override
    public String toString() {
        return "GossipMember{" +
                "cluster='" + cluster + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                ", id='" + id + '\'' +
                ", state=" + state +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GossipMember member = (GossipMember) o;

        if (!cluster.equals(member.cluster)) {
            return false;
        }
        if (!ipAddress.equals(member.ipAddress)) {
            return false;
        }
        return port.equals(member.port);
    }

    @Override
    public int hashCode() {
        int result = cluster.hashCode();
        result = 31 * result + ipAddress.hashCode();
        result = 31 * result + port.hashCode();
        return result;
    }

    /**
     * ip拼接端口
     *
     * @return {@code String}
     */
    public String ipSplicePort() {
        return ipAddress.concat(":").concat(String.valueOf(port));
    }

    /**
     * 特征值
     * cluster+ip+port
     *
     * @return {@code String}
     */
    public String eigenvalue() {
        return getCluster().concat(":").concat(getIpAddress()).concat(":").concat(getPort().toString());
    }
}
