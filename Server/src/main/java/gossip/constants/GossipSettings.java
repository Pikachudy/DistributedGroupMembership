package gossip.constants;


import gossip.entity.SeedMember;
import gossip.manager.GossipManager;
import gossip.manager.MessageManager;
import gossip.udp.UDPService;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Gossip 设置
 *
 * @author Pikachudy
 * @date 2022/12/08
 */
@Data
public class GossipSettings {
    //随机Ping的时间间隔
    private int gossipInterval = 1000;

    //网络超时时间
    private int networkDelay = 200;

    private UDPService msgService = new UDPService();

    //删除阈值
    private int deleteThreshold = 3;

    private List<SeedMember> seedMembers;

    private MessageManager messageManager = new MessageManager();

    public void setGossipInterval(int gossipInterval) {
        this.gossipInterval = gossipInterval;
    }

    public void setSeedMembers(List<SeedMember> seedMembers) {
        List<SeedMember> _seedMembers = new ArrayList<>();
        if (seedMembers != null && !seedMembers.isEmpty()) {
            for (SeedMember seed : seedMembers) {
                if (!seed.eigenvalue().equalsIgnoreCase(GossipManager.getInstance().getSelf().eigenvalue())) {
                    if (!_seedMembers.contains(seed)) {
                        _seedMembers.add(seed);
                    }
                }
            }
        }
        this.seedMembers = seedMembers;
    }
}
