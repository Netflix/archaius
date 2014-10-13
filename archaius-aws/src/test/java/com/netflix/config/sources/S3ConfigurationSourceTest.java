
package com.netflix.config.sources;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.netflix.config.PollResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class S3ConfigurationSourceTest {

    final static boolean INITIAL = false;
    final static Object CHECK_POINT = null;

    HttpServer fakeS3;
    AmazonS3Client client;

    public S3ConfigurationSourceTest() {
    }

    @Before
    public void setup() throws Exception {
        fakeS3 = createHttpServer();
        client = new AmazonS3Client(new StaticCredentialsProvider(new AnonymousAWSCredentials()));
        client.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));
        client.setEndpoint("http://localhost:8069");
    }

    @After
    public void teardown() {
        fakeS3.stop(5);
    }

    @Test
    public void testPoll_shouldLoadSomeData() throws Exception {
        S3ConfigurationSource instance = new S3ConfigurationSource(client, "bucketname", "standard-key.txt");
        PollResult result = instance.poll(INITIAL, CHECK_POINT);

        assertNotNull(result);
        assertEquals("true",result.getComplete().get("loaded"));
        assertEquals(1,result.getComplete().size());
    }

    @Test(expected=AmazonServiceException.class)
    public void testPoll_fileNotFound() throws Exception {
        S3ConfigurationSource instance = new S3ConfigurationSource(client, "bucketname", "404.txt");
        PollResult result = instance.poll(INITIAL, CHECK_POINT);

        assertNotNull(result);
        assertEquals("true",result.getComplete().get("loaded"));
        assertEquals(1,result.getComplete().size());
    }


    public HttpServer createHttpServer() throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(8069), 0);

        // create and register our handler
        httpServer.createContext("/bucketname/standard-key.txt",new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                byte[] response = "loaded=true".getBytes("UTF-8");
                    // RFC 2616 says HTTP headers are case-insensitive - but the
                // Amazon S3 client will crash if ETag has a different
                // capitalisation. And this HttpServer normalises the names
                // of headers using "ETag"->"Etag" if you use put, add or
                // set. But not if you use 'putAll' so that's what I use.
                Map<String, List<String>> responseHeaders = new HashMap();
                responseHeaders.put("ETag", Collections.singletonList("\"TEST-ETAG\""));
                responseHeaders.put("Content-Type", Collections.singletonList("text/plain"));
                exchange.getResponseHeaders().putAll(responseHeaders);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            }
        });

        httpServer.createContext("/bucketname/404.txt",new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                byte[] response = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<Error>\n" +
                        "  <Code>NoSuchKey</Code>\n" +
                        "  <Message>The resource you requested does not exist</Message>\n" +
                        "  <Resource>/bucketname/404.txt</Resource> \n" +
                        "</Error>").getBytes("UTF-8");
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND,response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            }
        });

        httpServer.start();
        return httpServer;
    }

}
