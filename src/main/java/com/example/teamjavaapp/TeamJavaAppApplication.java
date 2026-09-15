package com.example.teamjavaapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class TeamJavaAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeamJavaAppApplication.class, args);
    }

    @GetMapping("/")
    public String home() {
        return "Hello from team-java-app version 1.2!";
    }
}
