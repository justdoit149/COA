package memory.cache.cacheReplacementStrategy;

import memory.Memory;
import memory.cache.Cache;

/**
 * TODO 最近不经常使用算法
 */
public class LFUReplacement implements ReplacementStrategy {
    Cache cache = Cache.getCache();
    Memory memory = Memory.getMemory();
    @Override
    public void hit(int rowNO) {
        //TODO
        cache.addVisited(rowNO);//第一次访问是1，后续每访问一次则自增。
    }

    @Override
    public int replace(int start, int end, char[] addrTag, byte[] input) {
        //TODO
        int index = start;
        for(int i = start; i < end; i++){
            if(cache.getVisited(i) < cache.getVisited(index)){//不取等，找到第一个满足的。
                index = i;
            }
        }
        if(Cache.isWriteBack){
            if(cache.isValid(index) && cache.isDirty(index)){
                String addr = cache.calculatePAddr(index);
                memory.write(addr, Cache.LINE_SIZE_B, input);
            }
        }
        cache.update(index, addrTag, input);
        return index;
    }

}