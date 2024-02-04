package cpu.fpu;
import cpu.alu.ALU;
import util.DataType;
import util.IEEE754Float;
import util.BinaryIntegers;

/**
 * floating point unit
 * 执行浮点运算的抽象单元
 * 浮点数精度：使用3位保护位进行计算
 */
public class FPU {
    ALU alu = new ALU();
    private final String[][] addCorner = new String[][]{
            {IEEE754Float.P_ZERO, IEEE754Float.P_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.N_ZERO, IEEE754Float.P_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.P_ZERO, IEEE754Float.N_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.N_ZERO, IEEE754Float.N_ZERO, IEEE754Float.N_ZERO},
            {IEEE754Float.P_INF, IEEE754Float.N_INF, IEEE754Float.NaN},
            {IEEE754Float.N_INF, IEEE754Float.P_INF, IEEE754Float.NaN}
    };
    private final String[][] subCorner = new String[][]{
            {IEEE754Float.P_ZERO, IEEE754Float.P_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.N_ZERO, IEEE754Float.P_ZERO, IEEE754Float.N_ZERO},
            {IEEE754Float.P_ZERO, IEEE754Float.N_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.N_ZERO, IEEE754Float.N_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.P_INF, IEEE754Float.P_INF, IEEE754Float.NaN},
            {IEEE754Float.N_INF, IEEE754Float.N_INF, IEEE754Float.NaN}
    };
    private final String[][] mulCorner = new String[][]{
            {IEEE754Float.P_ZERO, IEEE754Float.N_ZERO, IEEE754Float.N_ZERO},
            {IEEE754Float.N_ZERO, IEEE754Float.P_ZERO, IEEE754Float.N_ZERO},
            {IEEE754Float.P_ZERO, IEEE754Float.P_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.N_ZERO, IEEE754Float.N_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.P_ZERO, IEEE754Float.P_INF, IEEE754Float.NaN},
            {IEEE754Float.P_ZERO, IEEE754Float.N_INF, IEEE754Float.NaN},
            {IEEE754Float.N_ZERO, IEEE754Float.P_INF, IEEE754Float.NaN},
            {IEEE754Float.N_ZERO, IEEE754Float.N_INF, IEEE754Float.NaN},
            {IEEE754Float.P_INF, IEEE754Float.P_ZERO, IEEE754Float.NaN},
            {IEEE754Float.P_INF, IEEE754Float.N_ZERO, IEEE754Float.NaN},
            {IEEE754Float.N_INF, IEEE754Float.P_ZERO, IEEE754Float.NaN},
            {IEEE754Float.N_INF, IEEE754Float.N_ZERO, IEEE754Float.NaN}
    };
    private final String[][] divCorner = new String[][]{
            {IEEE754Float.P_ZERO, IEEE754Float.P_ZERO, IEEE754Float.NaN},
            {IEEE754Float.N_ZERO, IEEE754Float.N_ZERO, IEEE754Float.NaN},
            {IEEE754Float.P_ZERO, IEEE754Float.N_ZERO, IEEE754Float.NaN},
            {IEEE754Float.N_ZERO, IEEE754Float.P_ZERO, IEEE754Float.NaN},
            {IEEE754Float.P_INF, IEEE754Float.P_INF, IEEE754Float.NaN},
            {IEEE754Float.N_INF, IEEE754Float.N_INF, IEEE754Float.NaN},
            {IEEE754Float.P_INF, IEEE754Float.N_INF, IEEE754Float.NaN},
            {IEEE754Float.N_INF, IEEE754Float.P_INF, IEEE754Float.NaN},
    };

    public int bTI(String binStr) {//二进制转整数，输入的二进制可以不是32位的.
        int ans = 0;
        for(int i = 0; i < binStr.length(); i++){
            ans += (binStr.charAt(i) - '0') * (1 << binStr.length()-1-i);
        }
        return ans;
    }

    public int compareFloatAbs(DataType a, DataType b){//比较浮点数的绝对值大小
        String a0 = a.toString(), b0 = b.toString();
        int seg1 = a0.substring(1,9).compareTo(b0.substring(1,9));
        int seg2 = a0.substring(9,32).compareTo(b0.substring(9,32));
        return seg1 != 0 ? seg1 : seg2;
    }

