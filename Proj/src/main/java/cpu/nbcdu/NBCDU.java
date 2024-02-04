package cpu.nbcdu;

import util.DataType;

public class NBCDU {
    //TODO：本题数据点不考虑溢出，若出现溢出应该怎么办呢？

    /**
     * @param src  A 32-bits NBCD String
     * @param dest A 32-bits NBCD String
     * @return dest + src
     */
    DataType add(DataType src, DataType dest) {
        // TODO
        String src_s = src.toString(), dest_s = dest.toString();
        String src_num = src_s.substring(4,32), dest_num = dest_s.substring(4,32);
        String src_sgn = src_s.substring(0,4), dest_sgn = dest_s.substring(0,4);
        String ans = "";
        if(src_sgn.equals(dest_sgn)){//同号
            ans = src_sgn + addNumPart(src_num,dest_num);
        }else{//异号
            if(src_num.compareTo(dest_num) > 0){//判断绝对值大小，决定谁减谁
                ans = src_sgn + subNumPart(dest_num,src_num);
            }else{
                ans = dest_sgn + subNumPart(src_num,dest_num);
            }
        }
        if(ans.substring(4,32).equals("0000000000000000000000000000")){//处理0的符号
            ans = "11000000000000000000000000000000";
        }
        return new DataType(ans);
    }

    /***
     *
     * @param src A 32-bits NBCD String
     * @param dest A 32-bits NBCD String
     * @return dest - src
     */
    DataType sub(DataType src, DataType dest) {//减法：将减数反号变加法。
        // TODO
        String src_s = src.toString(), dest_s = dest.toString();
        src_s = (src_s.substring(0,4).equals("1100") ? "1101" : "1100") + src_s.substring(4,32);
        return add(new DataType(src_s), new DataType(dest_s));
    }

    //对已经处理好符号问题的两个数的数据部分做加法，参数为两个数后4k位数据部分。
    private String addNumPart(String src_num, String dest_num){
        String last_ans = "00000";
        String ans = "";
        String src_seg, dest_seg;
        for(int i = src_num.length()/4-1; i >= 0; i--){
            src_seg = src_num.substring(4*i,4*i+4);
            dest_seg = dest_num.substring(4*i,4*i+4);
            last_ans = addSeg(src_seg,dest_seg,last_ans);
            ans = last_ans.substring(1,5) + ans;
        }
        return ans;
    }

    //对已经处理好符号问题的、被减数绝对值更大的两个数的数据部分做减法，参数为两个数的后28位数据部分。
    private String subNumPart(String src_num, String dest_num){
        String ans = "";
        int len = dest_num.length();
        for(int i = 0; i < len/4; i++){
            if(dest_num.substring(4*i,4*i+4).equals("0000")){
                ans = ans + "0000";
            }else{
                break;
            }
        }
        if(ans.length() != len) {
            int len_l = ans.length(), len_r = len - len_l;
            String src_sub_temp = src_num.substring(len_l, len);
            String dest_sub = dest_num.substring(len_l, len);
            String src_sub = "";
            for (int i = 0; i < len_r / 4; i++) {
                src_sub += negateNBCD_4(src_sub_temp.substring(4*i, 4*i+4));
            }
            String src_sub_last = src_sub.substring(len_r-4, len_r);
            src_sub = src_sub.substring(0, len_r-4) + addFourBit(src_sub_last,"0001","00000").substring(1,5);
            ans += addNumPart(src_sub, dest_sub);
        }
        return ans;
    }

    //对两个4位二进制数做加法，并对大于等于10的结果进行处理，返回5位数（含一位进位）
    //last_ans是上一次的计算结果，用于确定本次的初始carry位。
    private String addSeg(String src_4, String dest_4, String last_ans){
        String ans = addFourBit(src_4, dest_4, last_ans);
        if(ans.compareTo("01010") >= 0){
            String temp = addFourBit(ans.substring(1,5), "0110", "00000");
            ans = "1" + temp.substring(1,5);
        }
        return ans;
    }

    //只做加法，不考虑大于等于10的数的处理。
    private String addFourBit(String src_4, String dest_4, String last_ans){
        int carry = last_ans.charAt(0) - '0';
        String ans = "";
        for(int i = 3; i >= 0; i--){
            int src_1 = src_4.charAt(i) - '0';
            int dest_1 = dest_4.charAt(i) - '0';
            int temp = carry + src_1 + dest_1;
            ans = (temp % 2) + ans;
            carry = temp / 2;
        }
        return carry + ans;
    }

    //对4位NBCD码取反
    private String negateNBCD_4(String s){
        return addFourBit("1010",negateBit(s),"00000").substring(1,5);
    }

    //对二进制序列按位取反
    private String negateBit(String s){
        StringBuilder sb = new StringBuilder(s);
        for(int i = 0; i < sb.length(); i++){
            sb.setCharAt(i, (sb.charAt(i) == '0' ? '1' : '0'));
        }
        return sb.toString();
    }
}
