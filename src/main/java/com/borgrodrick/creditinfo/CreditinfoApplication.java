package com.borgrodrick.creditinfo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class CreditinfoApplication {

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext ctx = new SpringApplication("spring-integration.xml").run(args);

        FileCopyDemoCommon.displayDirectories(ctx);

        System.out.println("Hit Enter to terminate");
        System.in.read();
        ctx.close();
    }
}
