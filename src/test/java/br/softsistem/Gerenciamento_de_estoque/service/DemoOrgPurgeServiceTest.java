package br.softsistem.Gerenciamento_de_estoque.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoOrgPurgeServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DemoOrgPurgeService service;

    @Test
    void purgeOperationalData_deveUsarColunaPedidoIdCorreta() {
        when(jdbcTemplate.update(anyString(), anyLong())).thenReturn(1);

        service.purgeOperationalData(42L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).update(sqlCaptor.capture(), anyLong());

        boolean usesPedidoId = sqlCaptor.getAllValues().stream()
                .anyMatch(sql -> sql.contains("pedido_venda_itens") && sql.contains("pedido_id"));
        assertTrue(usesPedidoId, "SQL de pedido_venda_itens deve usar coluna pedido_id");
    }
}
