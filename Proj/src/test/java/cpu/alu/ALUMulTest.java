package cpu.alu;

import org.junit.Test;
import util.DataType;

import static org.junit.Assert.assertEquals;

public class ALUMulTest {

    private final ALU alu = new ALU();
    private DataType src;
    private DataType dest;
    private DataType result;

    @Test
    public void MulTest1() {
        src = new DataType("00000000000000000000000000001010");
        dest = new DataType("00000000000000000000000000001010");
        result = alu.mul(src, dest);
        assertEquals("00000000000000000000000001100100", result.toString());
    }
    @Test
    public void MulTest2() {
        src = new DataType("11111111111111111111111111111010");
        dest = new DataType("11111111111111111111111111111011");
        result = alu.mul(src, dest);
        assertEquals("00000000000000000000000000011110", result.toString());
    }
    @Test
    public void MulTest3() {
        src = new DataType("00000000000000000000000000001010");
        dest = new DataType("11111111111111111111111111111011");
        result = alu.mul(src, dest);
        assertEquals("11111111111111111111111111001110", result.toString());
    }

}
