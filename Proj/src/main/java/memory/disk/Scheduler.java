package memory.disk;

import java.util.Arrays;

/**
 * 用于决定磁头扫描策略
 * start：磁头初始位置
 * request[]：请求访问的磁道号列表
 * direction：磁头初始移动方向，true表示磁道号增大的方向，false表示磁道号减小的方向
 * 返回值为平均寻道长度
 */
public class Scheduler {
    private static final int len = Disk.TRACK_NUM;

    private int[] copyArray(int[] request){
        int[] copy = new int[request.length];//函数不能破坏原数组，所以要拷贝一份
        for(int i = 0; i < copy.length; i++){
            copy[i] = request[i];
        }
        return copy;
    }

    private double scan(int start, int[] copy, boolean direction, int l, int r) {
        //TODO
        //来——回——来——回的扫描，direction决定第一次扫描的初始方向
        //l、r表示扫描范围的上下界。用于SCAN和LOOK
        int sum = 0;
        if(direction){
            if(start <= copy[0]){
                sum = copy[copy.length-1] - start;
            }else{
                sum = Math.abs(r - start) + (r - copy[0]);
            }
        }else{
            if(start >= copy[copy.length-1]){
                sum = start - copy[0];
            }else{
                sum = Math.abs(start - l) + (copy[copy.length-1] - l);
            }
        }
        return sum * 1.0 / copy.length;
    }

    private double Cscan(int start, int[] copy, int l, int r) {
        //循环扫描，也是来——回——来——回，但回的方向只移动不扫描，但仍然计算在总长度里。
        int sum = 0;
        if(start <= copy[0]){
            sum = copy[copy.length-1] - start;
        }else{
            int index = 0;
            for(int i = 0; i < copy.length; i++){
                if(start > copy[i]){
                    index = i;
                }else{
                    break;
                }
            }
            sum = Math.abs(r - start) + (r - l) + (copy[index] - l);
        }
        return sum * 1.0 / copy.length;
    }



    // 先来先服务算法
    public double FCFS(int start, int[] request) {
        //TODO
        int sum = 0;
        for(int i = 0; i < request.length; i++){
            sum += Math.abs(start-request[i]);
            start = request[i];
        }
        return sum * 1.0 / request.length;
    }

    // 最短寻道时间优先算法
    public double SSTF(int start, int[] request) {
        //TODO
        int sum = 0;
        int min_index = 0;
        int[] copy = copyArray(request);
        for(int i = 0; i < copy.length; i++){
            for(int j = 0; j < copy.length; j++){
                if(copy[j] >= 0 && Math.abs(start-copy[j]) < Math.abs(start-copy[min_index])){
                    min_index = j;
                }
            }
            sum += Math.abs(start-copy[min_index]);
            start = copy[min_index];
            copy[min_index] = -Disk.DISK_SIZE_B;//这个要设定得与start足够远！！
        }
        return sum * 1.0 / request.length;
    }

    // 扫描算法SCAN
    public double SCAN(int start, int[] request, boolean direction) {
        //TODO
        int[] copy = copyArray(request);
        Arrays.sort(copy);
        return scan(start, copy, direction, 0, len-1);
    }

    // C-SCAN算法：默认磁头向磁道号增大方向移动
    public double CSCAN(int start,int[] request) {
        //TODO
        int[] copy = copyArray(request);
        Arrays.sort(copy);
        return Cscan(start, copy, 0, len-1);
    }

    // LOOK算法（两个LOOK是对两个SCAN的改进，不扫描到头，只扫描到request里的最大/最小）
    public double LOOK(int start,int[] request,boolean direction){
        //TODO
        int[] copy = copyArray(request);
        Arrays.sort(copy);
        return scan(start, copy, direction, copy[0], copy[copy.length-1]);
    }

    // C-LOOK算法：默认磁头向磁道号增大方向移动
    public double CLOOK(int start,int[] request) {
        //TODO
        int[] copy = copyArray(request);
        Arrays.sort(copy);
        return Cscan(start, copy, copy[0], copy[copy.length-1]);
    }
}
