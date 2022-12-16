import constant.GlobalSetting;
import constant.MessageType;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import lombok.Data;
import net.RequestCounter;
import net.UDPService;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Client端
 * @author: Pikachudy
 * @date: 2022/12/4
 */
public class Client {
    private Logger LOGGER = Logger.getLogger("Client");
    private final String introducerIp;
    private final int introducerExternalPort;
    private final int listeningPort;
    private RequestCounter counter;
    private UDPService udpService;
    private Client(){
        listeningPort = GlobalSetting.LISTENING_PORT;
        introducerIp = GlobalSetting.INTRODUCER_IP;
        introducerExternalPort = GlobalSetting.INTRODUCER_EXTERNAL_PORT;
        counter = new RequestCounter(1);
        counter.resetCount();
        udpService = new UDPService(counter);
    }
    static Client createClient() {
        return new Client();
    }
    void init(){
        try {
            udpService.startListen(InetAddress.getLocalHost().getHostAddress(),listeningPort);
        } catch (UnknownHostException e) {
            LOGGER.severe("UDP监听失败：无法获取本地IP地址");
        }
    }

    /**
     * 读取console输入的命令
     * @return 输入的查询关键词,若退出则返回null
     */
    private String readConsole() throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("请输入所查询关键字(输入quit则退出):");
        String s = console.readLine();
        if("quit".equals(s)){
            return null;
        }
        else{
            return s.trim();
        }
    }

    /**
     * 客户端-服务端通信管理器——便于多线程
     * @author: Pikachudy
     * @date: 2022/12/6
     */
    public static void main(String[] args) throws IOException {
        Logger logger = Logger.getLogger("client");

        Client c = createClient();
        while (true) {
            String query = c.readConsole();
            if (query == null) {
                break;
            }
            else{
                c.udpService.setQueryMsg(query);
                c.init();
                c.sendQuery(c);
                while (!c.counter.isOpen() || c.counter.getCount() > 0){
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                c.counter.setOpen(false);
                System.out.println("查询结果：");
                c.showResult(c.counter.getResult());
            }
        }
    }

    private void sendQuery(Client c) throws UnknownHostException {
        Buffer buffer = Buffer.buffer();
        JsonObject object = new JsonObject();
        object.put("type", MessageType.PING);
        object.put("ip",InetAddress.getLocalHost().getHostAddress());
        object.put("port", c.listeningPort);
        object.put("data","ping");
        buffer.appendString(object.encode());
        c.udpService.sendMsg(c.introducerIp, c.introducerExternalPort,buffer);
    }
   private void showResult(List<String> result){
        for (String s : result) {
            JsonObject object = new JsonObject(s);
            String from = object.getString("from_id");
            String data= object.getString("data");
            System.out.println("\n来自 "+from);
            System.out.println(data);
        }
    }
}





