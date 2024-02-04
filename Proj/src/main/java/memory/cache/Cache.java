package memory.cache;

import memory.Memory;
import memory.cache.cacheReplacementStrategy.ReplacementStrategy;
import util.Transformer;
import java.util.Arrays;

/**
 * 高速缓存抽象类
 */
public class Cache {
    public static boolean isAvailable = true; // 默认启用Cache
    public static final int CACHE_SIZE_B = 32 * 1024; // 32 KB 总大小
    public static final int LINE_SIZE_B = 64; // 64 B 行大小
    private final CacheLine[] cache = new CacheLine[CACHE_SIZE_B / LINE_SIZE_B];
    private int SETS;   // 组数
    private int setSize;    // 每组行数
    private static final Cache cacheInstance = new Cache();// 单例模式
    private ReplacementStrategy replacementStrategy;    // 替换策略
    public static boolean isWriteBack;   // 写策略

    private Cache() {//private的构造方法，让用户无法创建cache对象
        for (int i = 0; i < cache.length; i++) {
            cache[i] = new CacheLine();
        }
    }

    public static Cache getCache() {
        return cacheInstance;
    }//用户只能用这个方法获取一个唯一Cache对象。

    public static int myLog(int n){//计算以2为底的对数，用于确定位数。
        for(int i = 0; i < 32; i++){
            if(Math.pow(2,i) == n) return i;
        }
        return -1;
    }

    public char[] getTag(String pAddr){//根据内存地址、结合关联度计算26位tag标记
        int SETS_len = myLog(SETS);
        char[] tag_temp = new char[26];
        for(int j = 0; j < tag_temp.length; j++){
            tag_temp[j] = (j < SETS_len ? '0' : pAddr.charAt(j-SETS_len));
        }
        return tag_temp;
    }

    public String calculatePAddr(int rowNO) {//根据行号、结合关联度计算内存地址
        int SETS_len = myLog(SETS);
        String setNo = Transformer.intToBinary("" + rowNO / setSize).substring(32 - SETS_len, 32);
        char[] tag = cache[rowNO].tag;
        return new String(tag).substring(SETS_len, tag.length) + setNo + "000000";
    }

    /**
     * 读取[pAddr, pAddr + len)范围内的连续数据，可能包含多个数据块的内容
     *
     * @param pAddr 数据起始点(32位物理地址 = 26位块号 + 6位块内地址)
     * @param len   待读数据的字节数
     * @return 读取出的数据，以char数组的形式返回
     */
    public byte[] read(String pAddr, int len) {
        byte[] data = new byte[len];
        //addr：当前块的起始地址。
        int addr = Integer.parseInt(Transformer.binaryToInt("0" + pAddr));
        int upperBound = addr + len;
        int index = 0;
        while (addr < upperBound) {
            //下一块的大小。一般是LINE_SIZE_B，开头和结尾取实际的大小
            int nextSegLen = LINE_SIZE_B - (addr % LINE_SIZE_B);//处理开头
            if (addr + nextSegLen >= upperBound) {//处理结尾
                nextSegLen = upperBound - addr;
            }
            int rowNO = fetch(Transformer.intToBinary(String.valueOf(addr)));
            byte[] cache_data = cache[rowNO].getData();
            int i = 0;
            while (i < nextSegLen) {
                data[index] = cache_data[addr % LINE_SIZE_B + i];
                index++;
                i++;
            }
            addr += nextSegLen;
        }
        return data;
    }

