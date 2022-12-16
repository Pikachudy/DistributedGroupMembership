import gossip.constants.GossipSettings;
import gossip.constants.GossipState;
import gossip.entity.GossipMember;
import gossip.entity.SeedMember;
import gossip.manager.GossipManager;
import gossip.manager.GossipService;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * introducer服务入口
 *
 * @author Pikachudy
 * @date 2022/12/15
 */
public class Introducer {

    public static void main(String[] args) {
        int gossip_port = GlobalSetting.INTRODUCER_PORT;
        String cluster = GlobalSetting.CLUSTER;

        GossipSettings settings = new GossipSettings();
        GossipService gossipService = null;
        try {
            String myIpAddress = InetAddress.getLocalHost().getHostAddress();
            List<SeedMember> seedNodes = new ArrayList<>();
            SeedMember seed = new SeedMember(cluster,GlobalSetting.INTRODUCER_IP,GlobalSetting.INTRODUCER_PORT,null);
            seedNodes.add(seed);
            gossipService = new GossipService(cluster, myIpAddress, gossip_port, null, seedNodes, settings, (member, state, payload, selfManager) -> {
                eventHandle(member, state, payload, selfManager);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        gossipService.start();
        Thread thread = new Thread(new listenConsoleOp(gossipService));
        thread.start();

    }

    static void eventHandle(GossipMember member, GossipState state, Object payload, GossipManager selfManager) {
        if (state == GossipState.MEET){
            System.out.println("["+"\033[32;1m"+"新结点:"+ member + "加入!!!"+"\033[0m" + "]");
            selfManager.showDigests();
        }
        if (state == GossipState.RCV) {
            System.out.println("成员:" + member + "  状态: " + state + " 数据: " + payload);
        }
        if (state == GossipState.DOWN) {
            System.out.println("["+"\033[31;1m"+"结点:" + member + "已经下线!!! "+"\033[0m"+ "]");
            selfManager.showDigests();
        }
        if( state == GossipState.UP){
            System.out.println("["+"\033[36;1m"+"结点:" + member + "重新上线!!! "+" \033[0m"+ "]");
            selfManager.showDigests();
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