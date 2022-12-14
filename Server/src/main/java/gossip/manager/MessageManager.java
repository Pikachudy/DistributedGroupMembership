package gossip.manager;

import gossip.entity.RegularMessage;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息管理器
 *
 * @author Pikachudy
 * @date 2022/12/09
 */
public class MessageManager {
    private static final ConcurrentHashMap<String, RegularMessage> RegMessages = new ConcurrentHashMap<>();

    public void add(RegularMessage msg) {
        RegMessages.putIfAbsent(msg.getId(), msg);
    }

    public RegularMessage acquire(String id) {
        return RegMessages.get(id);
    }

    public RegularMessage remove(String id) {
        return RegMessages.remove(id);
    }

    public boolean contains(String id) {
        return RegMessages.containsKey(id);
    }

    public boolean isEmpty() {
        return RegMessages.isEmpty();
    }

    public Set<String> list() {
        return RegMessages.keySet();
    }
}
