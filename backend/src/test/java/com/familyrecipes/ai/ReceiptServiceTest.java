package com.familyrecipes.ai;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.familyrecipes.common.ApiException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ReceiptServiceTest {
    private final ReceiptService service=new ReceiptService(null,null,new ObjectMapper(),null,null,null,null);
    @Test void handlesFencesMissingEstimatesAndNumericQuantities() throws Exception {
        var result=service.parse("```json\n[{\"name\":\"milk\",\"quantity\":2,\"storage\":\"冷藏\",\"estimated_days\":null},{\"name\":\"rice\",\"quantity\":\"1kg\",\"storage\":\"常温\",\"estimated_days\":180}]\n```");
        assertEquals(2,result.size());assertEquals("2",result.get(0).quantity());assertNull(result.get(0).estimated_days());assertEquals(180,result.get(1).estimated_days());
    }
    @Test void rejectsInvalidShapesAndLeavesBadEstimatesEmpty() throws Exception {
        assertThrows(Exception.class,()->service.parse("not json"));assertThrows(Exception.class,()->service.parse("{}"));
        var result=service.parse("{\"items\":[{\"name\":\"egg\",\"storage\":\"unknown\",\"estimated_days\":-1},{\"name\":null}]}");
        assertEquals(1,result.size());assertNull(result.get(0).storage());assertNull(result.get(0).estimated_days());
    }
    @Test void keepsChineseAndEnglishProductNames() throws Exception {
        var result=service.parse("[{\"name\":\"伊利纯牛奶 Milk 250ml*12\",\"quantity\":\"250ml*12\",\"storage\":\"冷藏\",\"estimated_days\":180}]");
        assertEquals("伊利纯牛奶 Milk 250ml*12",result.get(0).name());assertEquals("250ml*12",result.get(0).quantity());
    }
    @Test void textRecognitionOnlyAcceptsReceiptMode() {
        assertThrows(ApiException.class,()->service.recognizeText(1L,1L,"milk","item"));
    }
}
