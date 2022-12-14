package gossip.handler;

import gossip.constants.GossipState;
import gossip.entity.RegularMessage;
import gossip.manager.GossipManager;
import gossip.manager.MessageManager;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Regular消息处理器
 * 用于处理 Regular消息
 *
 * @author Pikachudy
 * @date 2022/12/09
 */
public class RegularMessageHandler implements MessageHandler {
    private static final ConcurrentHashMap<String, String> RECEIVED = new ConcurrentHashMap<>();

    @Override
    public void handle(String cluster, String data, String from) {
        JsonObject dj = new JsonObject(data);
        RegularMessage msg = dj.mapTo(RegularMessage.class);
        MessageManager mm = GossipManager.getInstance().getSettings().getMessageManager();
        String creatorId = msg.getCreator().getId();
        if (!RECEIVED.containsKey(creatorId)) {
            RECEIVED.put(creatorId, msg.getId());
        } else {
            String rcvedId = RECEIVED.get(creatorId);
            int c = msg.getId().compareTo(rcvedId);
            if (c <= 0) {
                return;
            } else {
                mm.remove(rcvedId);
                RECEIVED.put(creatorId, msg.getId());
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("收到来自[" + from + "] 的消息 : [" + msg + "]");
        }
        if (!mm.contains(msg.getId())) {
            msg.setForwardCount(0);
            mm.add(msg);
            GossipManager.getInstance().fireGossipEvent(msg.getCreator(), GossipState.RCV, msg.getPayload());
        }
    }
}
