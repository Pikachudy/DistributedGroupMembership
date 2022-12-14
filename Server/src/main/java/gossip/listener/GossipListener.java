package gossip.listener;


import gossip.constants.GossipState;
import gossip.entity.GossipMember;

/**
 * 定义Gossip事件监听器
 * 用于监听Gossip事件
 * 事件包括：节点加入、节点离开、节点发布信息
 *
 * @author Pikachudy
 * @date 2022/12/11
 */
public interface GossipListener {
    void gossipEvent(GossipMember member, GossipState state, Object payload);
}
