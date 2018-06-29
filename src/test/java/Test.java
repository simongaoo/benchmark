import java.nio.ByteBuffer;
import java.util.Arrays;

public class Test {
    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(1850482245);
        System.out.println(Arrays.toString(buffer.array()));
    }
}
