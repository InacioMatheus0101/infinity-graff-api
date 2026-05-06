package com.infinitygraff.api.auth.service;

import com.infinitygraff.api.auth.dto.CompletarPerfilRequest;
import com.infinitygraff.api.auth.dto.MeuPerfilResponse;
import com.infinitygraff.api.common.exception.AcessoNegadoException;
import com.infinitygraff.api.common.exception.NegocioException;
import com.infinitygraff.api.common.exception.RecursoNaoEncontradoException;
import com.infinitygraff.api.security.SupabaseAuthenticatedPrincipal;
import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.model.Usuario;
import com.infinitygraff.api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

/**
 * Serviço responsável pela ponte entre Supabase Auth e o backend interno.
 *
 * O Supabase Auth cuida de:
 * - cadastro;
 * - login;
 * - senha;
 * - sessão;
 * - refresh token.
 *
 * O backend Spring cuida de:
 * - criar o perfil interno na tabela usuarios;
 * - controlar role interna;
 * - controlar status ativo/inativo;
 * - aplicar regras de permissão do marketplace.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Completa o perfil interno do usuário autenticado pelo Supabase Auth.
     *
     * O token Supabase já foi validado pelo JwtAuthenticationFilter.
     * Este método não valida token manualmente.
     *
     * Cenários:
     * - principal SupabaseAuthenticatedPrincipal: cria o perfil interno;
     * - principal Usuario: perfil já existe, retorna o perfil existente ou conflito se role divergir.
     */
    @Transactional
    public MeuPerfilResponse completarPerfil(CompletarPerfilRequest request) {
        Authentication authentication = obterAuthentication();

        Object principal = authentication.getPrincipal();

        if (principal instanceof SupabaseAuthenticatedPrincipal supabasePrincipal) {
            return criarPerfilInterno(supabasePrincipal, request);
        }

        if (principal instanceof Usuario usuario) {
            return tratarPerfilJaExistente(usuario, request);
        }

        throw new NegocioException("Token ausente ou inválido", HttpStatus.UNAUTHORIZED);
    }

    /**
     * Retorna o perfil interno do usuário autenticado.
     *
     * Esta rota exige que o usuário já exista na tabela usuarios.
     */
    @Transactional(readOnly = true)
    public MeuPerfilResponse meuPerfil() {
        Authentication authentication = obterAuthentication();

        if (!(authentication.getPrincipal() instanceof Usuario usuario)) {
            throw new RecursoNaoEncontradoException("Perfil interno ainda não foi criado");
        }

        if (!usuario.podeAcessarSistema()) {
            throw new AcessoNegadoException("Usuário inativo ou indisponível");
        }

        return MeuPerfilResponse.de(usuario);
    }

    private MeuPerfilResponse criarPerfilInterno(
            SupabaseAuthenticatedPrincipal principal,
            CompletarPerfilRequest request
    ) {
        validarRolePermitidaParaCadastroPublico(request.role());

        String emailNormalizado = normalizarEmail(principal.email());

        if (emailNormalizado.isBlank()) {
            throw new NegocioException("Token Supabase não possui e-mail válido", HttpStatus.UNAUTHORIZED);
        }

        usuarioRepository.findById(principal.id()).ifPresent(usuario -> {
            throw new NegocioException("Perfil interno já existe para este usuário", HttpStatus.CONFLICT);
        });

        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new NegocioException("E-mail já vinculado a outro perfil", HttpStatus.CONFLICT);
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

        // TODO: registrar auditoria USUARIO_CADASTRADO após implementação do AuditoriaService.

        return MeuPerfilResponse.de(salvo);
    }

    private MeuPerfilResponse tratarPerfilJaExistente(
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

        return MeuPerfilResponse.de(usuario);
    }

    private void validarRolePermitidaParaCadastroPublico(Role role) {
        if (role != Role.CLIENTE && role != Role.PRESTADOR) {
            throw new AcessoNegadoException(
                    "Cadastro público permitido apenas para CLIENTE ou PRESTADOR"
            );
        }
    }

    private Authentication obterAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            throw new NegocioException("Token ausente ou inválido", HttpStatus.UNAUTHORIZED);
        }

        return authentication;
    }

    private String normalizarEmail(String email) {
        return email == null
                ? ""
                : email.trim().toLowerCase(Locale.ROOT);
    }
}