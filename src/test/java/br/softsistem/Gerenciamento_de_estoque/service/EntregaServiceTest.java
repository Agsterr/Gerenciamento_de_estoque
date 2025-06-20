package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaComAvisoResponseDto;
import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaRequestDto;
import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaResponseDto;
import br.softsistem.Gerenciamento_de_estoque.model.*;
import br.softsistem.Gerenciamento_de_estoque.repository.ConsumidorRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.EntregaRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.ProdutoRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import org.mockito.MockedStatic;
import org.mockito.Mockito;


@SpringBootTest
class EntregaServiceTest {

    @MockBean
    private EntregaRepository entregaRepository;

    @MockBean
    private ConsumidorRepository consumidorRepository;

    @MockBean
    private ProdutoRepository produtoRepository;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntregaService entregaService;

    private EntregaRequestDto entregaRequestDto;
    private EntregaResponseDto entregaResponseDto;
    private Entrega entrega;

    // Declaração da variável mockada
    private MockedStatic<SecurityUtils> mockedSecurityUtils;

    @BeforeEach
    void setUp() {
        mockedSecurityUtils = Mockito.mockStatic(SecurityUtils.class);
        mockedSecurityUtils.when(SecurityUtils::getCurrentOrgId).thenReturn(1L);
        mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

        Org org = new Org(); org.setId(1L);

        Consumidor consumidor = new Consumidor();
        consumidor.setId(1L);
        consumidor.setNome("Carlos");
        consumidor.setCpf("12345678900");
        consumidor.setEndereco("Rua ABC");
        consumidor.setOrg(org);

        Produto produto = new Produto();
        produto.setId(1L);
        produto.setNome("Produto Teste");
        produto.setQuantidade(100);
        produto.setPreco(new BigDecimal("20.00"));
        produto.setOrg(org);

        entrega = new Entrega();
        entrega.setId(1L);
        entrega.setConsumidor(consumidor);
        entrega.setProduto(produto);
        entrega.setQuantidade(10);
        entrega.setHorarioEntrega(LocalDateTime.parse("2025-06-03T12:00:00"));
        entrega.setOrg(org);

        entregaRequestDto = new EntregaRequestDto();
        entregaRequestDto.setConsumidorId(1L);
        entregaRequestDto.setProdutoId(1L);
        entregaRequestDto.setQuantidade(10);
        entregaRequestDto.setHorarioEntrega(LocalDateTime.parse("2025-06-03T12:00:00"));
    }

    @AfterEach
    void tearDown() {
        mockedSecurityUtils.close();
    }

    @Test
    void criarEntrega() {
        // Simula usuário entregador
        Usuario entregador = new Usuario();
        entregador.setId(1L);
        entregador.setUsername("EntregadorTest");

        // Produto e consumidor simulados
        Produto produto = entrega.getProduto();

        // Entidade de entrega esperada após salvar
        Entrega entregaEsperada = new Entrega();
        entregaEsperada.setId(1L);
        entregaEsperada.setProduto(produto);
        entregaEsperada.setConsumidor(entrega.getConsumidor());
        entregaEsperada.setQuantidade(10);
        entregaEsperada.setEntregador(entregador);
        entregaEsperada.setOrg(produto.getOrg());
        entregaEsperada.setHorarioEntrega(entrega.getHorarioEntrega());

        // Mocks dos repositórios
        when(consumidorRepository.findByIdAndOrgId(anyLong(), anyLong()))
                .thenReturn(Optional.of(entrega.getConsumidor()));
        when(produtoRepository.findByIdAndOrgId(anyLong(), anyLong()))
                .thenReturn(Optional.of(produto));
        when(usuarioRepository.findByIdAndOrgId(anyLong(), anyLong()))
                .thenReturn(Optional.of(entregador));
        when(produtoRepository.save(any(Produto.class))).thenReturn(produto);
        when(entregaRepository.save(any(Entrega.class))).thenReturn(entregaEsperada);

        // Chama o método de serviço
        EntregaComAvisoResponseDto resultado = entregaService.criarEntrega(entregaRequestDto);

        // Validações principais
        assertNotNull(resultado);
        assertNotNull(resultado.entrega());
        assertEquals(1L, resultado.entrega().id());
        assertEquals("EntregadorTest", resultado.entrega().nomeEntregador());

        // Novos campos adicionados no DTO
        assertEquals(produto.getId(), resultado.entrega().produtoId(), "Deve trazer o id do produto");
        assertEquals(entrega.getConsumidor().getId(), resultado.entrega().consumidorId(), "Deve trazer o id do consumidor");

        // Valida flag e mensagem de estoque
        assertFalse(resultado.estoqueBaixo());
        assertNull(resultado.mensagemEstoqueBaixo(), "A mensagem deve ser null quando não há aviso de estoque baixo");
    }

