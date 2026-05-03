package com.infinitygraff.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da INFINITY GRAFF API.
 *
 * <p>Fase 1 — Fundação Segura e Escalável:
 * autenticação JWT, controle de acesso por perfil,
 * auditoria e gestão de usuários.
 *
 * <p>Requisito de timezone: a JVM deve iniciar com TZ=UTC
 * para garantir consistência de todos os timestamps do sistema.
 * Configurar via variável de ambiente TZ=UTC ou argumento -Duser.timezone=UTC.
 */

@SpringBootApplication
public class InfinityGraffApplication {

    public static void main(String[] args) {
        SpringApplication.run(InfinityGraffApplication.class, args);
    }
}