    public void fillHiddenBit(StringBuilder sb){//用来补上隐藏位。指数全0补0且指数设为1，指数不是全0则补1.
        if(sb.substring(1,9).equals("00000000")){
            sb.setCharAt(8,'1');
            sb.insert(9,'0');
        }else{
            sb.insert(9,'1');
        }
    }

    /**
     * compute the float add of (dest + src)
     */
    public DataType add(DataType src, DataType dest) {
        //TODO
        //一些重要的数据
        String ans_formal, ans_exp, ans_sign;//储存结果的符号、指数、尾数。
        StringBuilder dest_s = new StringBuilder(105), src_s = new StringBuilder(105);
        dest_s.append(dest.toString() + "000");
        src_s.append(src.toString() + "000");
        //处理各种边界情况
        String check_corner = cornerCheck(addCorner,dest.toString(),src.toString());
        if(check_corner != null){
            return new DataType(check_corner);
        }else if(dest.toString().matches(IEEE754Float.NaN_Regular) || src.toString().matches(IEEE754Float.NaN_Regular)){
            return new DataType(IEEE754Float.NaN);
        }else if(dest_s.substring(1,9).equals("11111111")){
            return dest;
        }else if(src_s.substring(1,9).equals("11111111")){
            return src;
        }
        //补上隐藏位。
        fillHiddenBit(dest_s);
        fillHiddenBit(src_s);
        //对阶。用变量n来判断谁大。这里bTI是binaryToInt，并没有直接转换而是按位转换的。ans_exp结果的指数暂时取大的。
        int n = bTI(dest_s.substring(1,9)) - bTI(src_s.substring(1,9));
        if(n >= 0){
            src_s.replace(9,36,rightShift(src_s.substring(9,36),n));
            ans_exp = dest_s.substring(1,9);
        }else{
            dest_s.replace(9,36,rightShift(dest_s.substring(9,36),-n));
            ans_exp = src_s.substring(1,9);
        }
        DataType ans_temp;
        int cmp = 0;
        //决定加还是减，cmp比较src与dest绝对值大小。同时在这一步确定结果的符号
        if(src_s.charAt(0) == dest_s.charAt(0)){
            ans_temp = alu.add(new DataType("00000"+src_s.substring(9,36)),new DataType("00000"+dest_s.substring(9,36)));
            ans_sign = src_s.charAt(0) + "";
        }else{
            cmp = compareFloatAbs(src,dest);
            if(cmp == 0){
                return new DataType(IEEE754Float.P_ZERO);
            }else if(cmp > 0){
                ans_temp = alu.sub(new DataType("00000"+dest_s.substring(9,36)),new DataType("00000"+src_s.substring(9,36)));
                ans_sign = src_s.charAt(0) + "";
            }else{
                ans_temp = alu.sub(new DataType("00000"+src_s.substring(9,36)),new DataType("00000"+dest_s.substring(9,36)));
                ans_sign = dest_s.charAt(0) + "";
            }
        }
        //尾数的规格化
        if(ans_temp.toString().charAt(4) == '1'){//尾数大于27位
            ans_formal = ans_temp.toString().substring(4,31);
            ans_exp = oneAdder(ans_exp).substring(1,9);
            if(ans_exp.equals("11111111")){
                if(ans_sign.equals("0")){
                    return new DataType(IEEE754Float.P_INF);
                }else{
                    return new DataType(IEEE754Float.N_INF);
                }
            }
        }else if(ans_temp.toString().charAt(5) == '0'){//尾数不足27位
            int i;
            DataType ans_exp_d = new DataType("000000000000000000000000" + ans_exp);
            for(i = 5; i < 32 && ans_temp.toString().charAt(i) == '0' && !ans_exp_d.toString().equals(BinaryIntegers.ZERO); i++){
                ans_exp_d = alu.add(new DataType(BinaryIntegers.NegativeOne),ans_exp_d);
            }
            if(ans_exp_d.toString().equals(BinaryIntegers.ZERO)){//这一步是为了解决规格化与非规格化之间的表示差异
                i--;
            }
            ans_exp = ans_exp_d.toString().substring(24,32);
            ans_formal = ans_temp.toString().substring(i);
            while(ans_formal.length() < 27){//如果尾数位数不够，那在后面补0
                ans_formal += "0";
            }
            ans_formal = ans_formal.substring(0,27);
        }else{//尾数恰好27位，已经规格化，直接取出来即可。
            ans_formal = ans_temp.toString().substring(5,32);
        }
        return new DataType(round(ans_sign.charAt(0),ans_exp,ans_formal));
    }

