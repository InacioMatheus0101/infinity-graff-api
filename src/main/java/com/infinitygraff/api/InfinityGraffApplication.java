package com.infinitygraff.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.TimeZone;

/**
 * Ponto de entrada da INFINITY GRAFF API.
 *
 * <p>Fase 1 — Fundação Segura e Escalável:
 * autenticação JWT, controle de acesso por perfil,
 * auditoria e gestão de usuários.
 *
 * <p>Requisito de timezone: a JVM deve iniciar em UTC
 * para garantir consistência de todos os timestamps do sistema.
 */

@SpringBootApplication
@ConfigurationPropertiesScan
public class InfinityGraffApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(InfinityGraffApplication.class, args);
    }
}