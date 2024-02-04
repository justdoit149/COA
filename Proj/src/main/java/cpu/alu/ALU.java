package cpu.alu;
import util.BinaryIntegers;
import util.DataType;
/**
 * ALU封装类,dest为被操作数，src为操作数，如dest - src
 */
public class ALU {
    DataType remainderReg = new DataType("00000000000000000000000000000000");
    DataType zero = new DataType(BinaryIntegers.ZERO);
    DataType NumOne = new DataType(BinaryIntegers.One);

    public DataType add(DataType src, DataType dest) {
        StringBuilder sb = new StringBuilder(dest.toString());
        String srcstr = src.toString();
        int carry = 0;
        int temp = 0;
        for(int i = 31; i >= 0; i--){
            temp = Integer.parseInt(sb.charAt(i) + "") + Integer.parseInt(srcstr.charAt(i) + "") + carry;
            carry = temp / 2;
            sb.setCharAt(i,(char)(temp%2 + '0'));
        }
        return new DataType(sb.toString());
    }

    public DataType sub(DataType src, DataType dest) {
        StringBuilder sb = new StringBuilder(src.toString());
        for(int i = 31; i >= 0; i--){
            sb.setCharAt(i,(sb.charAt(i) == '1'?'0':'1'));
        }
        DataType temp = add(new DataType(sb.toString()), new DataType(BinaryIntegers.One));
        return add(temp,dest);
    }

    public DataType mul(DataType src, DataType dest) {//只取32位结果
        String ans = mul(src, dest, 32);
        return new DataType(ans);
    }

    public String mul(DataType src, DataType dest, int len_ans) {//重载，最高可以计算64位的。
        StringBuilder ans = new StringBuilder(105);
        ans.append(zero.toString());//乘法不需要扩展成64位，前面直接补0即可。
        ans.append(src.toString() + "0");
        for(int count = 0; count < 32; count++){
            int f = ans.charAt(64) - ans.charAt(63);
            DataType temp1 = new DataType(ans.substring(0,32));
            if(f == 1) ans.replace(0,32,add(dest,temp1).toString());
            else if(f == -1) ans.replace(0,32,sub(dest,temp1).toString());
            ans.insert(0,ans.charAt(0));
        }
        return ans.substring(64-len_ans,64);
    }

    public DataType div(DataType src, DataType dest) {
        String src_str = src.toString();
        String dest_str = dest.toString();
        //除数为0异常。
        if(src_str.equals(zero.toString())) {
            throw new ArithmeticException();
        }
        //初始化：被除数扩展成64位，前面补上被除数的符号位
        StringBuilder ans = new StringBuilder(105);
        for(int i = 0; i < 32; i++){
            ans.append(dest_str.charAt(0));
        }
        ans.append(dest.toString());
        DataType temp1 = new DataType(ans.substring(0,32));
        //第一次处理：判断X和Y符号
        boolean isSame = (src_str.charAt(0) == dest_str.charAt(0));
        ans.replace(0,32,isSame?sub(src,temp1).toString():add(src,temp1).toString());
        //N次循环
        for(int i = 0; i < 32; i++){
            isSame = (ans.charAt(0) == src_str.charAt(0));
            ans.append(isSame ? '1' : '0');
            ans.deleteCharAt(0);
            temp1 = new DataType(ans.substring(0,32));
            ans.replace(0,32,isSame?sub(src,temp1).toString():add(src,temp1).toString());
        }
        //商修正
        isSame = (src_str.charAt(0) == dest_str.charAt(0));//这里isSame含义发生了改变
        ans.deleteCharAt(32);
        ans.append(ans.charAt(0) == src_str.charAt(0) ? '1' : '0');
        if(!isSame){
            temp1 = new DataType(ans.substring(32,64));
            ans.replace(32,64,add(NumOne,temp1).toString());
        }
        //余数修正
        if(ans.charAt(0) != dest_str.charAt(0)){
            temp1 = new DataType(ans.substring(0,32));
            ans.replace(0,32,isSame?add(src,temp1).toString():sub(src,temp1).toString());
        }
        //算法固有BUG修正：被除数为负且整除时，余数和除数相同或相反。
        remainderReg = new DataType(ans.substring(0,32));
        if(add(remainderReg,new DataType(src_str)).toString().equals(zero.toString())){
            remainderReg = zero;
            return sub(NumOne,new DataType(ans.substring(32,64)));
        }else if(sub(remainderReg,new DataType(src_str)).toString().equals(zero.toString())){
            remainderReg = zero;
            return add(NumOne,new DataType(ans.substring(32,64)));
        }
        return new DataType(ans.substring(32,64));
    }

    public String div(DataType src, DataType dest, int len_ans) {//只计算正数除法，不需要考虑恢复不恢复余数。
        // 结果的小数点位置不定，若被除数大那前面是整数部分后面是小数部分但是分界线不定，若被除数比除数小那第一位是0后面是小数部分。
        DataType src0 = new DataType(src.toString());
        DataType dest0 = new DataType(dest.toString());
        if(src.toString().equals(zero.toString())) throw new ArithmeticException();
        StringBuilder ans = new StringBuilder(105);
        for(int i = 0; i < len_ans; i++){
            if(sub(src0,dest0).toString().charAt(0) == '0'){
                dest0 = sub(src0,dest0);
                ans.append('1');
            }else{
                ans.append('0');
            }
            dest0 = add(dest0,dest0);
        }
        return ans.toString();
    }
}