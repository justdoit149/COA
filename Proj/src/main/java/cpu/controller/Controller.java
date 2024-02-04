package cpu.controller;

import cpu.alu.ALU;
import memory.Memory;
import util.DataType;
import util.Transformer;

import java.util.Arrays;


public class Controller {
    // general purpose register
    char[][] GPR = new char[32][32];
    // program counter
    char[] PC = new char[32];
    // instruction register
    char[] IR = new char[32];
    // memory address register
    char[] MAR = new char[32];
    // memory buffer register
    char[] MBR =  new char[32];
    char[] ICC = new char[2];

    // 单例模式
    private static final Controller controller = new Controller();

    private Controller(){
        //规定第0个寄存器为zero寄存器
        GPR[0] = new char[]{'0','0','0','0','0','0','0','0',
                '0','0','0','0','0','0','0','0',
                '0','0','0','0','0','0','0','0',
                '0','0','0','0','0','0','0','0'};
        ICC = new char[]{'0','0'}; // ICC初始化为00
    }

    public static Controller getController(){
        return controller;
    }

    public void reset(){
        PC = new char[32];
        IR = new char[32];
        MAR = new char[32];
        GPR[0] = new char[]{'0','0','0','0','0','0','0','0',
                '0','0','0','0','0','0','0','0',
                '0','0','0','0','0','0','0','0',
                '0','0','0','0','0','0','0','0'};
        ICC = new char[]{'0','0'}; // ICC初始化为00
        interruptController.reset();
    }

    public InterruptController interruptController = new InterruptController();
    private ALU alu = new ALU();
    private Memory memory = Memory.getMemory();

    public void tick(){
        // TODO
        String ICC_str = String.valueOf(ICC);
        if(ICC_str.equals("00")){
            getInstruct();
//            System.out.println("00");
            if(String.valueOf(IR,0,7).equals("1101110")){
                ICC = "01".toCharArray();
            }else{
                ICC = "10".toCharArray();
            }
        }else if(ICC_str.equals("01")){
//            System.out.println("01");
            findOperand();
            ICC = "10".toCharArray();
        }else if(ICC_str.equals("10")){
            operate();
//            System.out.println("10");
            if(interruptController.signal){
                ICC = "11".toCharArray();
                interrupt();
                interruptController.signal = false;
                ICC = "00".toCharArray();
            }else{
                ICC = "00".toCharArray();
            }
        }else if(ICC_str.equals("11")){
            if(interruptController.signal) {
                interrupt();
                interruptController.signal = false;
            }
            ICC = "00".toCharArray();
        }
    }

    /** 执行取指操作 */
    private void getInstruct(){
        // TODO
        MAR = Arrays.copyOf(PC,32);
        MBR = byteToChar(memory.read(String.valueOf(MAR),4));
        PC = PC_next(PC);
        IR = Arrays.copyOf(MBR,32);
    }

    /** 执行间址操作 */
    private void findOperand(){
        // TODO
        int rs2 = rs2_get();
        MAR = Arrays.copyOf(GPR[rs2],32);
        GPR[rs2] = byteToChar(memory.read(String.valueOf(MAR),4));
    }

    /** 执行周期 */
    private void operate(){
        // TODO
        String opcode = String.valueOf(IR,0,7);
        if(opcode.equals("1100110") || opcode.equals("1101110")){
            add();
        }else if(opcode.equals("1100100")){
            addi();
        }else if(opcode.equals("1100000")){
            lw();
        }else if(opcode.equals("1110110")){
            lui();
        }else if(opcode.equals("1110011")){
            jalr();
        }else if(opcode.equals("1100111")){
            interruptController.signal = true;
        }else{

        }
    }

    /** 执行中断操作 */
    private void interrupt(){
        // TODO
        GPR[1] = Arrays.copyOf(PC,32);
        String opcode = String.valueOf(IR,0,7);
        if(opcode.equals("1100111")) {
            ecall();
        }else{

        }
        PC = Arrays.copyOf(GPR[1],32);
    }

    public class InterruptController{
        // 中断信号：是否发生中断
        public boolean signal;
        public StringBuffer console = new StringBuffer();
        /** 处理中断 */
        public void handleInterrupt(){
            console.append("ecall ");
        }
        public void reset(){
            signal = false;
            console = new StringBuffer();
        }
    }

    // 以下一系列的get方法用于检查寄存器中的内容进行测试，请勿修改

    // 假定代码程序存储在主存起始位置，忽略系统程序空间
    public void loadPC(){
        PC = GPR[0];
    }

    public char[] getRA() {
        //规定第1个寄存器为返回地址寄存器
        return GPR[1];
    }

    public char[] getGPR(int i) {
        return GPR[i];
    }

    private void add() {
        String rs1_num = String.valueOf(GPR[rs1_get()]);
        String rs2_num = String.valueOf(GPR[rs2_get()]);
        String rd_num = alu.add(new DataType(rs2_num), new DataType(rs1_num)).toString();
        GPR[rd_get()] = rd_num.toCharArray();
    }

    private void addi(){
        String rs1_num = String.valueOf(GPR[rs1_get()]);
        String imm_num = signExtend(String.valueOf(IR, 20, 12));
        String rd_num = alu.add(new DataType(imm_num), new DataType(rs1_num)).toString();
        GPR[rd_get()] = rd_num.toCharArray();
    }

    private void lw(){
        String rs1_num = String.valueOf(GPR[rs1_get()]);
        String imm_num = signExtend(String.valueOf(IR, 20, 12));
        String address = alu.add(new DataType(imm_num), new DataType(rs1_num)).toString();
        GPR[rd_get()] = byteToChar(memory.read(address,4));
    }

    private void lui(){
        String imm_num = String.valueOf(IR, 12, 20) + "000000000000";
        GPR[rd_get()] = imm_num.toCharArray();
    }

    private void jalr(){
        GPR[rd_get()] = Arrays.copyOf(PC,32);
        String rs1_num = String.valueOf(GPR[rs1_get()]);
        String imm_num = signExtend(String.valueOf(IR, 20, 12));
        PC = alu.add(new DataType(imm_num), new DataType(rs1_num)).toString().toCharArray();
    }

    private void ecall(){
        interruptController.handleInterrupt();
    }

    private char[] byteToChar(byte[] bytes){
        String ans = "";
        for(int i = 0; i < bytes.length; i++){
            ans += Transformer.intToBinary(bytes[i]+"").substring(24,32);
        }
        return ans.toCharArray();
    }

    private String signExtend(String str){//符号扩展成32位
        while(str.length() < 32){
            str = str.charAt(0) + str;
        }
        return str;
    }

    private char[] PC_next(char[] PC){// PC = (PC) + 4
        DataType one_word = new DataType(Transformer.intToBinary("4"));
        return alu.add(new DataType(String.valueOf(PC)), one_word).toString().toCharArray();
    }

    private int rs1_get(){
        return Integer.parseInt(Transformer.binaryToInt(String.valueOf(IR, 15, 5)));
    }

    private int rs2_get(){
        return Integer.parseInt(Transformer.binaryToInt(String.valueOf(IR, 20, 5)));
    }

    private int rd_get(){
        return Integer.parseInt(Transformer.binaryToInt(String.valueOf(IR, 7, 5)));
    }
}
