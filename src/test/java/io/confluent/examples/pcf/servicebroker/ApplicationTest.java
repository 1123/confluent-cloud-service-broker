package io.confluent.examples.pcf.servicebroker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ApplicationTest {

    @Test
    public void contextLoads() throws InterruptedException {
        Thread.sleep(10000);
    }

}