    /**
     * compute the float add of (dest - src)
     */
    public DataType sub(DataType src, DataType dest) {//对减数符号位取反，调用加法即可。
        //TODO
        StringBuilder src_temp = new StringBuilder(src.toString());
        src_temp.setCharAt(0, src_temp.charAt(0) == '0' ? '1' : '0');
        return add(new DataType(src_temp.toString()),dest);
    }

    /**
     * compute the float mul of (dest * src)
     */
    public DataType mul(DataType src,DataType dest){
        //TODO
        //一些重要的数据
        String ans_formal, ans_exp, ans_sign;
        StringBuilder dest_s = new StringBuilder(105), src_s = new StringBuilder(105);
        dest_s.append(dest.toString() + "000");
        src_s.append(src.toString() + "000");
        //处理边界情况
        String check_corner = cornerCheck(mulCorner,dest.toString(),src.toString());
        if(check_corner != null){
            return new DataType(check_corner);
        }else if(dest.toString().matches(IEEE754Float.NaN_Regular) || src.toString().matches(IEEE754Float.NaN_Regular)){
            return new DataType(IEEE754Float.NaN);
        }else if(dest_s.substring(1,9).equals("11111111") || src_s.substring(1,9).equals("11111111")){
            if(dest_s.charAt(0) == src_s.charAt(0)){
                return new DataType(IEEE754Float.P_INF);
            }else{
                return new DataType(IEEE754Float.N_INF);
            }
        }
        //隐藏位，符号位（同0异1），指数位（因为尾数位处理隐藏位需要阶码加一，直接让它在这里加），尾数位的初步处理
        fillHiddenBit(dest_s);
        fillHiddenBit(src_s);
        ans_sign = (dest_s.charAt(0) == src_s.charAt(0) ? "0" : "1");
        DataType exp1 = new DataType("000000000000000000000000" + dest_s.substring(1,9));
        DataType exp2 = new DataType("000000000000000000000000" + src_s.substring(1,9));
        DataType exp_temp = alu.sub(new DataType("000000000000000000000000"+"01111110"),alu.add(exp2,exp1));
        DataType num1 = new DataType("00000" + dest_s.substring(9,36));
        DataType num2 = new DataType("00000" + src_s.substring(9,36));
        ans_formal = alu.mul(num2,num1,54);
        //规格化处理。
        if(ans_formal.charAt(0) == '0' && alu.add(new DataType(BinaryIntegers.NegativeOne),exp_temp).toString().charAt(0) == '0'){
            //隐藏位为0且阶码大于0。
            int i;
            for(i = 0; !exp_temp.toString().equals(BinaryIntegers.ZERO); i++){
                if(i < 54 && ans_formal.charAt(i) != '0'){
                    break;
                }
                exp_temp = alu.add(new DataType(BinaryIntegers.NegativeOne),exp_temp);

            }
            if(exp_temp.toString().equals(BinaryIntegers.ZERO)){//阶码变0的处理
                i--;
            }
            ans_formal = ans_formal.substring(Math.min(i,53));
            while(ans_formal.length() < 27){//如果尾数位数不够，那在后面补0
                ans_formal += "0";
            }
        }else if(exp_temp.toString().charAt(0) == '1'){//阶码小于0，不用判断是否尾数前27位是否不为0，直接变成阶码为0即可。
            for(int i = 0; exp_temp.toString().charAt(0) == '1'; i++){
                ans_formal = rightShift(ans_formal,1);
                exp_temp = alu.sub(new DataType(BinaryIntegers.NegativeOne),exp_temp);
            }
            ans_formal = "0" + ans_formal;//阶码变0的处理
        }else if(exp_temp.toString().substring(24,32).equals("00000000")){//阶码等于0
            ans_formal = "0" + ans_formal;//阶码为0的处理
        }
        ans_exp = exp_temp.toString().substring(24,32);
        if(exp_temp.toString().charAt(23) == '1' || ans_exp.equals("11111111")){
            if(dest_s.charAt(0) == src_s.charAt(0)){//上溢处理（下溢按照0处理了，不用单独讨论）。
                return new DataType(IEEE754Float.P_INF);
            }else{
                return new DataType(IEEE754Float.N_INF);
            }
        }
        return new DataType(round(ans_sign.charAt(0),ans_exp,ans_formal));
    }