    @Test
    void editarEntrega() {
        EntregaRequestDto entregaRequest = new EntregaRequestDto();
        entregaRequest.setConsumidorId(1L);
        entregaRequest.setProdutoId(1L);
        entregaRequest.setQuantidade(20);
        entregaRequest.setHorarioEntrega(LocalDateTime.parse("2025-06-03T12:00:00"));

        Consumidor consumidor = new Consumidor(1L, "Carlos", "12345678900", "Rua ABC");
        Produto produto = new Produto(1L, "Produto Teste", 100, new BigDecimal("20.00"));
        produto.setQuantidade(10);
        produto.setPreco(new BigDecimal("20.00"));

        Org org = new Org(); org.setId(1L);
        consumidor.setOrg(org);

        Entrega entregaExistente = new Entrega();
        entregaExistente.setId(1L);
        entregaExistente.setConsumidor(consumidor);
        entregaExistente.setProduto(produto);
        entregaExistente.setQuantidade(10);
        entregaExistente.setOrg(org);

        when(entregaRepository.findById(1L)).thenReturn(Optional.of(entregaExistente));
        when(consumidorRepository.findByIdAndOrgId(anyLong(), anyLong())).thenReturn(Optional.of(consumidor));
        when(produtoRepository.findByIdAndOrgId(anyLong(), anyLong())).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any(Produto.class))).thenReturn(produto);
        when(entregaRepository.save(any(Entrega.class))).thenReturn(entregaExistente);

        Entrega resultado = entregaService.editarEntrega(1L, entregaRequest);

        assertNotNull(resultado);
        assertEquals(20, resultado.getQuantidade());
        assertEquals(new BigDecimal("400.00"), resultado.getValor());
    }
    @Test
    void deletarEntrega() {
        // Mocks
        Consumidor consumidor = new Consumidor(1L, "Carlos", "12345678900", "Rua ABC");
        Produto produto = new Produto(1L, "Produto Teste", 100, new BigDecimal("20.00"));
        produto.setQuantidade(10);  // Atribuindo um valor válido à quantidade do produto

        Org org = new Org(); // Criando uma instância de Org
        org.setId(1L);  // Atribuindo um ID para a organização

        Entrega entregaExistente = new Entrega();
        entregaExistente.setId(1L);
        entregaExistente.setConsumidor(consumidor);
        entregaExistente.setProduto(produto);
        entregaExistente.setQuantidade(10);
        entregaExistente.setOrg(org);  // Associe a organização à entrega

        when(entregaRepository.findById(1L)).thenReturn(Optional.of(entregaExistente)); // Simula a busca no repositório
        when(produtoRepository.save(any(Produto.class))).thenReturn(produto);

        // Simula que a exclusão vai acontecer sem erros
        doNothing().when(entregaRepository).delete(any(Entrega.class));

        // Chamada do método
        entregaService.deletarEntrega(1L);

        // Verificações
        verify(entregaRepository, times(1)).delete(any(Entrega.class));  // Verifica se o método de deleção foi chamado
    }



    @Test
    void listarEntregas() {
        // Mocks
        Page<Entrega> pageMock = new PageImpl<>(List.of(entrega));
        when(entregaRepository.findByOrgId(anyLong(), any(Pageable.class))).thenReturn(pageMock);

        // Chamada do método
        Page<Entrega> resultado = entregaService.listarEntregas(Pageable.unpaged());

        // Verificações
        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
    }

    @Test
    void listarEntregasPorDia() {
        // Mocks
        LocalDate dia = LocalDate.of(2025, 6, 3);

        Org org = new Org();
        org.setId(1L);

        Consumidor consumidor = new Consumidor();
        consumidor.setId(1L);
        consumidor.setNome("Carlos");
        consumidor.setCpf("12345678900");
        consumidor.setEndereco("Rua ABC");
        consumidor.setOrg(org);

        Produto produto = new Produto();
        produto.setId(1L);
        produto.setNome("Produto Teste");
        produto.setQuantidade(100);
        produto.setPreco(new BigDecimal("20.00"));
        produto.setOrg(org);

        Usuario entregador = new Usuario();
        entregador.setId(1L);
        entregador.setUsername("Entregador Teste");

        Entrega entrega = new Entrega();
        entrega.setId(1L);
        entrega.setConsumidor(consumidor);
        entrega.setProduto(produto);
        entrega.setQuantidade(10);
        entrega.setHorarioEntrega(LocalDateTime.parse("2025-06-03T12:00:00"));
        entrega.setEntregador(entregador); // Adicionando o entregador!!
        entrega.setOrg(org);

        // Criando um mock de Page para simular a resposta do repositório
        Page<Entrega> pageMock = new PageImpl<>(List.of(entrega), PageRequest.of(0, 10), 1);

        // Mock da consulta no repositório
        when(entregaRepository.findByHorarioEntregaBetweenAndOrgId(any(), any(), anyLong(), any(Pageable.class)))
                .thenReturn(pageMock);

        // Chamada do método
        Page<EntregaResponseDto> resultado = entregaService.listarEntregasPorDia(dia, PageRequest.of(0, 10));

        // Verificações
        assertNotNull(resultado); // Verifica se o resultado não é nulo
        assertEquals(1, resultado.getTotalElements()); // Verifica se há 1 elemento na página
        assertEquals("Entregador Teste", resultado.getContent().get(0).nomeEntregador()); // Verificando se o entregador foi mapeado corretamente
    }


    @Test
    void listarEntregasPorPeriodo() {
        // Mocks
        LocalDateTime inicio = LocalDateTime.of(2025, 6, 1, 0, 0, 0);
        LocalDateTime fim = LocalDateTime.of(2025, 6, 3, 23, 59, 59);

        Org org = new Org();
        org.setId(1L);

        Consumidor consumidor = new Consumidor();
        consumidor.setId(1L);
        consumidor.setNome("Carlos");
        consumidor.setCpf("12345678900");
        consumidor.setEndereco("Rua ABC");
        consumidor.setOrg(org);

        Produto produto = new Produto();
        produto.setId(1L);
        produto.setNome("Produto Teste");
        produto.setQuantidade(100);
        produto.setPreco(new BigDecimal("20.00"));
        produto.setOrg(org);

        Usuario entregador = new Usuario();
        entregador.setId(1L);
        entregador.setUsername("Entregador Teste");

        Entrega entrega = new Entrega();
        entrega.setId(1L);
        entrega.setConsumidor(consumidor);
        entrega.setProduto(produto);
        entrega.setQuantidade(10);
        entrega.setHorarioEntrega(LocalDateTime.parse("2025-06-03T12:00:00"));
        entrega.setEntregador(entregador); // Correção: adicionando o entregador
        entrega.setOrg(org);

        // Criando um mock de Page para simular a resposta do repositório
        Page<Entrega> pageMock = new PageImpl<>(List.of(entrega), PageRequest.of(0, 10), 1);

        // Mock da consulta no repositório
        when(entregaRepository.findByHorarioEntregaBetweenAndOrgId(any(), any(), anyLong(), any(Pageable.class)))
                .thenReturn(pageMock);

        // Chamada do método
        Page<EntregaResponseDto> resultado = entregaService.listarEntregasPorPeriodo(inicio, fim, PageRequest.of(0, 10));

        // Verificações
        assertNotNull(resultado); // Verifica se o resultado não é nulo
        assertEquals(1, resultado.getTotalElements()); // Verifica se há 1 elemento na página
        assertEquals("Entregador Teste", resultado.getContent().get(0).nomeEntregador()); // Verifica o nome do entregador
    }



    @Test
    void listarEntregasPorMes() {
        // Mocks
        int mes = 6;
        int ano = 2025;

        Org org = new Org();
        org.setId(1L);

        Consumidor consumidor = new Consumidor();
        consumidor.setId(1L);
        consumidor.setNome("Carlos");
        consumidor.setCpf("12345678900");
        consumidor.setEndereco("Rua ABC");
        consumidor.setOrg(org);

        Produto produto = new Produto();
        produto.setId(1L);
        produto.setNome("Produto Teste");
        produto.setQuantidade(100);
        produto.setPreco(new BigDecimal("20.00"));
        produto.setOrg(org);

        Usuario entregador = new Usuario();
        entregador.setId(1L);
        entregador.setUsername("Entregador Teste");

        Entrega entrega = new Entrega();
        entrega.setId(1L);
        entrega.setConsumidor(consumidor);
        entrega.setProduto(produto);
        entrega.setQuantidade(10);
        entrega.setHorarioEntrega(LocalDateTime.parse("2025-06-03T12:00:00"));
        entrega.setEntregador(entregador); // Correção: setando entregador
        entrega.setOrg(org);

        // Criando um mock de Page para simular a resposta paginada do repositório
        Page<Entrega> pageMock = new PageImpl<>(List.of(entrega), PageRequest.of(0, 10), 1);

        // Mock da consulta no repositório
        when(entregaRepository.findByHorarioEntregaBetweenAndOrgId(any(), any(), anyLong(), any(Pageable.class)))
                .thenReturn(pageMock);

        // Chamada do método
        Page<EntregaResponseDto> resultado = entregaService.listarEntregasPorMes(mes, ano, PageRequest.of(0, 10));

        // Verificações
        assertNotNull(resultado); // Verifica se o resultado não é nulo
        assertEquals(1, resultado.getTotalElements()); // Verifica se há 1 elemento na página
        assertEquals("Entregador Teste", resultado.getContent().get(0).nomeEntregador()); // Verifica o nome do entregador
    }




    @Test
    void getTotalPorDia() {
        // Mocks
        LocalDate dia = LocalDate.of(2025, 6, 3);
        BigDecimal totalEsperado = new BigDecimal("200.00");

        when(entregaRepository.totalPorIntervalo(any(), any(), anyLong())).thenReturn(totalEsperado);

        // Chamada do método
        BigDecimal total = entregaService.getTotalPorDia(dia);

        // Verificações
        assertNotNull(total);
        assertEquals(new BigDecimal("200.00"), total);
    }

    @Test
    void getTotalSemanal() {
        // Mocks
        LocalDate inicioSemana = LocalDate.of(2025, 6, 1);
        LocalDate fimSemana = LocalDate.of(2025, 6, 7);
        BigDecimal totalEsperado = new BigDecimal("1000.00");

        when(entregaRepository.totalPorIntervalo(any(), any(), anyLong())).thenReturn(totalEsperado);

        // Chamada do método
        BigDecimal total = entregaService.getTotalSemanal(inicioSemana, fimSemana);

        // Verificações
        assertNotNull(total);
        assertEquals(new BigDecimal("1000.00"), total);
    }

    @Test
    void getTotalMensal() {
        // Mocks
        int mes = 6;
        int ano = 2025;
        BigDecimal totalEsperado = new BigDecimal("5000.00");

        when(entregaRepository.totalPorIntervalo(any(), any(), anyLong())).thenReturn(totalEsperado);

        // Chamada do método
        BigDecimal total = entregaService.getTotalMensal(mes, ano);

        // Verificações
        assertNotNull(total);
        assertEquals(new BigDecimal("5000.00"), total);
    }
}
