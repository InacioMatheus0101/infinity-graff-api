package com.infinitygraff.api.usuario.service;

import com.infinitygraff.api.auditoria.model.LogAuditoria;
import com.infinitygraff.api.auditoria.service.AuditoriaContext;
import com.infinitygraff.api.auditoria.service.AuditoriaHelper;
import com.infinitygraff.api.auditoria.service.AuditoriaService;
import com.infinitygraff.api.common.exception.AcessoNegadoException;
import com.infinitygraff.api.common.exception.NegocioException;
import com.infinitygraff.api.common.exception.RecursoNaoEncontradoException;
import com.infinitygraff.api.common.response.PageResponse;
import com.infinitygraff.api.security.SecurityContextHelper;
import com.infinitygraff.api.usuario.dto.AtualizarStatusRequest;
import com.infinitygraff.api.usuario.dto.UsuarioResponse;
import com.infinitygraff.api.usuario.dto.UsuarioResumoResponse;
import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.model.Usuario;
import com.infinitygraff.api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Serviço responsável pelas regras de negócio de usuários internos da plataforma.
 *
 * <p>O Supabase Auth cuida da identidade, login, senha e sessão.
 * Este service cuida apenas do perfil interno salvo na tabela {@code usuarios}.
 *
 * <p>Regras principais:
 * <ul>
 *   <li>ADMIN gerencia usuários de forma ampla, exceto ações destrutivas contra si mesmo;</li>
 *   <li>GERENTE gerencia apenas CLIENTE e PRESTADOR;</li>
 *   <li>CLIENTE e PRESTADOR só acessam o próprio perfil;</li>
 *   <li>soft delete nunca remove fisicamente o registro do banco.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final String ACAO_USUARIO_ATIVADO = "USUARIO_ATIVADO";
    private static final String ACAO_USUARIO_DESATIVADO = "USUARIO_DESATIVADO";
    private static final String ACAO_USUARIO_DELETADO = "USUARIO_DELETADO";
    private static final String ENTIDADE_USUARIOS = "usuarios";

    private static final List<Role> ROLES_GERENCIAVEIS_PELO_GERENTE = List.of(
            Role.CLIENTE,
            Role.PRESTADOR
    );

    private final UsuarioRepository usuarioRepository;
    private final SecurityContextHelper securityContextHelper;
    private final AuditoriaService auditoriaService;
    private final AuditoriaHelper auditoriaHelper;

    /**
     * Lista usuários com paginação e filtros opcionais.
     *
     * <p>ADMIN visualiza todos os usuários não deletados.
     * GERENTE visualiza apenas CLIENTE e PRESTADOR.
     */
    @Transactional(readOnly = true)
    public PageResponse<UsuarioResumoResponse> listar(
            Role role,
            Boolean ativo,
            String nome,
            Pageable pageable
    ) {
        Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();
        String nomeNormalizado = normalizarFiltroTexto(nome);

        Page<Usuario> pagina;

        if (autenticado.getRole() == Role.ADMIN) {
            pagina = usuarioRepository.listarComFiltro(
                    role,
                    ativo,
                    nomeNormalizado,
                    pageable
            );

            return PageResponse.de(pagina, UsuarioResumoResponse::de);
        }

        if (autenticado.getRole() == Role.GERENTE) {
            if (role != null && !roleGerenciavelPeloGerente(role)) {
                throw new AcessoNegadoException("Gerente não pode visualizar usuários com este perfil");
            }

            pagina = usuarioRepository.listarComFiltroPorRoles(
                    ROLES_GERENCIAVEIS_PELO_GERENTE,
                    role,
                    ativo,
                    nomeNormalizado,
                    pageable
            );

            return PageResponse.de(pagina, UsuarioResumoResponse::de);
        }

        /*
         * Fallback defensivo.
         * O SecurityConfig já deve bloquear CLIENTE e PRESTADOR nesta rota,
         * mas mantemos a validação no service para proteger chamadas internas futuras.
         */
        throw new AcessoNegadoException("Você não tem permissão para listar usuários");
    }

    /**
     * Busca detalhes de um usuário por ID, respeitando as permissões do usuário autenticado.
     */
    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(UUID id) {
        Objects.requireNonNull(id, "id não pode ser nulo");

        Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();
        Usuario alvo = buscarUsuarioOuFalhar(id);

        validarPermissaoVisualizacao(autenticado, alvo);

        return UsuarioResponse.de(alvo);
    }

    /**
     * Ativa ou desativa um usuário.
     *
     * <p>ADMIN pode alterar qualquer usuário, exceto desativar a si mesmo.
     * GERENTE pode alterar apenas CLIENTE e PRESTADOR.
     */
    @Transactional
    public UsuarioResponse atualizarStatus(
            UUID id,
            AtualizarStatusRequest request,
            AuditoriaContext auditoriaContext
    ) {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Objects.requireNonNull(request, "request não pode ser nulo");

        Boolean ativoSolicitado = Objects.requireNonNull(
                request.ativo(),
                "ativo não pode ser nulo"
        );

        Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();
        Usuario alvo = buscarUsuarioOuFalhar(id);

        validarPermissaoAlterarStatus(autenticado, alvo, ativoSolicitado);

        if (ativoSolicitado.equals(alvo.isAtivo())) {
            return UsuarioResponse.de(alvo);
        }

        UsuarioResponse antes = UsuarioResponse.de(alvo);

        if (ativoSolicitado) {
            alvo.ativar();
        } else {
            alvo.desativar();
        }

        Usuario salvo = usuarioRepository.save(alvo);
        UsuarioResponse depois = UsuarioResponse.de(salvo);

        registrarAlteracaoStatusAposCommit(
                autenticado,
                salvo,
                antes,
                depois,
                ativoSolicitado,
                auditoriaContext
        );

        return depois;
    }

    /**
     * Aplica soft delete em um usuário.
     *
     * <p>Não remove fisicamente o registro do banco.
     * O fluxo oficial é {@code usuario.deletar()} seguido de {@code save()}.
     */
    @Transactional
    public void aplicarSoftDelete(
            UUID id,
            AuditoriaContext auditoriaContext
    ) {
        Objects.requireNonNull(id, "id não pode ser nulo");

        Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();
        Usuario alvo = buscarUsuarioOuFalhar(id);

        validarPermissaoSoftDelete(autenticado, alvo);

        UsuarioResponse antes = UsuarioResponse.de(alvo);

        alvo.deletar();
        Usuario salvo = usuarioRepository.save(alvo);

        /*
         * O método retorna 204 No Content no controller.
         * Este DTO é criado apenas para registrar dadosDepois na auditoria.
         * UsuarioResponse não expõe deletadoEm por decisão de segurança/encapsulamento.
         */
        UsuarioResponse depois = UsuarioResponse.de(salvo);

        registrarSoftDeleteAposCommit(
                autenticado,
                salvo,
                antes,
                depois,
                auditoriaContext
        );
    }

    private Usuario buscarUsuarioOuFalhar(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    private void validarPermissaoVisualizacao(Usuario autenticado, Usuario alvo) {
        if (mesmoUsuario(autenticado, alvo)) {
            return;
        }

        if (autenticado.getRole() == Role.ADMIN) {
            return;
        }

        if (autenticado.getRole() == Role.GERENTE && roleGerenciavelPeloGerente(alvo.getRole())) {
            return;
        }

        throw new AcessoNegadoException("Você não tem permissão para visualizar este usuário");
    }

    private void validarPermissaoAlterarStatus(
            Usuario autenticado,
            Usuario alvo,
            boolean novoStatus
    ) {
        if (autenticado.getRole() == Role.ADMIN) {
            if (mesmoUsuario(autenticado, alvo) && !novoStatus) {
                throw new NegocioException(
                        "Administrador não pode desativar a si mesmo",
                        HttpStatus.BAD_REQUEST
                );
            }

            return;
        }

        if (autenticado.getRole() == Role.GERENTE && roleGerenciavelPeloGerente(alvo.getRole())) {
            return;
        }

        throw new AcessoNegadoException("Você não tem permissão para alterar o status deste usuário");
    }

    private void validarPermissaoSoftDelete(Usuario autenticado, Usuario alvo) {
        if (autenticado.getRole() != Role.ADMIN) {
            throw new AcessoNegadoException("Apenas ADMIN pode aplicar soft delete em usuários");
        }

        if (mesmoUsuario(autenticado, alvo)) {
            throw new NegocioException(
                    "Administrador não pode aplicar soft delete em si mesmo",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void registrarAlteracaoStatusAposCommit(
            Usuario autor,
            Usuario alvo,
            UsuarioResponse antes,
            UsuarioResponse depois,
            boolean novoStatus,
            AuditoriaContext auditoriaContext
    ) {
        String acao = novoStatus
                ? ACAO_USUARIO_ATIVADO
                : ACAO_USUARIO_DESATIVADO;

        registrarAuditoriaUsuarioAposCommit(
                autor,
                alvo,
                acao,
                antes,
                depois,
                auditoriaContext
        );
    }

    private void registrarSoftDeleteAposCommit(
            Usuario autor,
            Usuario alvo,
            UsuarioResponse antes,
            UsuarioResponse depois,
            AuditoriaContext auditoriaContext
    ) {
        registrarAuditoriaUsuarioAposCommit(
                autor,
                alvo,
                ACAO_USUARIO_DELETADO,
                antes,
                depois,
                auditoriaContext
        );
    }

    private void registrarAuditoriaUsuarioAposCommit(
            Usuario autor,
            Usuario alvo,
            String acao,
            UsuarioResponse antes,
            UsuarioResponse depois,
            AuditoriaContext auditoriaContext
    ) {
        AuditoriaContext contexto = auditoriaContext != null
                ? auditoriaContext
                : AuditoriaContext.vazio();

        LogAuditoria entrada = LogAuditoria.builder()
                .usuarioId(autor.getId())
                .acao(acao)
                .entidade(ENTIDADE_USUARIOS)
                .entidadeId(alvo.getId())
                .dadosAntes(auditoriaHelper.serializarSeguro(antes))
                .dadosDepois(auditoriaHelper.serializarSeguro(depois))
                .ip(contexto.ip())
                .userAgent(contexto.userAgent())
                .build();

        auditoriaHelper.executarAposCommit(() -> auditoriaService.registrar(entrada));
    }

    private boolean mesmoUsuario(Usuario usuarioA, Usuario usuarioB) {
        return Objects.equals(usuarioA.getId(), usuarioB.getId());
    }

    private boolean roleGerenciavelPeloGerente(Role role) {
        return ROLES_GERENCIAVEIS_PELO_GERENTE.contains(role);
    }

    private String normalizarFiltroTexto(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }
}