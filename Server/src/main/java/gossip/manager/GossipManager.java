package gossip.manager;

import gossip.constants.GossipSettings;
import gossip.constants.GossipState;
import gossip.constants.MessageType;
import gossip.entity.*;
import gossip.listener.GossipListener;
import gossip.serializer.Serializer;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import lombok.Data;

import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * 本地结点维护
 *
 * @author Pikachudy
 * @date 2022/12/10
 */
@Data
public class GossipManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(GossipManager.class);
    private static final GossipManager instance = new GossipManager();
    private final ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();
    private final ScheduledExecutorService doGossipExecutor = Executors.newScheduledThreadPool(1);
    private final Map<GossipMember, HeartbeatState> endpointMembers = new ConcurrentHashMap<>();
    private final List<GossipMember> liveMembers = new ArrayList<>();
    private final List<GossipMember> deadMembers = new ArrayList<>();
    private final Map<GossipMember, CandidateMemberState> candidateMembers = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private boolean isWorking = false;
    private Boolean isSeedNode = null;
    private GossipSettings settings;
    private GossipMember localGossipMember;
    private String cluster;
    private GossipListener listener;

    private GossipManager() {
    }

    public static GossipManager getInstance() {
        return instance;
    }

    public void init(String cluster, String ipAddress, Integer port, String id, List<SeedMember> seedMembers, GossipSettings settings, GossipListener listener) {
        this.cluster = cluster;
        this.localGossipMember = new GossipMember();
        this.localGossipMember.setCluster(cluster);
        this.localGossipMember.setIpAddress(ipAddress);
        this.localGossipMember.setPort(port);
        this.localGossipMember.setId(id);
        this.localGossipMember.setState(GossipState.JOIN);
        this.endpointMembers.put(localGossipMember, new HeartbeatState());
        this.listener = listener;
        this.settings = settings;
        this.settings.setSeedMembers(seedMembers);
        fireGossipEvent(localGossipMember, GossipState.JOIN);
    }

    /**
     * 开始
     */
    protected void start() {
        LOGGER.info(String.format("正在开启服务…… cluster[%s] ip[%s] port[%d] id[%s]", localGossipMember.getCluster(), localGossipMember.getIpAddress(), localGossipMember.getPort(), localGossipMember.getId()
        ));
        isWorking = true;
        settings.getMsgService().startListen(getSelf().getIpAddress(), getSelf().getPort());
        doGossipExecutor.scheduleAtFixedRate(new GossipTask(), settings.getGossipInterval(), settings.getGossipInterval(), TimeUnit.MILLISECONDS);
    }

    public GossipMember getSelf() {
        return localGossipMember;
    }

    public String getID() {
        return getSelf().getId();
    }

    public Map<GossipMember, HeartbeatState> getEndpointMembers() {
        return endpointMembers;
    }

    public String getCluster() {
        return cluster;
    }

    private void randomGossipDigest(List<GossipDigest> digests) throws UnknownHostException {
        List<GossipMember> endpoints = new ArrayList<>(endpointMembers.keySet());
        Collections.shuffle(endpoints, random);
        for (GossipMember ep : endpoints) {
            HeartbeatState hb = endpointMembers.get(ep);
            long hbTime = 0;
            long hbVersion = 0;
            if (hb != null) {
                hbTime = hb.getHeartbeatTime();
                hbVersion = hb.getVersion();
            }
            digests.add(new GossipDigest(ep, hbTime, hbVersion));
        }
    }

    private Buffer encodeSyncMessage(List<GossipDigest> digests) {
        Buffer buffer = Buffer.buffer();
        JsonArray array = new JsonArray();
        for (GossipDigest e : digests) {
            array.add(Serializer.getInstance().encode(e).toString());
        }
        buffer.appendString(GossipMessageFactory.getInstance().makeMessage(MessageType.SYNC_MESSAGE, array.encode(), getCluster(), getSelf().ipSplicePort()).encode());
        return buffer;
    }

    public Buffer encodeAckMessage(AckMessage ackMessage) {
        Buffer buffer = Buffer.buffer();
        JsonObject ackJson = JsonObject.mapFrom(ackMessage);
        buffer.appendString(GossipMessageFactory.getInstance().makeMessage(MessageType.ACK_MESSAGE, ackJson.encode(), getCluster(), getSelf().ipSplicePort()).encode());
        return buffer;
    }

    public Buffer encodeAck2Message(Ack2Message ack2Message) {
        Buffer buffer = Buffer.buffer();
        JsonObject ack2Json = JsonObject.mapFrom(ack2Message);
        buffer.appendString(GossipMessageFactory.getInstance().makeMessage(MessageType.ACK2_MESSAGE, ack2Json.encode(), getCluster(), getSelf().ipSplicePort()).encode());
        return buffer;
    }

    private Buffer encodeShutdownMessage() {
        Buffer buffer = Buffer.buffer();
        JsonObject self = JsonObject.mapFrom(getSelf());
        buffer.appendString(GossipMessageFactory.getInstance().makeMessage(MessageType.SHUTDOWN, self.encode(), getCluster(), getSelf().ipSplicePort()).encode());
        return buffer;
    }

    private Buffer encodeRegularMessage(RegularMessage regularMessage) {
        Buffer buffer = Buffer.buffer();
        JsonObject msg = JsonObject.mapFrom(regularMessage);
        buffer.appendString(GossipMessageFactory.getInstance().makeMessage(MessageType.REG_MESSAGE, msg.encode(), getCluster(), getSelf().ipSplicePort()).encode());
        return buffer;
    }

    public void apply2LocalState(Map<GossipMember, HeartbeatState> endpointMembers) {
        Set<GossipMember> keys = endpointMembers.keySet();
        for (GossipMember m : keys) {
            if (getSelf().equals(m)) {
                continue;
            }

            try {
                HeartbeatState localState = getEndpointMembers().get(m);
                HeartbeatState remoteState = endpointMembers.get(m);

                if (localState != null) {
                    long localHeartbeatTime = localState.getHeartbeatTime();
                    long remoteHeartbeatTime = remoteState.getHeartbeatTime();
                    if (remoteHeartbeatTime > localHeartbeatTime) {
                        remoteStateReplaceLocalState(m, remoteState);
                    } else if (remoteHeartbeatTime == localHeartbeatTime) {
                        long localVersion = localState.getVersion();
                        long remoteVersion = remoteState.getVersion();
                        if (remoteVersion > localVersion) {
                            remoteStateReplaceLocalState(m, remoteState);
                        }
                    }
                } else {
                    remoteStateReplaceLocalState(m, remoteState);
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    private void remoteStateReplaceLocalState(GossipMember member, HeartbeatState remoteState) {
        if (member.getState() == GossipState.UP) {
            up(member);
        }
        if (member.getState() == GossipState.DOWN) {
            down(member);
        }
        if (endpointMembers.containsKey(member)) {
            endpointMembers.remove(member);
        }
        endpointMembers.put(member, remoteState);
    }

    public GossipMember createByDigest(GossipDigest digest) {
        GossipMember member = new GossipMember();
        member.setPort(digest.getEndpoint().getPort());
        member.setIpAddress(digest.getEndpoint().getAddress().getHostAddress());
        member.setCluster(cluster);

        Set<GossipMember> keys = getEndpointMembers().keySet();
        for (GossipMember m : keys) {
            if (m.equals(member)) {
                member.setId(m.getId());
                member.setState(m.getState());
                break;
            }
        }

        return member;
    }

    /**
     * 发送sync信息至活结点
     *
     * @param buffer 数据信息
     * @return 如果至少发送给一个则返回true，否则返回false
     */
    private boolean gossip2LiveMember(Buffer buffer) {
        int liveSize = liveMembers.size();
        if (liveSize <= 0) {
            return false;
        }
        boolean b = false;
        int c = Math.min(liveSize, convergenceCount());
        for (int i = 0; i < c; i++) {
            int index = random.nextInt(liveSize);
            b = b || sendGossip(buffer, liveMembers, index);
        }
        return b;
    }

    /**
     * 发送sync信息至死结点
     *
     * @param buffer 数据信息
     */
    private void gossip2UndiscoverableMember(Buffer buffer) {
        int deadSize = deadMembers.size();
        if (deadSize <= 0) {
            return;
        }
        int index = (deadSize == 1) ? 0 : random.nextInt(deadSize);
        sendGossip(buffer, deadMembers, index);
    }

    private void gossip2Seed(Buffer buffer) {
        int size = settings.getSeedMembers().size();
        if (size > 0) {
            if (size == 1 && isSeedNode()) {
                return;
            }
            int index = (size == 1) ? 0 : random.nextInt(size);
            if (liveMembers.size() == 1) {
                sendGossip2Seed(buffer, settings.getSeedMembers(), index);
            } else {
                double prob = size / (double) liveMembers.size();
                if (random.nextDouble() < prob) {
                    sendGossip2Seed(buffer, settings.getSeedMembers(), index);
                }
            }
        }
    }

    private boolean sendGossip(Buffer buffer, List<GossipMember> members, int index) {
        if (buffer != null && index >= 0) {
            try {
                GossipMember target = members.get(index);
                if (target.equals(getSelf())) {
                    int m_size = members.size();
                    if (m_size == 1) {
                        return false;
                    } else {
                        target = members.get((index + 1) % m_size);
                    }
                }
                settings.getMsgService().sendMsg(target.getIpAddress(), target.getPort(), buffer);
                return settings.getSeedMembers().contains(gossipMember2SeedMember(target));
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
        return false;
    }

    private boolean sendGossip2Seed(Buffer buffer, List<SeedMember> members, int index) {
        if (buffer != null && index >= 0) {
            try {
                SeedMember target = members.get(index);
                int m_size = members.size();
                if (target.equals(gossipMember2SeedMember(getSelf()))) {
                    if (m_size <= 1) {
                        return false;
                    } else {
                        target = members.get((index + 1) % m_size);
                    }
                }
                settings.getMsgService().sendMsg(target.getIpAddress(), target.getPort(), buffer);
                return true;
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
        return false;
    }

    private SeedMember gossipMember2SeedMember(GossipMember member) {
        return new SeedMember(member.getCluster(), member.getIpAddress(), member.getPort(), member.getId());
    }

    /**
     * 检查各个节点的心跳时间，如果超过阈值则认为该节点已经 down
     */
    private void checkStatus() {
        try {
            GossipMember local = getSelf();
            Map<GossipMember, HeartbeatState> endpoints = getEndpointMembers();
            Set<GossipMember> epKeys = endpoints.keySet();
            for (GossipMember k : epKeys) {
                if (!k.equals(local)) {
                    HeartbeatState state = endpoints.get(k);
                    long now = System.currentTimeMillis();
                    long duration = now - state.getHeartbeatTime();
                    long convictedTime = convictedTime();
                    LOGGER.info("检测 : " + k + " 状态 : " + state + " 延时 : " + duration + " 用时 : " + convictedTime);
                    if (duration > convictedTime && (isAlive(k) || getLiveMembers().contains(k))) {
                        downing(k, state);
                    }
                    if (duration <= convictedTime && (isDiscoverable(k) || getDeadMembers().contains(k))) {
                        up(k);
                    }
                }
            }
            checkCandidate();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    private int convergenceCount() {
        int size = getEndpointMembers().size();
        return (int) Math.floor(Math.log10(size) + Math.log(size) + 1);
    }

    private long convictedTime() {
        long executeGossipTime = 500;
        return ((convergenceCount() * (settings.getNetworkDelay() * 3L + executeGossipTime)) << 1) + settings.getGossipInterval();
    }

    private boolean isDiscoverable(GossipMember member) {
        return member.getState() == GossipState.JOIN || member.getState() == GossipState.DOWN;
    }

    private boolean isAlive(GossipMember member) {
        return member.getState() == GossipState.UP;
    }

    public boolean isSeedNode() {
        if (isSeedNode == null) {
            isSeedNode = settings.getSeedMembers().contains(gossipMember2SeedMember(getSelf()));
        }
        return isSeedNode;
    }

    private void fireGossipEvent(GossipMember member, GossipState state) {
        fireGossipEvent(member, state, null);
    }

    /**
     * 发送事件
     *
     * @param member  成员
     * @param state   状态
     * @param payload 负载
     */
    public void fireGossipEvent(GossipMember member, GossipState state, Object payload) {
        if (getListener() != null) {
            if (state == GossipState.RCV) {
                new Thread(() -> getListener().gossipEvent(member, state, payload)).start();
            } else {
                getListener().gossipEvent(member, state, payload);
            }
        }
    }

    public void down(GossipMember member) {
        LOGGER.info("节点断开！");
        try {
            rwlock.writeLock().lock();
            member.setState(GossipState.DOWN);
            liveMembers.remove(member);
            if (!deadMembers.contains(member)) {
                deadMembers.add(member);
            }
            fireGossipEvent(member, GossipState.DOWN);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    private void up(GossipMember member) {
        try {
            rwlock.writeLock().lock();
            member.setState(GossipState.UP);
            if (!liveMembers.contains(member)) {
                liveMembers.add(member);
            }
            if (candidateMembers.containsKey(member)) {
                candidateMembers.remove(member);
            }
            if (deadMembers.contains(member)) {
                deadMembers.remove(member);
                LOGGER.info("节点加入！！");
                if (!member.equals(getSelf())) {
                    fireGossipEvent(member, GossipState.UP);
                }
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        } finally {
            rwlock.writeLock().unlock();
        }

    }

    private void downing(GossipMember member, HeartbeatState state) {
        LOGGER.info("正在下线……");
        try {
            if (candidateMembers.containsKey(member)) {
                CandidateMemberState cState = candidateMembers.get(member);
                if (state.getHeartbeatTime() == cState.getHeartbeatTime()) {
                    cState.updateCount();
                } else if (state.getHeartbeatTime() > cState.getHeartbeatTime()) {
                    candidateMembers.remove(member);
                }
            } else {
                candidateMembers.put(member, new CandidateMemberState(state.getHeartbeatTime()));
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void checkCandidate() {
        Set<GossipMember> keys = candidateMembers.keySet();
        for (GossipMember m : keys) {
            if (candidateMembers.get(m).getDowningCount().get() >= convergenceCount()) {
                down(m);
                candidateMembers.remove(m);
            }
        }
    }

    protected void shutdown() {
        getSettings().getMsgService().unListen();
        doGossipExecutor.shutdown();
        try {
            Thread.sleep(getSettings().getGossipInterval());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Buffer buffer = encodeShutdownMessage();
        for (int i = 0; i < getLiveMembers().size(); i++) {
            sendGossip(buffer, getLiveMembers(), i);
        }
        isWorking = false;
    }

    public void publish(Object payload) {
        RegularMessage msg = new RegularMessage(getSelf(), payload, convictedTime());
        settings.getMessageManager().add(msg);
    }

    class GossipTask implements Runnable {

        @Override
        public void run() {
            //更新本地版本信息
            long version = endpointMembers.get(getSelf()).updateVersion();
            if (isDiscoverable(getSelf())) {
                up(getSelf());
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("同步数据");
                LOGGER.trace(String.format("更新后的 heartbeat version为 %d", version));
            }

            List<GossipDigest> digests = new ArrayList<>();
            try {
                randomGossipDigest(digests);
                if (digests.size() > 0) {
                    Buffer syncMessageBuffer = encodeSyncMessage(digests);
                    sendBuf(syncMessageBuffer);
                }
                checkStatus();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("live member : " + getLiveMembers());
                    LOGGER.trace("dead member : " + getDeadMembers());
                    LOGGER.trace("endpoint : " + getEndpointMembers());
                }
                new Thread(() -> {
                    MessageManager mm = settings.getMessageManager();
                    if (!mm.isEmpty()) {
                        for (String id : mm.list()) {
                            RegularMessage msg = mm.acquire(id);
                            int c = msg.getForwardCount();
                            int maxTry = convergenceCount();
                            if (c < maxTry) {
                                sendBuf(encodeRegularMessage(msg));
                                msg.setForwardCount(c + 1);
                            }
                            if ((System.currentTimeMillis() - msg.getCreateTime()) >= msg.getTtl()) {
                                mm.remove(id);
                            }
                        }
                    }
                }).start();
            } catch (UnknownHostException e) {
                LOGGER.error(e.getMessage());
            }

        }

        private void sendBuf(Buffer buf) {
            // 随机 live 结点
            boolean b = gossip2LiveMember(buf);

            // 从 dead 结点中取一个
            gossip2UndiscoverableMember(buf);

            //step3.
            if (!b || liveMembers.size() <= settings.getSeedMembers().size()) {
                gossip2Seed(buf);
            }
        }
    }

}