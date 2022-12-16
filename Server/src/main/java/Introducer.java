import aboutClient.GlobalSetting;
import aboutClient.UDPClientService;
import gossip.constants.GossipSettings;
import gossip.constants.GossipState;
import gossip.entity.GossipMember;
import gossip.entity.SeedMember;
import gossip.manager.GossipManager;
import gossip.manager.GossipService;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * introducer服务入口
 *
 * @author Pikachudy
 * @date 2022/12/15
 */
public class Introducer {
    /**
     * udp服务,用户和客户端通信
     */
    private static UDPClientService udpClient;

    public static void main(String[] args) {
        int gossip_port = GlobalSetting.INTRODUCER_PORT;
        String cluster = GlobalSetting.CLUSTER;
        Logger LOGGER = Logger.getLogger(NodeMember.class.getName()+":"+gossip_port);
        String filePath = "Server/src/main/java/log/Event_"+cluster+"_"+gossip_port+".log";
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
            gossipService = new GossipService(cluster, myIpAddress, gossip_port, null, seedNodes, settings, (member, state, payload, selfManager) -> {
                eventHandle(member, state, payload, selfManager,LOGGER,GlobalSetting.PROJECT_PATH+filePath);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        gossipService.start();
        udpClient= new UDPClientService(gossipService.getGossipManager(),filePath);
        Thread thread = new Thread(new listenConsoleOp(gossipService));
        Thread thread1 = new Thread(new listenClientOp(gossipService));
        thread.start();
        thread1.start();

    }

    static void eventHandle(GossipMember member, GossipState state, Object payload, GossipManager selfManager, Logger LOGGER,String filePath) {
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
                    udpClient.unListen();
                    this.service.shutdown();
                    break;
                }
            }
            System.out.println("服务已关闭");
            System.out.println(this.service.getGossipManager().getSelf()+"已下线");
            System.exit(0);
        }
    }

    static class listenClientOp implements Runnable{
        private final GossipService service;
        private String clientIp;
        private int clientPort;
        listenClientOp(GossipService service) {
            this.service = service;
        }
        public void run(){
            try {
                udpClient.startListen(InetAddress.getLocalHost().getHostAddress(),GlobalSetting.INTRODUCER_EXTERNAL_PORT);
            } catch (UnknownHostException e) {
                System.out.println("监听端口"+GlobalSetting.INTRODUCER_EXTERNAL_PORT+"失败");
                throw new RuntimeException(e);
            }
        }
    }

}