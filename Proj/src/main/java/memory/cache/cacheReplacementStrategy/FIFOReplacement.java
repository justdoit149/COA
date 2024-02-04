package memory.cache.cacheReplacementStrategy;

import memory.Memory;
import memory.cache.Cache;

/**
 * TODO 先进先出算法
 */
public class FIFOReplacement implements ReplacementStrategy {
    Cache cache = Cache.getCache();
    Memory memory = Memory.getMemory();
    @Override
    public void hit(int rowNO) {
        //TODO:
    }

    @Override
    public int replace(int start, int end, char[] addrTag, byte[] input) {
        //TODO
        for(int i = start; i < end; i++){
            if(cache.getTimeStamp(i) == 0){//第一个0就是最早进入的。
                if(Cache.isWriteBack){//写回法是写回原先的内容，所以是先写回再更新。
                    if(cache.isValid(i) && cache.isDirty(i)){
                        String addr = cache.calculatePAddr(i);
                        memory.write(addr, Cache.LINE_SIZE_B, cache.getData(i));
                    }
                }
                cache.update(i, addrTag, input);
                cache.setTimeStampFIFO(i);
                return i;
            }
        }
        return -1;
    }
}