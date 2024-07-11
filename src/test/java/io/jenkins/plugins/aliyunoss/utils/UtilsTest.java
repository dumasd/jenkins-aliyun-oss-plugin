package io.jenkins.plugins.aliyunoss.utils;

import java.io.File;
import java.util.Objects;
import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;

/**
 * @author Bruce.Wu
 * @date 2024-06-18
 */
@Log
class UtilsTest {

    @Test
    void testSplicePath() {
        String path = Utils.splicePath(null, "test", "1/", null, "cc/", "", "ff/");
        log.info(path);

        log.info("" + Utils.isFile("/opt/"));
        log.info("" + Utils.isFile("/opt/ccc.tar.gz"));

        File p = new File("opt/fatfish-sit/fatfish-online-store-sit/job/18/");

        File f = new File(p, "build.tar.gz");
        log.info("" + f.getPath());
        File parent = f.getParentFile();
        if (Objects.nonNull(parent) && !parent.exists() && !parent.mkdirs()) {
            throw new RuntimeException("Make dir error");
        }
        log.info("" + parent.isDirectory());
    }
}
