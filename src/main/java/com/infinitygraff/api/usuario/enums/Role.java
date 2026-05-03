package com.infinitygraff.api.usuario.enums;

/**
 * Perfis de acesso da plataforma INFINITY GRAFF.
 *
 * <p>Hierarquia de permissões (do maior para o menor):
 * <ol>
 *   <li>{@link #ADMIN} — acesso total, incluindo gestão de usuários e logs de auditoria</li>
 *   <li>{@link #GERENTE} — pode listar e alterar status de usuários, mas não deletar</li>
 *   <li>{@link #PRESTADOR} — acessa apenas o próprio perfil e funcionalidades de serviço</li>
 *   <li>{@link #CLIENTE} — acessa apenas o próprio perfil e funcionalidades de pedido</li>
 * </ol>
 *
 * <p>O valor do enum é persistido como String no banco de dados
 * (coluna {@code role} da tabela {@code usuarios}).
 * A constraint {@code ck_usuarios_role} no banco garante integridade referencial.
 */
public enum Role {

    /** Administrador — acesso irrestrito à plataforma. */
    ADMIN,

    /** Gerente — gestão de usuários sem permissão de exclusão. */
    GERENTE,

    /** Prestador de serviço — acesso ao próprio perfil e aos serviços. */
    PRESTADOR,

    /** Cliente — acessa o próprio perfil e realiza pedidos. */
    CLIENTE
}