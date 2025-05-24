package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaRequestDto;
import br.softsistem.Gerenciamento_de_estoque.exception.OrganizacaoNaoEncontradaException;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.*;
import br.softsistem.Gerenciamento_de_estoque.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EntregaService {

    private final EntregaRepository entregaRepository;
    private final ConsumidorRepository consumidorRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;

    // Constructor Injection
    public EntregaService(EntregaRepository entregaRepository, ConsumidorRepository consumidorRepository, ProdutoRepository produtoRepository, UsuarioRepository usuarioRepository) {
        this.entregaRepository = entregaRepository;
        this.consumidorRepository = consumidorRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // Criar uma nova entrega, associada ao orgId
    public Entrega criarEntrega(EntregaRequestDto entregaRequest) {
        // O orgId é passado através do contexto de segurança (ex: usuário logado ou algum contexto)
        Long orgId = SecurityUtils.getCurrentOrgId();  // Ou utilize qualquer outro método para obter o orgId

        // Buscar as entidades relacionadas
        Consumidor consumidor = buscarConsumidorPorId(entregaRequest.getConsumidorId());
        Produto produto = buscarProdutoPorId(entregaRequest.getProdutoId());
        Usuario entregador = buscarEntregadorPorId(entregaRequest.getEntregadorId());

        // Criar a entrega
        Entrega entrega = new Entrega();
        entrega.setConsumidor(consumidor);
        entrega.setProduto(produto);
        entrega.setEntregador(entregador);
        entrega.setQuantidade(entregaRequest.getQuantidade());
        entrega.setHorarioEntrega(entregaRequest.getHorarioEntrega() != null ? entregaRequest.getHorarioEntrega() : LocalDateTime.now());

        return entregaRepository.save(entrega);  // Salva a entrega no banco de dados
    }

    // Editar uma entrega existente
    public Entrega editarEntrega(Long id, EntregaRequestDto entregaRequest) {
        // O orgId é passado através do contexto de segurança
        Long orgId = SecurityUtils.getCurrentOrgId();

        // Busca a entrega existente
        Entrega entregaExistente = entregaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrega não encontrada"));

        // Verifica se a entrega pertence à organização correta
        if (!entregaExistente.getOrg().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Entrega não pertence à organização.");
        }

        // Atualiza as entidades relacionadas
        Consumidor consumidor = buscarConsumidorPorId(entregaRequest.getConsumidorId());
        Produto produto = buscarProdutoPorId(entregaRequest.getProdutoId());
        Usuario entregador = buscarEntregadorPorId(entregaRequest.getEntregadorId());

        entregaExistente.setConsumidor(consumidor);
        entregaExistente.setProduto(produto);
        entregaExistente.setEntregador(entregador);
        entregaExistente.setQuantidade(entregaRequest.getQuantidade());
        entregaExistente.setHorarioEntrega(entregaRequest.getHorarioEntrega() != null ? entregaRequest.getHorarioEntrega() : LocalDateTime.now());

        entregaExistente.calcularValor();  // Recalcula o valor da entrega

        return entregaRepository.save(entregaExistente);  // Salva a entrega atualizada
    }

    // Deletar uma entrega existente
    public void deletarEntrega(Long id) {
        // O orgId é passado através do contexto de segurança
        Long orgId = SecurityUtils.getCurrentOrgId();

        // Verifica se a entrega existe
        Entrega entrega = entregaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrega não encontrada"));

        // Verifica se a entrega pertence à organização correta
        if (!entrega.getOrg().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Você não pode excluir uma entrega de outra organização.");
        }

        entregaRepository.delete(entrega);  // Deleta a entrega
    }

    // Total de entregas por dia (soma dos valores) filtrando pela organização
    public BigDecimal getTotalPorDia(LocalDate dia) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalPorDia(dia, orgId);
    }

    // Total de entregas por semana (soma dos valores) filtrando pela organização
    public BigDecimal getTotalSemanal(LocalDate inicioSemana, LocalDate fimSemana) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalSemanal(inicioSemana, fimSemana, orgId);
    }

    // Total de entregas por mês (soma dos valores) filtrando pela organização
    public BigDecimal getTotalMensal(int mes, int ano) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalMensal(mes, ano, orgId);
    }

    // Total de entregas feitas por um consumidor específico filtrando pela organização
    public BigDecimal getTotalPorConsumidor(Long consumidorId) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalPorConsumidor(consumidorId, orgId);
    }

    // Total de entregas no mês atual filtrando pela organização
    public BigDecimal getTotalDoMesAtual() {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalDoMesAtual(orgId);
    }

    // Total de entregas semanais por consumidor filtrando pela organização
    public BigDecimal getTotalSemanalPorConsumidor(Long consumidorId, LocalDate inicioSemana, LocalDate fimSemana) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalSemanalPorConsumidor(consumidorId, inicioSemana, fimSemana, orgId);
    }

    // Consultar total anual de entregas
    public BigDecimal getTotalAnual(int ano) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalAnual(ano, orgId);
    }

    // Consultar total anual de entregas feitas por consumidor
    public BigDecimal getTotalAnualPorConsumidor(Long consumidorId, int ano) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalAnualPorConsumidor(consumidorId, ano, orgId);
    }

    // Métodos de busca por ID
    private Consumidor buscarConsumidorPorId(Long consumidorId) {
        return consumidorRepository.findById(consumidorId)
                .orElseThrow(() -> new ResourceNotFoundException("Consumidor não encontrado."));
    }

    private Produto buscarProdutoPorId(Long produtoId) {
        return produtoRepository.findById(produtoId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado."));
    }

    private Usuario buscarEntregadorPorId(Long entregadorId) {
        return usuarioRepository.findById(entregadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregador não encontrado."));
    }

    // Listar todas as entregas de uma organização com paginação
    public Page<Entrega> listarEntregas(Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();  // Obtém o org_id do contexto de segurança
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }
        return entregaRepository.findByOrgId(orgId, pageable);  // Retorna as entregas com paginação
    }
}
