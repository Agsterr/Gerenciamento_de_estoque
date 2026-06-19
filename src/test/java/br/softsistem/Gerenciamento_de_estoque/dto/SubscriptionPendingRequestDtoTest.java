//package br.softsistem.Gerenciamento_de_estoque.dto;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.Test;
//import java.math.BigDecimal;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//
//public class SubscriptionPendingRequestDtoTest {
//
//    @Test
//    public void testDeserialization() throws Exception {
//        String json = "{\n" +
//                "    \"reason\": \"Yoga classes\",\n" +
//                "    \"external_reference\": \"YG-1234\",\n" +
//                "    \"payer_email\": \"test_user_75650838@testuser.com\",\n" +
//                "    \"auto_recurring\": {\n" +
//                "        \"frequency\": 1,\n" +
//                "        \"frequency_type\": \"months\",\n" +
//                "        \"transaction_amount\": 10,\n" +
//                "        \"currency_id\": \"BRL\"\n" +
//                "    },\n" +
//                "    \"back_url\": \"https://www.yoursite.com\",\n" +
//                "    \"status\": \"pending\"\n" +
//                "}";
//
//        ObjectMapper mapper = new ObjectMapper();
//        SubscriptionPendingRequestDto dto = mapper.readValue(json, SubscriptionPendingRequestDto.class);
//
//        assertEquals("Yoga classes", dto.getReason());
//        assertEquals("YG-1234", dto.getExternalReference());
//        assertEquals("test_user_75650838@testuser.com", dto.getPayerEmail());
//        assertEquals("https://www.yoursite.com", dto.getBackUrl());
//
//        assertNotNull(dto.getAutoRecurring());
//        assertEquals(1, dto.getAutoRecurring().getFrequency());
//        assertEquals("months", dto.getAutoRecurring().getFrequencyType());
//        assertEquals(new BigDecimal("10"), dto.getAutoRecurring().getTransactionAmount());
//        assertEquals("BRL", dto.getAutoRecurring().getCurrencyId());
//    }
//}
