package io.jenkins.plugins.aliyunoss.utils;

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
    }
}