    /**
     * 向cache中写入[pAddr, pAddr + len)范围内的连续数据，可能包含多个数据块的内容
     *
     * @param pAddr 数据起始点(32位物理地址 = 26位块号 + 6位块内地址)
     * @param len   待写数据的字节数
     * @param data  待写数据
     */
    public void write(String pAddr, int len, byte[] data) {
        int addr = Integer.parseInt(Transformer.binaryToInt("0" + pAddr));
        int upperBound = addr + len;
        int index = 0;
        while (addr < upperBound) {
            int nextSegLen = LINE_SIZE_B - (addr % LINE_SIZE_B);
            if (addr + nextSegLen >= upperBound) {
                nextSegLen = upperBound - addr;
            }
            int rowNO = fetch(Transformer.intToBinary(String.valueOf(addr)));
            byte[] cache_data = cache[rowNO].getData();//这个不是拷贝，是指针，可以通过这个修改cache
            int i = 0;
            while (i < nextSegLen) {//这里已经写入cache了，不需要再对cache更新。下面只需要写入主存。
                cache_data[addr % LINE_SIZE_B + i] = data[index];
                index++;
                i++;
            }
            //TODO:
            if(isWriteBack){
                //写回：这块数据变成dirty的了，可以读（此时内存中是无效、cache中有效），被替换的时候需要写回。
                //如果是改了内存，变成内存有效、cache无效，内存write写入时通知cache无效，用validBit为false约束。
                cache[rowNO].dirty = true;
            }else{
                Memory.getMemory().write(calculatePAddr(rowNO),LINE_SIZE_B,cache_data);
                cache[rowNO].validBit = true;
            }
            addr += nextSegLen;
        }
    }

    /**
     * 查询{@link Cache#cache}表以确认包含pAddr的数据块是否在cache内
     * 如果目标数据块不在Cache内，则将其从内存加载到Cache
     *
     * @param pAddr 数据起始点(32位物理地址 = 26位块号 + 6位块内地址)
     * @return 数据块在Cache中的对应行号
     */
    private int fetch(String pAddr) {
        //TODO
        int blockID = getBlockNO(pAddr);
        int cacheID = map(blockID);
        if(cacheID >= 0){//命中
            replacementStrategy.hit(cacheID);
            return cacheID;
        }else{//处理未命中的情况
            int setID_num = blockID % SETS;
            int start = setID_num * setSize, end = (setID_num+1) * setSize;
            char[] tag_temp = getTag(pAddr);
            byte[] input = Memory.getMemory().read(pAddr.substring(0,26)+"000000", LINE_SIZE_B);
            for(int i = start; i < end; i++){
                if(!cache[i].validBit){
                    update(i, tag_temp, input);
                    if(replacementStrategy != null){//这个判断应对不需要替换、未指定替换策略的情况。
                        replacementStrategy.hit(i);//这里主要是为了保证LRU的初始化。
                    }
                    return i;//有空位的情况
                }
            }
            return replacementStrategy.replace(start, end, tag_temp, input);//没有空位的情况。
        }
    }

    /**
     * 根据目标数据内存地址前26位的int表示，进行映射
     *
     * @param blockNO 数据在内存中的块号
     * @return 返回cache中所对应的行，-1表示未命中
     */
    private int map(int blockNO) {
        //TODO
        int setID_num = blockNO % SETS;
        int tagID_num = blockNO / SETS;
        for(int i = setID_num * setSize; i < (setID_num+1) * setSize; i++){
            int tag_temp = Integer.parseInt(Transformer.binaryToInt(String.valueOf(cache[i].getTag())));
            if(cache[i].validBit && tag_temp == tagID_num){
                return i;
            }
        }
        return -1;
    }

    /**
     * 更新cache
     *
     * @param rowNO 需要更新的cache行号
     * @param tag   待更新数据的Tag
     * @param input 待更新的数据
     */
    public void update(int rowNO, char[] tag, byte[] input) {
        //TODO
        cache[rowNO].validBit = true;//更新数据，需要修改有效位、标记、数据。
        for(int j = 0; j < cache[rowNO].tag.length; j++){
            cache[rowNO].tag[j] = tag[j];
        }
        for(int j = 0; j < LINE_SIZE_B; j++){
            cache[rowNO].data[j] = input[j];
        }
    }

    private int getBlockNO(String pAddr) {//根据物理地址，获取目标数据在内存中对应的块号
        return Integer.parseInt(Transformer.binaryToInt("0" + pAddr.substring(0, 26)));
    }

