package net;

import constant.GlobalSetting;
import constant.MessageType;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import lombok.Data;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * 封装 UDP 消息服务
 *
 * @author Pikachudy
 * @date 2022/12/08
 */
@Data
public class UDPService {
    private Logger LOGGER;
    private DatagramSocket socket;
    private String queryMsg;
    private RequestCounter counter;
    public UDPService(RequestCounter counter) {
        LOGGER = LoggerFactory.getLogger(UDPService.class);
        queryMsg = null;
        this.counter = counter;
    }
    /**
     * 开启监听
     *
     * @param ipAddress ip地址
     * @param port      端口
     */
    public void startListen(String ipAddress, int port) {
        DatagramSocketOptions options = new DatagramSocketOptions();
        options.setReceiveBufferSize(65535);
        socket = Vertx.vertx().createDatagramSocket(options);
        socket.listen(port, ipAddress, asyncResult -> {
            if (asyncResult.succeeded()) {
                socket.handler(packet -> handleMsg(packet.data()));
            } else {
                // LOGGER.error("监听失败 " + asyncResult.cause());
            }
        });
    }

    /**
     * 消息解析、处理
     *
     * @param data Buffer数据
     */
    public void handleMsg(Buffer data) {
        // 解析消息
        JsonObject j = data.toJsonObject();
        String type = j.getString("type");
        if(type.equals(MessageType.PONG)){
            Thread thread = new Thread(new PongHandler(data, counter));
            thread.start();
        }
        if(type.equals(MessageType.RESULT)){
            Thread thread = new Thread(new ResultHandler(data, counter));
            thread.start();
        }


    }

    public void sendMsg(String targetIp, Integer targetPort, Buffer data) {
        if (targetIp != null && targetPort != null && data != null) {
            socket.send(data, targetPort, targetIp, asyncResult -> {
            });
        }
    }

    public void unListen() {
        if (socket != null) {
            socket.close(asyncResult -> {
                if (asyncResult.succeeded()) {
                    LOGGER.info("Socket 已关闭");
                } else {
                    LOGGER.error("Socket关闭出错： " + asyncResult.cause().getMessage());
                }
            });
        }
    }

    class PongHandler implements Runnable{
        private Buffer data;
        RequestCounter counter;
        PongHandler(Buffer data, RequestCounter counter){
            this.data = data;
            this.counter= counter;
        }
        @Override
        public void run() {
            JsonObject j = data.toJsonObject();
            JsonArray array = j.getJsonArray("data");
            int port = j.getInteger("port");
            String ip = j.getString("ip");
            this.counter.setRequest_num(array.size());
            this.counter.resetResult();
            this.counter.resetCount();
            this.counter.setOpen(true);
            // 发送查找请求
            JsonObject object = new JsonObject();
            object.put("type", MessageType.QUERY);
            try {
                object.put("ip", InetAddress.getLocalHost().getHostAddress());
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            object.put("port", GlobalSetting.LISTENING_PORT);
            object.put("data", queryMsg);
            queryMsg=null;
            sendMsg(ip, port, Buffer.buffer(object.encode()));
        }
    }

    private class ResultHandler implements Runnable {
        final RequestCounter counter;
        private final Buffer data;
        public ResultHandler(Buffer data, RequestCounter counter) {
            this.data = data;
            this.counter = counter;
        }

        @Override
        public void run() {
            // 解析消息
            JsonObject j = data.toJsonObject();
            synchronized (counter) {
                counter.countDown();
                counter.addResult(j.encode());
            }
        }
    }
}
