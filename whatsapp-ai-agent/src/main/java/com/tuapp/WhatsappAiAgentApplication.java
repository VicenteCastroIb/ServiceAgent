package com.tuapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling: necesario para el job de recordatorios automáticos
// (ReminderJob, plan Pro - ver doc secciones 3 y 6).
@EnableScheduling
@SpringBootApplication
public class WhatsappAiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhatsappAiAgentApplication.class, args);
    }

}
