package com.infinitygraff.api.auditoria;

/**
 * Constantes compartilhadas do módulo de auditoria.
 *
 * <p>Os limites de tamanho devem permanecer alinhados com a migration:
 * {@code V2__create_audit_logs_table.sql}.
 */
public final class AuditoriaConstantes {

    /**
     * Deve corresponder ao VARCHAR(100) da coluna logs_auditoria.acao.
     */
    public static final int TAMANHO_MAXIMO_ACAO = 100;

    /**
     * Deve corresponder ao VARCHAR(50) da coluna logs_auditoria.entidade.
     */
    public static final int TAMANHO_MAXIMO_ENTIDADE = 50;

    /**
     * Deve corresponder ao VARCHAR(100) da coluna logs_auditoria.ip.
     *
     * <p>IPv6 completo tem até 39 caracteres. O limite 100 permite IP com porta
     * ou formatos comuns vindos de proxy.
     */
    public static final int TAMANHO_MAXIMO_IP = 100;

    private AuditoriaConstantes() {
        throw new UnsupportedOperationException("Classe utilitária não deve ser instanciada");
    }
}