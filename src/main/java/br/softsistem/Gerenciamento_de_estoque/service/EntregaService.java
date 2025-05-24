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
        Long orgId = SecurityUtils.getCurrentOrgId();

        Consumidor consumidor = buscarConsumidorPorId(entregaRequest.getConsumidorId());
        Produto produto = buscarProdutoPorId(entregaRequest.getProdutoId());
        Usuario entregador = buscarEntregadorPorId(entregaRequest.getEntregadorId());

        if (produto.getQuantidade() < entregaRequest.getQuantidade()) {
            throw new IllegalArgumentException("Quantidade insuficiente de produto em estoque.");
        }

        produto.setQuantidade(produto.getQuantidade() - entregaRequest.getQuantidade());
        produto.setQuantidadeSaida(produto.getQuantidadeSaida() != null ? produto.getQuantidadeSaida() + entregaRequest.getQuantidade() : entregaRequest.getQuantidade());
        produto.setDataSaida(LocalDateTime.now());
        produtoRepository.save(produto);

        Entrega entrega = new Entrega();
        entrega.setConsumidor(consumidor);
        entrega.setProduto(produto);
        entrega.setEntregador(entregador);
        entrega.setQuantidade(entregaRequest.getQuantidade());
        entrega.setHorarioEntrega(LocalDateTime.now());

        return entregaRepository.save(entrega);
    }

    public Entrega editarEntrega(Long id, EntregaRequestDto entregaRequest) {
        Long orgId = SecurityUtils.getCurrentOrgId();

        Entrega entregaExistente = entregaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrega não encontrada"));

        if (!entregaExistente.getOrg().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Entrega não pertence à organização.");
        }

        Consumidor consumidor = buscarConsumidorPorId(entregaRequest.getConsumidorId());
        Produto produto = buscarProdutoPorId(entregaRequest.getProdutoId());
        Usuario entregador = buscarEntregadorPorId(entregaRequest.getEntregadorId());

        int quantidadeAnterior = entregaExistente.getQuantidade();
        int novaQuantidade = entregaRequest.getQuantidade();
        int diferenca = novaQuantidade - quantidadeAnterior;

        if (produto.getQuantidade() < diferenca) {
            throw new IllegalArgumentException("Quantidade insuficiente de produto em estoque para atualização.");
        }

        produto.setQuantidade(produto.getQuantidade() - diferenca);
        produto.setQuantidadeSaida(produto.getQuantidadeSaida() != null ? produto.getQuantidadeSaida() + diferenca : diferenca);
        produto.setDataSaida(LocalDateTime.now());
        produtoRepository.save(produto);

        entregaExistente.setConsumidor(consumidor);
        entregaExistente.setProduto(produto);
        entregaExistente.setEntregador(entregador);
        entregaExistente.setQuantidade(novaQuantidade);
        entregaExistente.setHorarioEntrega(LocalDateTime.now());
        entregaExistente.calcularValor();

        return entregaRepository.save(entregaExistente);
    }

    public void deletarEntrega(Long id) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        Entrega entrega = entregaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrega não encontrada"));

        if (!entrega.getOrg().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Você não pode excluir uma entrega de outra organização.");
        }

        Produto produto = entrega.getProduto();
        produto.setQuantidade(produto.getQuantidade() + entrega.getQuantidade());
        produto.setQuantidadeSaida(produto.getQuantidadeSaida() != null ? produto.getQuantidadeSaida() - entrega.getQuantidade() : 0);
        produtoRepository.save(produto);

        entregaRepository.delete(entrega);
    }

    public BigDecimal getTotalPorDia(LocalDate dia) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalPorDia(dia, orgId);
    }

    public BigDecimal getTotalSemanal(LocalDate inicioSemana, LocalDate fimSemana) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalSemanal(inicioSemana, fimSemana, orgId);
    }

    public BigDecimal getTotalMensal(int mes, int ano) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalMensal(mes, ano, orgId);
    }

    public BigDecimal getTotalPorConsumidor(Long consumidorId) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalPorConsumidor(consumidorId, orgId);
    }

    public BigDecimal getTotalDoMesAtual() {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalDoMesAtual(orgId);
    }

    public BigDecimal getTotalSemanalPorConsumidor(Long consumidorId, LocalDate inicioSemana, LocalDate fimSemana) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalSemanalPorConsumidor(consumidorId, inicioSemana, fimSemana, orgId);
    }

    public BigDecimal getTotalAnual(int ano) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalAnual(ano, orgId);
    }

    public BigDecimal getTotalAnualPorConsumidor(Long consumidorId, int ano) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalAnualPorConsumidor(consumidorId, ano, orgId);
    }

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

    public Page<Entrega> listarEntregas(Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }
        return entregaRepository.findByOrgId(orgId, pageable);
    }
}
