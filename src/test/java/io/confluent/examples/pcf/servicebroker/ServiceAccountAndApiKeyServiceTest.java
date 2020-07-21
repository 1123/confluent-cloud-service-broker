package io.confluent.examples.pcf.servicebroker;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

class ServiceAccountAndApiKeyServiceTest {

    @Test
    @SneakyThrows
    @Disabled
    public void testCreate() {
        String s;
        Process p = Runtime.getRuntime().exec("src/main/resources/ccloud kafka cluster list");
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        while ((s = br.readLine()) != null)
            System.out.println(s);
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        while ((s = errorReader.readLine()) != null)
            System.out.println(s);
        p.waitFor();
        System.out.println ("exit: " + p.exitValue());
        p.destroy();
    }


}