    /**
     * compute the float mul of (dest / src)
     */
    public DataType div(DataType src,DataType dest){
        //TODO
        //一些重要的数据
        String ans_formal, ans_exp, ans_sign;
        StringBuilder dest_s = new StringBuilder(105), src_s = new StringBuilder(105);
        dest_s.append(dest.toString() + "000");
        src_s.append(src.toString() + "000");
        //处理边界情况
        String check_corner = cornerCheck(divCorner,dest.toString(),src.toString());
        if(check_corner != null){
            return new DataType(check_corner);
        }else if(src.toString().substring(1,32).equals("0000000000000000000000000000000")){
            throw new ArithmeticException();
        }else if(dest.toString().matches(IEEE754Float.NaN_Regular) || src.toString().matches(IEEE754Float.NaN_Regular)) {
            return new DataType(IEEE754Float.NaN);
        }else if(dest_s.substring(1,9).equals("11111111") || src_s.substring(1,9).equals("11111111")){
            if(dest_s.charAt(0) == src_s.charAt(0) && dest_s.substring(1,9).equals("11111111")){
                return new DataType(IEEE754Float.P_INF);
            }else if(dest_s.charAt(0) != src_s.charAt(0) && dest_s.substring(1,9).equals("11111111")){
                return new DataType(IEEE754Float.N_INF);
            }else if(dest_s.charAt(0) == src_s.charAt(0) && src_s.substring(1,9).equals("11111111")){
                return new DataType(IEEE754Float.P_ZERO);
            }else if(dest_s.charAt(0) != src_s.charAt(0) && src_s.substring(1,9).equals("11111111")){
                return new DataType(IEEE754Float.N_ZERO);
            }
        }//没有最后这个else if居然也能过，测试点确实太不全面了，连无穷都不考虑……
        //隐藏位、符号位、指数位、尾数位
        fillHiddenBit(dest_s);
        fillHiddenBit(src_s);
        ans_sign = (dest_s.charAt(0) == src_s.charAt(0) ? "0" : "1");
        DataType exp1 = new DataType("000000000000000000000000" + dest_s.substring(1,9));
        DataType exp2 = new DataType("000000000000000000000000" + src_s.substring(1,9));
        DataType exp_temp = alu.add(new DataType("000000000000000000000000"+"01111111"),alu.sub(exp2,exp1));
        DataType num1 = new DataType("00000" + dest_s.substring(9,36));
        DataType num2 = new DataType("00000" + src_s.substring(9,36));
        ans_formal = alu.div(num2,num1,27);
        //规格化结果。
        if(ans_formal.charAt(0) == '0' && alu.add(new DataType(BinaryIntegers.NegativeOne),exp_temp).toString().charAt(0) == '0'){
            //隐藏位为0且阶码大于0。
            int i;
            for(i = 0; !exp_temp.toString().equals(BinaryIntegers.ZERO); i++){
                if(i < 54 && ans_formal.charAt(i) != '0'){
                    break;
                }
                exp_temp = alu.add(new DataType(BinaryIntegers.NegativeOne),exp_temp);

            }
            if(exp_temp.toString().equals(BinaryIntegers.ZERO)){//阶码变0的处理
                i--;
            }
            ans_formal = ans_formal.substring(Math.min(i,53));
            while(ans_formal.length() < 27){//如果尾数位数不够，那在后面补0
                ans_formal += "0";
            }
        }else if(exp_temp.toString().charAt(0) == '1'){//阶码小于0。
            for(int i = 0; exp_temp.toString().charAt(0) == '1'; i++){
                ans_formal = rightShift(ans_formal,1);
                exp_temp = alu.sub(new DataType(BinaryIntegers.NegativeOne),exp_temp);
            }
            ans_formal = "0" + ans_formal;//阶码变0的处理
        }else if(exp_temp.toString().substring(24,32).equals("00000000")){//阶码等于0
            ans_formal = "0" + ans_formal;//阶码为0的处理
        }
        ans_exp = exp_temp.toString().substring(24,32);
        if(exp_temp.toString().charAt(23) == '1' || ans_exp.equals("11111111")){
            if(dest_s.charAt(0) == src_s.charAt(0)){//上溢处理。
                return new DataType(IEEE754Float.P_INF);
            }else{
                return new DataType(IEEE754Float.N_INF);
            }
        }
        //由于除法没有非规格化的测试点，其实可以去掉很多代码，但考虑到完善性还是保留了，基本上除法和乘法一样。
        return new DataType(round(ans_sign.charAt(0),ans_exp,ans_formal));
    }

