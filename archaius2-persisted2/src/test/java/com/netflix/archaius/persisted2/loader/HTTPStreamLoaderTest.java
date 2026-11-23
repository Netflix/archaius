package com.netflix.archaius.persisted2.loader;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class HTTPStreamLoaderTest {
    private HttpServer server;
    private URL url;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
        url = new URL("http://localhost:" + server.getAddress().getPort() + "/props");
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int n = in.read(buffer);
        while (n != -1) {
            out.write(buffer, 0, n);
            n = in.read(buffer);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    @Test
    void returnsBodyOn200() throws Exception {
        byte[] body = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);
        server.createContext("/props", exchange -> {
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        InputStream result = new HTTPStreamLoader(url).call();

        assertEquals("{\"key\":\"value\"}", readAll(result));
    }

    @Test
    void decompressesGzip() throws Exception {
        // set up: compress "hello" with gzip
        ByteArrayOutputStream zipped = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(zipped)) {
            gz.write("hello".getBytes(StandardCharsets.UTF_8));
        }
        byte[] body = zipped.toByteArray();

        // server replies 200, says "this is gzipped", sends the compressed bytes
        server.createContext("/props", exchange -> {
            exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        // act + check: we should get plain "hello" back
        assertEquals("hello", readAll(new HTTPStreamLoader(url).call()));
    }

    @Test
    void returnsNullOn304() throws Exception {
        // server says "nothing changed", with no body
        server.createContext("/props", exchange -> {
            exchange.sendResponseHeaders(304, -1);
            exchange.close();
        });

        assertNull(new HTTPStreamLoader(url).call());
    }

    @Test
    void throwsOnServerError() {
        // server fails with 500
        server.createContext("/props", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });

        assertThrows(RuntimeException.class, () -> new HTTPStreamLoader(url).call());
    }
}