package net;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 请求计数器
 *
 * @author: Pikachudy
 * @date: 2022/12/6
 */
@Data
public class RequestCounter {
    private boolean isOpen;
    private int count;
    private int request_num;
    private List<String> result;

    public RequestCounter(int request_num) {
        this.request_num = request_num;
        result = new ArrayList<>();
        isOpen = false;
    }

    /**
     * 将计数器设定为 request_num
     */
    public void resetCount() {
        this.count = this.request_num;
    }
    public void resetResult(){
        result.clear();
    }

    /**
     * 计数减 1
     */
    public void countDown() {
        this.count--;
    }

    /**
     * 增加结果
     *
     * @param result
     */
    public void addResult(String result) {
        this.result.add(result);
    }

}
