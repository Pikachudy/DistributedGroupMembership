package gossip.handler;

import gossip.entity.GossipMember;
import gossip.manager.GossipManager;
import io.vertx.core.json.JsonObject;

/**
 * Shutdown消息处理器
 * 用于处理Shutdown消息
 *
 * @author Pikachudy
 * @date 2022/12/09
 */
public class ShutdownMessageHandler implements MessageHandler {
    @Override
    public void handle(String cluster, String data, String from) {
        JsonObject dj = new JsonObject(data);
        GossipMember whoShutdown = dj.mapTo(GossipMember.class);
        if (whoShutdown != null) {
            GossipManager.getInstance().down(whoShutdown);
        }
    }
}
