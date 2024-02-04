package memory.cache.cacheReplacementStrategy;

import memory.Memory;
import memory.cache.Cache;

/**
 * TODO 最近最少用算法
 */
public class LRUReplacement implements ReplacementStrategy {
    //保证时间戳是0到t-1 (t<=n) 的一个排列。但是这个对架构产生了一定改动，不知道有没有更好的实现方法。
    Cache cache = Cache.getCache();
    Memory memory = Memory.getMemory();
    @Override
    public void hit(int rowNO) {
        //TODO
        int start = rowNO / cache.getSetSize() * cache.getSetSize();
        int end = start + cache.getSetSize();
        int count = 0;
        for(int i = start; i < end; i++){
            if(cache.isValid(i) && i != rowNO){//把时间戳比rowNO大的都减一
                if(cache.getTimeStamp(i) > cache.getTimeStamp(rowNO)) {
                    cache.setTimeStamp(i,cache.getTimeStamp(i)-1);
                }
                count++;
            }
        }
        cache.setTimeStamp(rowNO,count);//把rowN0的时间戳更新成之前的有效行数
    }

    @Override
    public int replace(int start, int end, char[] addrTag, byte[] input) {
        //TODO
        for (int i = start; i < end; i++) {
            if (cache.getTimeStamp(i) == 0) {
                if(Cache.isWriteBack){
                    if(cache.isValid(i) && cache.isDirty(i)){
                        String addr = cache.calculatePAddr(i);
                        memory.write(addr, Cache.LINE_SIZE_B, input);
                    }
                }
                cache.update(i, addrTag, input);
                for(int j = start; j < end; j++){
                    if(i != j){
                        cache.setTimeStamp(j, Math.max(0, cache.getTimeStamp(j)-1));
                    }else{
                        cache.setTimeStamp(j, end - start - 1);
                    }
                }
                return i;
            }
        }
        return -1;
    }
}