    /**
     * check corner cases of mul and div
     *
     * @param cornerMatrix corner cases pre-stored
     * @param oprA first operand (String)
     * @param oprB second operand (String)
     * @return the result of the corner case (String)
     */
    private String cornerCheck(String[][] cornerMatrix, String oprA, String oprB) {
        for (String[] matrix : cornerMatrix) {
            if (oprA.equals(matrix[0]) && oprB.equals(matrix[1])) {
                return matrix[2];
            }
        }
        return null;
    }

    /**
     * right shift a num without considering its sign using its string format
     *
     * @param operand to be moved
     * @param n       moving nums of bits
     * @return after moving
     */
    private String rightShift(String operand, int n) {
        StringBuilder result = new StringBuilder(operand);  //保证位数不变
        boolean sticky = false;
        for (int i = 0; i < n; i++) {
            sticky = sticky || result.toString().endsWith("1");
            result.insert(0, "0");
            result.deleteCharAt(result.length() - 1);
        }
        if (sticky) {
            result.replace(operand.length() - 1, operand.length(), "1");
        }
        return result.substring(0, operand.length());
    }

    /**
     * 对GRS保护位进行舍入
     *
     * @param sign    符号位
     * @param exp     阶码
     * @param sig_grs 带隐藏位和保护位的尾数
     * @return 舍入后的结果
     */
    private String round(char sign, String exp, String sig_grs) {
        int grs = Integer.parseInt(sig_grs.substring(24, 27), 2);
        if ((sig_grs.substring(27).contains("1")) && (grs % 2 == 0)) {
            grs++;
        }
        String sig = sig_grs.substring(0, 24); // 隐藏位+23位
        if (grs > 4) {
            //向前舍入的话那会多出来一位，所以后面是取后23位。且如果加1的操作发生了”溢出“那指数加1.
            sig = oneAdder(sig);
        } else if (grs == 4 && sig.endsWith("1")) {
            sig = oneAdder(sig);
        }
        if (Integer.parseInt(sig.substring(0, sig.length() - 23), 2) > 1) {
            sig = rightShift(sig, 1);
            exp = oneAdder(exp).substring(1);
        }
        if (exp.equals("11111111")) {
            return sign == '0' ? IEEE754Float.P_INF : IEEE754Float.N_INF;
        }
        return sign + exp + sig.substring(sig.length() - 23);
    }

    /**
     * add one to the operand
     *
     * @param operand the operand
     * @return result after adding, the first position means overflow (not equal to the carry to the next)
     *         and the remains means the result
     */
    private String oneAdder(String operand) {//这个的返回结果前面会多出来一位，用来存储可能的进位
        int len = operand.length();
        StringBuilder temp = new StringBuilder(operand);
        temp.reverse();
        int[] num = new int[len];
        for (int i = 0; i < len; i++) num[i] = temp.charAt(i) - '0';  //先转化为反转后对应的int数组
        int bit = 0x0;
        int carry = 0x1;
        char[] res = new char[len];
        for (int i = 0; i < len; i++) {
            bit = num[i] ^ carry;
            carry = num[i] & carry;
            res[i] = (char) ('0' + bit);  //显示转化为char
        }
        String result = new StringBuffer(new String(res)).reverse().toString();
        return "" + (result.charAt(0) == operand.charAt(0) ? '0' : '1') + result;  //注意有进位不等于溢出，溢出要另外判断
    }
}
