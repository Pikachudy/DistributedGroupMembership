package gossip.entity;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gossip.serializer.CustomDeserializer;
import gossip.serializer.CustomSerializer;

import java.io.Serializable;
import java.util.Map;

/**
 * 定义 Ack2消息
 *
 * @author Pikachudy
 * @date 2022/12/10
 */
public class Ack2Message implements Serializable {
    @JsonSerialize(keyUsing = CustomSerializer.class)
    @JsonDeserialize(keyUsing = CustomDeserializer.class)
    private Map<GossipMember, HeartbeatState> endpoints;

    public Ack2Message() {
    }

    public Ack2Message(Map<GossipMember, HeartbeatState> endpoints) {

        this.endpoints = endpoints;
    }

    @Override
    public String toString() {
        return "GossipDigestAck2Message{" +
                "endpoints=" + endpoints +
                '}';
    }

    public Map<GossipMember, HeartbeatState> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<GossipMember, HeartbeatState> endpoints) {
        this.endpoints = endpoints;
    }
}
