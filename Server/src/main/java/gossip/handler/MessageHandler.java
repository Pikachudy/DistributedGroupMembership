package gossip.handler;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * 消息处理器接口
 * 用于处理消息
 *
 * @author Pikachudy
 * @date 2022/12/09
 */
public interface MessageHandler {
    Logger LOGGER = LoggerFactory.getLogger(MessageHandler.class);

    void handle(String cluster, String data, String from);
}
