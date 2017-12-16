package mobi.waj.idea.gitwatch.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Utils {
    public static void save(String intervalStr) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream("interval");
        fileOutputStream.write(intervalStr.getBytes());
        fileOutputStream.close();
    }

    public static String readInterval() throws IOException {
        FileInputStream fileInputStream = new FileInputStream("interval");
        byte[] bytes = new byte[1024];
        int len = fileInputStream.read(bytes);
        String ret = new String(bytes,0,len);
        return ret;
    }
}
