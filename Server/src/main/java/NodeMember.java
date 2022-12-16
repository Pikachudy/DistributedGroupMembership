import aboutClient.GlobalSetting;
import aboutClient.UDPClientService;
import gossip.constants.GossipSettings;
import gossip.constants.GossipState;
import gossip.entity.GossipMember;
import gossip.entity.SeedMember;
import gossip.manager.GossipManager;
import gossip.manager.GossipService;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * 普通节点服务入口
 *
 * @author Pikachudy
 * @date 2022/12/15
 */
public class NodeMember {
    public static void main(String[] args) {

        int myPort = selectPort(1201);
        String cluster = GlobalSetting.CLUSTER;

        Logger LOGGER = Logger.getLogger(NodeMember.class.getName()+":"+myPort);
        String filePath = "Server/src/main/java/log/Event_"+cluster+"_"+myPort+".log";
        // 判断文件是否存在,若不存在则新建
        if(!new java.io.File(filePath).exists()){
            try {
                new java.io.File(filePath).createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        FileHandler fileHandler = null;
        try {
            fileHandler = new FileHandler(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        fileHandler.setFormatter(new SimpleFormatter());
        LOGGER.setUseParentHandlers(false);
        LOGGER.addHandler(fileHandler);

        GossipSettings settings = new GossipSettings();
        GossipService gossipService = null;
        try {
            String myIpAddress = InetAddress.getLocalHost().getHostAddress();
            List<SeedMember> seedNodes = new ArrayList<>();
            SeedMember seed = new SeedMember(cluster,GlobalSetting.INTRODUCER_IP,GlobalSetting.INTRODUCER_PORT,null);
            seedNodes.add(seed);
            gossipService = new GossipService(cluster, myIpAddress, myPort, null, seedNodes, settings, (member, state, payload, selfManager) -> {
                eventHandle(member, state, payload, selfManager, LOGGER, filePath);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        gossipService.start();
        Thread thread = new Thread(new Introducer.listenConsoleOp(gossipService));
        thread.start();
    }
    static void eventHandle(GossipMember member, GossipState state, Object payload, GossipManager selfManager,Logger LOGGER,String filePath) {
        if (state == GossipState.MEET){
            System.out.println("["+"\033[32;1m"+"新结点:"+ member + "加入!!!"+"\033[0m" + "]");
            selfManager.showDigests();
            LOGGER.info("[新结点:"+ member + "加入!!!]");
        }
        if (state == GossipState.RCV) {
            UDPClientService udpClientService = new UDPClientService(selfManager,filePath);
            List<String> argList = List.of(payload.toString().split("-"));
            udpClientService.queryHandler(argList.get(0),argList.get(1), Integer.parseInt(argList.get(2)));
        }
        if (state == GossipState.DOWN) {
            System.out.println("["+"\033[31;1m"+"结点:" + member + "已经下线!!! "+"\033[0m"+ "]");
            selfManager.showDigests();
            LOGGER.info("[结点:" + member + "已经下线!!! ]");
        }
        if( state == GossipState.UP){
            System.out.println("["+"\033[36;1m"+"结点:" + member + "重新上线!!! "+" \033[0m"+ "]");
            selfManager.showDigests();
            LOGGER.info("[结点:" + member + "重新上线!!! ]");
        }
    }

    /**
     * 选择可用端口，如果端口被占用，则选择下一个端口
     *
     * @param startPort 启动端口
     * @return int 可用端口
     */
    static int selectPort(int startPort){
        int port = startPort;
        while (true){
            try {
                DatagramSocket socket = new DatagramSocket(port);
                socket.close();
                break;
            } catch (IOException e) {
                port++;
            }
        }
        return port;
    }

    /**
     * 监听控制台输入
     *
     * @author Pikachudy
     * @date 2022/12/15
     */
    static class listenConsoleOp implements Runnable{
        private final GossipService service;
        listenConsoleOp(GossipService service) {
            this.service = service;
        }
        public void run(){
            while(true){
                Scanner scanner = new Scanner(System.in);
                String input = scanner.nextLine();
                if(input.equals("quit")){
                    this.service.shutdown();
                    break;
                }
            }
            System.out.println("服务已关闭");
            System.out.println(this.service.getGossipManager().getSelf()+"已下线");

            System.exit(0);
        }
    }

}
