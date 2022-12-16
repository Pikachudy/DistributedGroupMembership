package aboutClient;

import gossip.manager.GossipManager;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 封装 UDP 消息服务 用于与 client 端通信
 *
 * @author Pikachudy
 * @date 2022/12/08
 */
public class UDPClientService {
    Logger LOGGER = LoggerFactory.getLogger(UDPClientService.class);
    String logPath;
    DatagramSocket socket;
    GossipManager manager;
    public UDPClientService(GossipManager manager,String logPath) {
        this.logPath = logPath;
        this.manager = manager;
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
                LOGGER.error("监听失败 " + asyncResult.cause());
            }
        });
    }

    /**
     * 消息解析、处理
     *
     * @param data Buffer数据
     */
    public void handleMsg(Buffer data) {
        JsonObject object = data.toJsonObject();
        String type = object.getString("type");
        String ip = object.getString("ip");
        int port = object.getInteger("port");
        String msg = object.getString("data");
        if(Objects.equals(type, ClientMessageType.PING)){
            // 收到 ping 消息，回复 pong
            pingHandler(ip, port);
        }
        if(Objects.equals(type, ClientMessageType.QUERY)){
            // 构造消息格式，发布消息
            String queryQuest= msg+"-"+ip+"-"+port;
            manager.publish(queryQuest);
            // 收到 query 请求，回复 result
            queryHandler(msg, ip, port);
        }
    }

    private void pingHandler(String ip, int port) {
        JsonObject j = new JsonObject();
        j.put("type", ClientMessageType.PONG);
        j.put("ip", GlobalSetting.INTRODUCER_IP);
        j.put("port", GlobalSetting.INTRODUCER_EXTERNAL_PORT);
        j.put("data", manager.getLiveMembers());
        sendMsg(ip, port, Buffer.buffer(j.encode()));
    }
    public void queryHandler(String msg,String ip, int port) {

        // 返回本地查询结果
        List<String> args = new ArrayList<>();
        args.add(msg);
        args.add(GlobalSetting.PROJECT_PATH + logPath);
        String result;
        try {
            result = callShell(GlobalSetting.SHELL_PATH,"querylog.sh",args);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        JsonObject j = new JsonObject();
        j.put("type", ClientMessageType.RESULT);
        try {
            j.put("ip", InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            LOGGER.error("获取本地ip失败");
            throw new RuntimeException(e);
        }
        j.put("port", GlobalSetting.INTRODUCER_EXTERNAL_PORT);
        j.put("from_id",manager.getID());
        j.put("data", Objects.requireNonNullElse(result, "-"));
        sendMsg(ip, port, Buffer.buffer(j.encode()));
    }
    public void sendMsg(String targetIp, Integer targetPort, Buffer data) {
        if(socket == null){
            DatagramSocketOptions options = new DatagramSocketOptions();
            options.setReceiveBufferSize(65535);
            socket = Vertx.vertx().createDatagramSocket(options);
        }
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
    /**
     * 调用脚本查询文件
     * @param shellDir shell所在路径
     * @param shellName shell名称
     * @param args shell参数 - keyword、文件path
     * @return shell返回值
     * @throws IOException 捕获shell输出失败
     */
    public String callShell(String shellDir, String shellName, List<String> args) throws IOException {

        List<String> command = new ArrayList<>();
        command.add("./"+shellName);
        command.addAll(args);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(shellDir));
        // 调用脚本
        int runningStatus = 0;
        Process p = null;
        try {
            p = pb.start();
            try {
                runningStatus = p.waitFor();
            } catch (InterruptedException e) {
                System.out.println("shell run failed!\n"+e);
            }

        } catch (IOException e) {
            System.out.println("shell run failed\n"+e);
        }
        if (runningStatus != 0) {
            System.out.println("shell return code:"+runningStatus);
        }

        // 返回值
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = null;
        StringBuilder result= new StringBuilder();
        while((line = reader.readLine())!=null){
            result.append(line).append("\n");
        }
        return String.valueOf(result);
    }
}
