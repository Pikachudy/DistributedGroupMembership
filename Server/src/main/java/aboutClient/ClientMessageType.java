package aboutClient;

/**
 * 消息类型
 *
 * @author Pikachudy
 * @date 2022/12/16
 */
public interface ClientMessageType {
    String PING = "ping";
    String PONG = "pong";
    String QUERY = "query";
    String RESULT = "result";

}
