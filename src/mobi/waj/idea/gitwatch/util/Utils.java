package mobi.waj.idea.gitwatch.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Utils {

    private static String storeIntervalFileName = "D:\\git_watch\\" + "interval.txt";
    private static final String storePathFileName = "D:\\git_watch\\" + "path.txt";

    public static void saveInterval(String intervalStr) throws IOException {
        save(intervalStr, storeIntervalFileName);
    }

    public static String readInterval() throws IOException {
        return read(storeIntervalFileName);
    }

    public static void savePath(String path) throws IOException {
        save(path, storePathFileName);
    }

    public static String readPath() throws IOException{
        return read(storePathFileName);
    }

    private static void save(String intervalStr, String fileName) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(fileName);
        fileOutputStream.write(intervalStr.getBytes());
        fileOutputStream.close();
    }

    private static String read(String fileName) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(fileName);
        byte[] bytes = new byte[1024];
        int len = fileInputStream.read(bytes);
        String ret = new String(bytes,0,len);
        return ret;
    }
}
