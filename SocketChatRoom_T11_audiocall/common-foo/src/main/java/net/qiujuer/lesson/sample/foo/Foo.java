package net.qiujuer.lesson.sample.foo;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class Foo {
    private static final String CACHE_DIR = "cache";
    public static final String COMMAND_EXIT = "00bye00";

    // 加入群聊
    public static final String COMMAND_GROUP_JOIN = "--m g join";
    // 退出群聊
    public static final String COMMAND_GROUP_LEAVE = "--m g leave";
    // 默认群名
    public static final String DEFAULT_GROUP_NAME = "Group";

    public static File getCacheDir(String dir) {

        String path = System.getProperty("user.dir") + (File.separator + CACHE_DIR + File.separator + dir);
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new RuntimeException("Create path error:" + path);
            }
        }
        return file;
    }

    public static File createRandomTemp(File parent) {
        String string = UUID.randomUUID().toString() + ".tmp";
        File file = new File(parent, string);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
