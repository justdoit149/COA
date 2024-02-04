package util;

public class Transformer {
    private static final String[] NBCDhelper = {"0000","0001","0010","0011","0100","0101","0110","0111","1000","1001"};

    /**
     * 将整数转换为32位二进制表示。
     * @param numStr：String，待转换整数。
     * @return String，转换后的32位二进制数。
     */
    public static String intToBinary(String numStr) {
        //注：本方法可多用（如需要16位二进制数，可以将低16位截取出来）
        int x = Integer.parseInt(numStr);
        String ans = "";
        if(x < 0) {
            x += (1 << 31);
        }
        for(int i = 31; i >= 1; i--) {
            ans = (x % 2 == 0 ? "0" : "1") + ans;
            x /= 2;
        }
        ans = (Integer.parseInt(numStr) >= 0 ? "0" : "1") + ans;
        return ans;
    }

    /**
     * 将二进制数转为整数。
     * @param binStr：String，待转换二进制数，不超过32位。
     * @return String，转换后的整数。
     */
    public static String binaryToInt(String binStr) {
        return String.valueOf(valueOf(binStr, 2));
    }

    /**
     * 将整数转为NBCD码。
     * @param decimalStr：String，待转换整数。
     * @return String，转换后的NBCD码。
     */
    public static String decimalToNBCD(String decimalStr) {
        int n = Integer.parseInt(decimalStr);
        String ans = "";
        for(int i = 7; i >= 1; i--) {
            ans = NBCDhelper[n%10] + ans;
            n /= 10;
        }
        ans = (Integer.parseInt(decimalStr) >= 0 ? "1100" : "1101") + ans;
        return ans;
    }

    /**
     * 将NBCD码转为整数。
     * @param NBCDStr：String，待转换NBCD码。
     * @return String，转换后的整数。
     */
    public static String NBCDToDecimal(String NBCDStr) {
        int ans = 0;
        String temp;
        for(int i = 4; i < 32; i += 4){
            ans *= 10;
            temp = NBCDStr.substring(i,i+4);
            for(int j = 0; j < 10; j++){
                if(temp.equals(NBCDhelper[j])){
                    ans = ans + j;
                }
            }
        }
        if(NBCDStr.substring(0,4).equals("1101")) ans = -ans;//不能用==，因为函数返回的字符串不在常量池
        return String.valueOf(ans);
    }

    /**
     * 将浮点数转为32位二进制码。
     * @param floatStr：String，待转换浮点数。
     * @return String，转换后的32位二进制码。
     */
    public static String floatToBinary(String floatStr) {
        float x = Float.parseFloat(floatStr);
        String ans = "";
        if(x > Float.MAX_VALUE) {//Float.MAX_VALUE为0x7f7fffff
            return "+Inf";
        }else if(x < -Float.MAX_VALUE) {
            return "-Inf";
        }else{
            //“-0”，符号位是1
            ans += (floatStr.charAt(0) == '-' ? "1" : "0");
            if(x >= Math.pow(2,-126) || x <= -Math.pow(2,-126)){
                int pow0 = 1;
                //规格化的,x/2不丢精度，但从规格化到非规格化，这样就会丢精度了。因此只能除到-125不能到-126
                while(x >= Math.pow(2,-125) || x <= -Math.pow(2,-125)){
                    x /= 2;
                    pow0++;
                }
                ans += intToBinary(String.valueOf(pow0)).substring(24,32);
                x = (float) (Math.abs(x) * Math.pow(2,126) - 1);
                for(int i = 0; i < 23; i++) {
                    x *= 2;
                    if (x >= 1) {
                        ans += "1";
                        x -= 1;
                    } else {
                        ans += "0";
                    }
                }
            }else{
                ans += "00000000";
                x = (float) (Math.abs(x) * Math.pow(2,126));
                for(int i = 0; i < 23; i++) {
                    x *= 2;
                    if (x >= 1) {
                        ans += "1";
                        x -= 1;
                    } else {
                        ans += "0";
                    }
                }
            }
        }
        return ans;
    }

    /**
     * 将32位二进制码转为浮点数。
     * @param binStr：String，待转换二进制码。
     * @return String，转换后的浮点数。
     */
    public static String binaryToFloat(String binStr) {
        if(binStr.substring(1,9).equals("11111111")) {
            if(binStr.charAt(0) == '0') return "+Inf";
            else return "-Inf";
        }
        float ans = 0;
        float last = Integer.parseInt(binaryToInt(binStr.substring(32-23,32))) * (float)1.0 / (1<<23);
        if(binStr.substring(1,9).equals("00000000")){
            ans = (last * (float)Math.pow(2,-126));
        }else{
            last += 1;
            int pow0 = Integer.parseInt(binaryToInt(binStr.substring(1,9))) - 127;
            ans = (last * (float)Math.pow(2,pow0));
        }
        if(binStr.charAt(0) == '1'){
            ans = - ans;
        }
        return String.valueOf(ans);
    }

    private static int valueOf(String num, int radix) {
        int ans = 0;
        for (int i = 0; i < num.length(); i++) {
            int temp = 0;
            if (num.charAt(i) <= '9' && num.charAt(i) >= '0') temp = num.charAt(i) - '0';
            else temp = num.charAt(i) - 'a' + 10;
            ans = ans * radix + temp;
        }
        return ans;
    }

    /**
     * add one to the operand
     *
     * @param operand the operand
     * @return result after adding, the first position means overflow (not equal to the carray to the next) and the remains means the result
     */
    private static String oneAdder(String operand) {
        int len = operand.length();
        StringBuffer temp = new StringBuffer(operand);
        temp = temp.reverse();
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

    /**
     * convert the string's 0 and 1.
     * e.g 00000 to 11111
     *
     * @param operand string to convert (by default, it is 32 bits long)
     * @return string after converting
     */
    private static String negation(String operand) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < operand.length(); i++) {
            result = operand.charAt(i) == '1' ? result.append("0") : result.append("1");
        }
        return result.toString();
    }
}
