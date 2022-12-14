package gossip.constants;

/**
 * 消息类型
 *
 * @author Pikachudy
 * @date 2022/12/08
 */
public enum MessageType {
    SYNC_MESSAGE("sync_message"), ACK_MESSAGE("ack_message"), ACK2_MESSAGE("ack2_message"), SHUTDOWN("shutdown"), REG_MESSAGE("reg_message");

    private final String type;

    MessageType(String type) {
        this.type = type;
    }
}
