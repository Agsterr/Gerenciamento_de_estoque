package br.softsistem.Gerenciamento_de_estoque.dto;



import java.math.BigDecimal;
import java.time.LocalDate;

public class VendaDTO {

    private Long id;
    private String cliente;
    private LocalDate data;
    private BigDecimal valorTotal;

    public VendaDTO() {
    }

    public VendaDTO(Long id, String cliente, LocalDate data, BigDecimal valorTotal) {
        this.id = id;
        this.cliente = cliente;
        this.data = data;
        this.valorTotal = valorTotal;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }
}

