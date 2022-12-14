import gossip.constants.GossipSettings;
import gossip.constants.GossipState;
import gossip.entity.SeedMember;
import gossip.manager.GossipManager;
import gossip.manager.GossipService;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class ForTest {
    public static void main(String[] args) {
        int gossip_port = 5000;
        String cluster = "Pikachudy";

        GossipSettings settings = new GossipSettings();
        settings.setGossipInterval(1000);
        GossipService gossipService = null;
        try {
            String myIpAddress = InetAddress.getLocalHost().getHostAddress();
            List<SeedMember> seedNodes = new ArrayList<>();
//            SeedMember seed = new SeedMember();
//            seed.setCluster(cluster);
//            seed.setIpAddress(myIpAddress);
//            seed.setPort(5001);
//            seedNodes.add(seed);
            SeedMember seed_1 = new SeedMember();
            seed_1.setCluster(cluster);
            seed_1.setIpAddress(myIpAddress);
            seed_1.setPort(5000);
            seedNodes.add(seed_1);
            gossipService = new GossipService(cluster, myIpAddress, gossip_port, null, seedNodes, settings, (member, state, payload) -> {
                if (state == GossipState.RCV) {
                    System.out.println("成员:" + member + "  状态: " + state + " 数据: " + payload);
                }
                if (state == GossipState.DOWN) {
                    System.out.println("[结点:" + member + "已经下线!!! ]");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        gossipService.start();
        GossipManager manager = gossipService.getGossipManager();


    }
}