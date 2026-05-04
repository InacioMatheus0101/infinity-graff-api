package com.infinitygraff.api.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * Utilitário para extração do IP do cliente a partir da requisição HTTP.
 *
 * <p>Respeita headers de proxy na seguinte ordem de prioridade:
 * <ol>
 *   <li>{@code X-Forwarded-For} — usado por proxies e load balancers</li>
 *   <li>{@code X-Real-IP} — usado por Nginx e alguns proxies</li>
 *   <li>{@code request.getRemoteAddr()} — fallback direto</li>
 * </ol>
 *
 * <p><b>Atenção de segurança:</b> headers como {@code X-Forwarded-For}
 * e {@code X-Real-IP} só são confiáveis quando a aplicação está atrás de
 * um proxy confiável que sobrescreve esses headers. Se a API estiver exposta
 * diretamente à internet, esses headers podem ser falsificados pelo cliente.
 *
 * <p>Quando {@code X-Forwarded-For} contém múltiplos IPs, o primeiro IP da lista
 * é retornado como a origem informada pelo proxy.
 */

public final class IpUtil {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";
    private static final String VALOR_DESCONHECIDO = "unknown";

    private IpUtil() {
        // classe utilitária — não instanciar
    }

    /**
     * Extrai o IP real do cliente da requisição HTTP.
     *
     * @param request requisição HTTP atual
     * @return IP do cliente em formato String, ou {@code "unknown"} se não identificado
     */
    public static String extrairIp(HttpServletRequest request) {
        String ip = request.getHeader(HEADER_X_FORWARDED_FOR);

        if (ipValido(ip)) {
            // X-Forwarded-For pode conter cadeia de IPs — pega o primeiro (origem real)
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader(HEADER_X_REAL_IP);

        if (ipValido(ip)) {
            return ip.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr : VALOR_DESCONHECIDO;
    }

    /**
     * Verifica se o valor do header é um IP utilizável.
     *
     * @param ip valor bruto do header
     * @return {@code true} se não nulo, não vazio e diferente de "unknown"
     */
    private static boolean ipValido(String ip) {
        return StringUtils.hasText(ip) && !VALOR_DESCONHECIDO.equalsIgnoreCase(ip);
    }
}