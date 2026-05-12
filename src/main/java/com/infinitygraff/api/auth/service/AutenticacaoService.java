package com.infinitygraff.api.auth.service;

import com.infinitygraff.api.auditoria.model.LogAuditoria;
import com.infinitygraff.api.auditoria.service.AuditoriaContext;
import com.infinitygraff.api.auditoria.service.AuditoriaHelper;
import com.infinitygraff.api.auditoria.service.AuditoriaService;
import com.infinitygraff.api.auth.dto.CompletarPerfilRequest;
import com.infinitygraff.api.common.exception.AcessoNegadoException;
import com.infinitygraff.api.common.exception.NegocioException;
import com.infinitygraff.api.security.SecurityContextHelper;
import com.infinitygraff.api.security.SupabaseAuthenticatedPrincipal;
import com.infinitygraff.api.usuario.dto.UsuarioResponse;
import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.model.Usuario;
import com.infinitygraff.api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

/**
 * Serviço responsável pela ponte entre Supabase Auth e o backend interno.
 *
 * <p>O Supabase Auth cuida de cadastro, login, senha, sessão e refresh token.
 *
 * <p>O backend Spring cuida de criar o perfil interno na tabela {@code usuarios},
 * controlar role interna, controlar status ativo/inativo e aplicar regras de permissão.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    private static final String ACAO_USUARIO_CADASTRADO = "USUARIO_CADASTRADO";
    private static final String ENTIDADE_USUARIOS = "usuarios";

    private final UsuarioRepository usuarioRepository;
    private final SecurityContextHelper securityContextHelper;
    private final AuditoriaService auditoriaService;
    private final AuditoriaHelper auditoriaHelper;

    /**
     * Completa o perfil interno do usuário autenticado pelo Supabase Auth.
     *
     * <p>O token Supabase já foi validado pelo JwtAuthenticationFilter.
     * Este método não valida token manualmente.
     *
     * <p>Este fluxo é idempotente quando o perfil já existe com a mesma role:
     * nesse caso, o perfil existente é retornado.
     */
    @Transactional
    public ResultadoCompletarPerfil completarPerfil(
            CompletarPerfilRequest request,
            AuditoriaContext auditoriaContext
    ) {
        Authentication authentication = securityContextHelper.obterAuthentication();

        Object principal = authentication.getPrincipal();

        if (principal instanceof SupabaseAuthenticatedPrincipal supabasePrincipal) {
            return criarPerfilInterno(supabasePrincipal, request, auditoriaContext);
        }

        if (principal instanceof Usuario usuario) {
            return tratarPerfilJaExistente(usuario, request);
        }

        throw new BadCredentialsException("Token ausente ou inválido");
    }

    /**
     * Retorna o perfil interno do usuário autenticado.
     *
     * <p>Esta rota exige que o usuário já exista na tabela {@code usuarios}.
     */
    @Transactional(readOnly = true)
    public UsuarioResponse meuPerfil() {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        return UsuarioResponse.de(usuario);
    }

    private ResultadoCompletarPerfil criarPerfilInterno(
            SupabaseAuthenticatedPrincipal principal,
            CompletarPerfilRequest request,
            AuditoriaContext auditoriaContext
    ) {
        validarRolePermitidaParaCadastroPublico(request.role());

        String emailNormalizado = normalizarEmail(principal.email());

        if (emailNormalizado.isBlank()) {
            throw new BadCredentialsException("Token Supabase não possui e-mail válido");
        }

        if (usuarioRepository.existsById(principal.id())) {
            throw new NegocioException(
                    "Perfil interno já existe para este usuário",
                    HttpStatus.CONFLICT
            );
        }

        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new NegocioException(
                    "E-mail já vinculado a outro perfil",
                    HttpStatus.CONFLICT
            );
        }

        Usuario usuario = Usuario.builder()
                .id(principal.id())
                .nome(request.nome().trim())
                .email(emailNormalizado)
                .role(request.role())
                .ativo(true)
                .aceitoTermosEm(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        Usuario salvo = usuarioRepository.save(usuario);
        UsuarioResponse response = UsuarioResponse.de(salvo);

        registrarUsuarioCadastradoAposCommit(salvo, response, auditoriaContext);

        return ResultadoCompletarPerfil.criado(response);
    }

    private ResultadoCompletarPerfil tratarPerfilJaExistente(
            Usuario usuario,
            CompletarPerfilRequest request
    ) {
        if (!usuario.podeAcessarSistema()) {
            throw new AcessoNegadoException("Usuário inativo ou indisponível");
        }

        if (usuario.getRole() != request.role()) {
            throw new NegocioException(
                    "Perfil interno já existe com role diferente",
                    HttpStatus.CONFLICT
            );
        }

        log.warn(
                "Rota completar-perfil chamada para usuário que já possui perfil interno. usuarioId={}, role={}",
                usuario.getId(),
                usuario.getRole()
        );

        return ResultadoCompletarPerfil.existente(UsuarioResponse.de(usuario));
    }

    private void validarRolePermitidaParaCadastroPublico(Role role) {
        if (role != Role.CLIENTE && role != Role.PRESTADOR) {
            throw new AcessoNegadoException(
                    "Cadastro público permitido apenas para CLIENTE ou PRESTADOR"
            );
        }
    }

    private void registrarUsuarioCadastradoAposCommit(
            Usuario usuario,
            UsuarioResponse response,
            AuditoriaContext auditoriaContext
    ) {
        AuditoriaContext contexto = auditoriaContext != null
                ? auditoriaContext
                : AuditoriaContext.vazio();

        LogAuditoria entrada = LogAuditoria.builder()
                .usuarioId(usuario.getId())
                .acao(ACAO_USUARIO_CADASTRADO)
                .entidade(ENTIDADE_USUARIOS)
                .entidadeId(usuario.getId())
                .dadosAntes(null)
                .dadosDepois(auditoriaHelper.serializarSeguro(response))
                .ip(contexto.ip())
                .userAgent(contexto.userAgent())
                .build();

        auditoriaHelper.executarAposCommit(() -> auditoriaService.registrar(entrada));
    }

    private String normalizarEmail(String email) {
        return email == null
                ? ""
                : email.trim().toLowerCase(Locale.ROOT);
    }
}