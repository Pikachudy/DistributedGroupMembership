package gossip.handler;

import gossip.entity.AckMessage;
import gossip.entity.GossipDigest;
import gossip.entity.GossipMember;
import gossip.entity.HeartbeatState;
import gossip.manager.GossipManager;
import gossip.serializer.Serializer;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;

import java.util.*;


/**
 * 同步消息处理器
 * 用于处理同步消息
 *
 * @author Pikachudy
 * @date 2022/12/10
 */
public class SyncMessageHandler implements MessageHandler {
    @Override
    public void handle(String cluster, String data, String from) {
        if (data != null) {
            try {
                // 更新当前结点维护的成员列表
                JsonArray array = new JsonArray(data);
                List<GossipDigest> olders = new ArrayList<>();
                Map<GossipMember, HeartbeatState> newers = new HashMap<>();
                List<GossipMember> gMemberList = new ArrayList<>();
                for (Object e : array) {
                    GossipDigest g = Serializer.getInstance().decode(Buffer.buffer().appendString(e.toString()), GossipDigest.class);
                    GossipMember member = new GossipMember();
                    member.setCluster(cluster);
                    member.setIpAddress(g.getEndpoint().getAddress().getHostAddress());
                    member.setPort(g.getEndpoint().getPort());
                    member.setId(g.getId());
                    gMemberList.add(member);

                    compareDigest(g, member, cluster, olders, newers);
                }
                // 与发送端同步成员列表
                Map<GossipMember, HeartbeatState> endpoints = GossipManager.getInstance().getEndpointMembers();
                Set<GossipMember> epKeys = endpoints.keySet();
                for (GossipMember m : epKeys) {
                    if (!gMemberList.contains(m)) {
                        newers.put(m, endpoints.get(m));
                    }
                    if (m.equals(GossipManager.getInstance().getSelf())) {
                        newers.put(m, endpoints.get(m));
                    }
                }
                AckMessage ackMessage = new AckMessage(olders, newers);
                Buffer ackBuffer = GossipManager.getInstance().encodeAckMessage(ackMessage);
                if (from != null) {
                    String[] host = from.split(":");
                    GossipManager.getInstance().getSettings().getMsgService().sendMsg(host[0], Integer.valueOf(host[1]), ackBuffer);
                }
            } catch (NumberFormatException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }


    private void compareDigest(GossipDigest g, GossipMember member, String cluster, List<GossipDigest> olders, Map<GossipMember, HeartbeatState> newers) {

        try {
            HeartbeatState hb = GossipManager.getInstance().getEndpointMembers().get(member);
            long remoteHeartbeatTime = g.getHeartbeatTime();
            long remoteVersion = g.getVersion();
            if (hb != null) {
                long localHeartbeatTime = hb.getHeartbeatTime();
                long localVersion = hb.getVersion();

                if (remoteHeartbeatTime > localHeartbeatTime) {
                    olders.add(g);
                } else if (remoteHeartbeatTime < localHeartbeatTime) {
                    newers.put(member, hb);
                } else {
                    if (remoteVersion > localVersion) {
                        olders.add(g);
                    } else if (remoteVersion < localVersion) {
                        newers.put(member, hb);
                    }
                }
            } else {
                olders.add(g);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }
}
