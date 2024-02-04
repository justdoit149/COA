package cpu.nbcdu;

import org.junit.Test;
import util.DataType;

import static org.junit.Assert.assertEquals;

public class NBCDUAddTest {

    private final NBCDU nbcdu = new NBCDU();
    private DataType src;
    private DataType dest;
    private DataType result;

    @Test
    public void AddTest1() {
        src = new DataType("11000000000000000000000010011000");
        dest = new DataType("11000000000000000000000001111001");
        result = nbcdu.add(src, dest);
        assertEquals("11000000000000000000000101110111", result.toString());
    }
    @Test
    public void myAddTest() {
        src = new DataType ("11000000000000000000000000000000");
        dest = new DataType("11010000000000000000000000000000");
        result = nbcdu.add(src, dest);
        assertEquals("11000000000000000000000000000000", result.toString());
    }
}
