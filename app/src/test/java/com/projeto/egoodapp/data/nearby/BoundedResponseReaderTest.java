package com.projeto.egoodapp.data.nearby;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class BoundedResponseReaderTest {
    @Test public void responseAtLimitIsAcceptedAsUtf8() throws Exception {
        byte[] input = "ação".getBytes(StandardCharsets.UTF_8);
        assertEquals("ação", BoundedResponseReader.readUtf8(
                new ByteArrayInputStream(input), input.length));
    }

    @Test public void responsePastLimitIsRejected() throws Exception {
        byte[] input = "12345".getBytes(StandardCharsets.UTF_8);
        try {
            BoundedResponseReader.readUtf8(new ByteArrayInputStream(input), 4);
            fail("Oversized responses must be rejected");
        } catch (IOException expected) {
            assertEquals("Resposta do serviço de mapas maior que o permitido",
                    expected.getMessage());
        }
    }
}
