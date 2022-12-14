package gossip.handler;

import gossip.entity.Ack2Message;
import gossip.entity.GossipMember;
import gossip.entity.HeartbeatState;
import gossip.manager.GossipManager;
import io.vertx.core.json.JsonObject;

import java.util.Map;

/**
 * Ack2消息处理器
 *
 * @author Pikachudy
 * @date 2022/12/09
 */
public class Ack2MessageHandler implements MessageHandler {
    @Override
    public void handle(String cluster, String data, String from) {
        JsonObject dj = new JsonObject(data);
        Ack2Message ack2Message = dj.mapTo(Ack2Message.class);

        Map<GossipMember, HeartbeatState> deltaEndpoints = ack2Message.getEndpoints();
        GossipManager.getInstance().apply2LocalState(deltaEndpoints);
    }
}
