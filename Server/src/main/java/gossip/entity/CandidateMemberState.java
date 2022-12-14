package gossip.entity;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 候选节点状态
 *
 * @author Pikachudy
 * @date 2022/12/11
 */
@Data
public class CandidateMemberState {
    private long heartbeatTime;
    private AtomicInteger downingCount;

    public CandidateMemberState(long heartbeatTime) {
        this.heartbeatTime = heartbeatTime;
        this.downingCount = new AtomicInteger(0);
    }

    public void updateCount() {
        this.downingCount.incrementAndGet();
    }

    @Override
    public String toString() {
        return "CandidateMemberState{" +
                "heartbeatTime=" + heartbeatTime +
                ", downingCount=" + downingCount.get() +
                '}';
    }
}