    //使用策略模式，设置cache的替换策略
    public void setReplacementStrategy(ReplacementStrategy r) { this.replacementStrategy = r;}

    public void setSETS(int SETS) { this.SETS = SETS;}//设置组数

    public void setSetSize(int setSize) { this.setSize = setSize;}//设置每组行数

    public int getSetSize() { return this.setSize;}//获取每组行数

    public void invalid(String pAddr, int len) {//告知Cache某个连续地址范围内的数据发生了修改，缓存失效
        int from = getBlockNO(pAddr);
        int to = getBlockNO(Transformer.intToBinary(String.valueOf(Integer.parseInt(Transformer.binaryToInt("0" + pAddr)) + len - 1)));
        for (int blockNO = from; blockNO <= to; blockNO++) {
            int rowNO = map(blockNO);
            if (rowNO != -1) {
                cache[rowNO].validBit = false;
            }
        }
    }

    public void clear() {//清空cache
        for (CacheLine line : cache) {
            if (line != null) {
                line.validBit = false;
                line.dirty = false;
                line.timeStamp = 0L;
                line.visited = 0;
            }
        }
    }

    /**
     * 输入行号和对应的预期值，判断Cache当前状态是否符合预期
     * 这个方法仅用于测试，请勿修改
     *
     * @param lineNOs     行号
     * @param validations 有效值
     * @param tags        tag
     * @return 判断结果
     */
    public boolean checkStatus(int[] lineNOs, boolean[] validations, char[][] tags) {
        if (lineNOs.length != validations.length || validations.length != tags.length) {
            return false;
        }
        for (int i = 0; i < lineNOs.length; i++) {
            CacheLine line = cache[lineNOs[i]];
            if (line.validBit != validations[i]) {
                return false;
            }
            if (!Arrays.equals(line.getTag(), tags[i])) {
                return false;
            }
        }
        return true;
    }

    //以下内容用于CacheLine内部类

    // 获取有效位
    public boolean isValid(int rowNO){
        return cache[rowNO].validBit;
    }

    // 获取脏位
    public boolean isDirty(int rowNO){
        return cache[rowNO].dirty;
    }

    // LFU算法增加访问次数
    public void addVisited(int rowNO){
        cache[rowNO].visited++;
    }

    // 获取访问次数
    public int getVisited(int rowNO){
        return cache[rowNO].visited;
    }

    // （自己实现的，不知道给的那个有什么用）用于LRU算法，重置时间戳
    public void setTimeStamp(int rowNO, long newTimeStamp){
        cache[rowNO].timeStamp = newTimeStamp;
    }

    //用于FIFO算法，重置时间戳
    public void setTimeStampFIFO(int rowNo){
        cache[rowNo].timeStamp = 1L;
        if((rowNo+1)%setSize == 0){
            cache[rowNo+1-setSize].timeStamp = 0L;
        }else{
            cache[rowNo+1].timeStamp = 0L;
        }
    }

    // 获取时间戳
    public long getTimeStamp(int rowNO){
        return cache[rowNO].timeStamp;
    }

    // 获取该行数据
    public byte[] getData(int rowNO){
        return cache[rowNO].data;
    }

    /**
     * Cache行，每行长度为(1+22+{@link Cache#LINE_SIZE_B})
     */
    private static class CacheLine {
        boolean validBit = false;// 有效位，标记该条数据是否有效
        boolean dirty = false;// 脏位，标记该条数据是否被修改
        int visited = 0;// 用于LFU算法，记录该条cache使用次数
        Long timeStamp = 0L;// 用于LRU和FIFO算法，记录该条数据时间戳
        // 标记，占位长度为26位，有效长度取决于映射策略；在前面补0后，有效数据按照从前往后的顺序：
        // (2^n)-路组关联映射: 26-(9-n) 位
        char[] tag = new char[26];
        byte[] data = new byte[LINE_SIZE_B];// 数据
        byte[] getData() {
            return this.data;
        }
        char[] getTag() {
            return this.tag;
        }
    }
}
