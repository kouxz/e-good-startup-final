package com.projeto.egoodapp.data.nearby;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Reads an HTTP response without allowing an unbounded allocation. */
public final class BoundedResponseReader {
    private BoundedResponseReader() {}

    public static String readUtf8(InputStream input, int maximumBytes) throws IOException {
        if (maximumBytes <= 0) throw new IllegalArgumentException("maximumBytes must be positive");
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maximumBytes, 16 * 1024));
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (read > maximumBytes - total) {
                throw new IOException("Resposta do serviço de mapas maior que o permitido");
            }
            output.write(buffer, 0, read);
            total += read;
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
