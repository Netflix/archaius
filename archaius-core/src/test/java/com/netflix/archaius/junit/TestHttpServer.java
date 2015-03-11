package com.netflix.archaius.junit;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Rule for running an embedded HTTP server for basic testing.  The
 * server uses ephemeral ports to ensure there are no conflicts when
 * running concurrent unit tests
 * 
 * The server uses HttpServer classes available in the JVM to avoid
 * pulling in any extra dependencies.
 * 
 * Available endpoints are,
 * 
 * /                      Returns a 200
 * /status?code=${code}   Returns a request provide code
 * /noresponse            No response from the server
 * 
 * Optional query parameters
 * delay=${delay}         Inject a delay into the request
 * 
 * @author elandau
 *
 */
public class TestHttpServer implements TestRule {
    public static final String INTERNALERROR_PATH = "/internalerror";
    public static final String NORESPONSE_PATH = "/noresponse";
    public static final String STATUS_PATH = "/status";
    public static final String OK_PATH = "/ok";
    public static final String ROOT_PATH = "/";
    
    private static final int DEFAULT_THREAD_COUNT = 10;
    private static final String DELAY_QUERY_PARAM = "delay";
    
    private HttpServer server;
    private int localHttpServerPort = 0;
    private ExecutorService service;
    private int threadCount = DEFAULT_THREAD_COUNT;
    private LinkedHashMap<String, HttpHandler> handlers = new LinkedHashMap<String, HttpHandler>();
    
    private String GENERIC_RESPONSE = "GenericTestHttpServer Response";
    
    public TestHttpServer() {
        handlers.put(ROOT_PATH, new TestHttpHandler() {
            @Override
            protected void handle(RequestContext context) throws IOException {
                context.response(200, GENERIC_RESPONSE);
            }});
        
        handlers.put(OK_PATH, new TestHttpHandler() {
            @Override
            protected void handle(RequestContext context) throws IOException {
                context.response(200, GENERIC_RESPONSE);
            }});
        
        handlers.put(STATUS_PATH, new TestHttpHandler() {
            @Override
            protected void handle(RequestContext context) throws IOException {
                context.response(Integer.parseInt(context.query("code")), GENERIC_RESPONSE);
            }});
        
        handlers.put(NORESPONSE_PATH, new TestHttpHandler() {
            @Override
            protected void handle(RequestContext context) throws IOException {
            }});
        
        handlers.put(INTERNALERROR_PATH, new TestHttpHandler() {
            @Override
            protected void handle(RequestContext context) throws IOException {
                throw new RuntimeException("InternalError");
            }});
    }
    
    public TestHttpServer handler(String path, HttpHandler handler) {
        handlers.put(path, handler);
        return this;
    }
    
    public TestHttpServer port(int port) {
        this.localHttpServerPort = port;
        return this;
    }
    
    public TestHttpServer threadCount(int threads) {
        this.threadCount = threads;
        return this;
    }
    
    @Override
    public Statement apply(final Statement statement, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before(description);
                try {
                    statement.evaluate();
                } finally {
                    after(description);
                }
            }
        };
    }

    private static interface RequestContext {
        void response(int code, String body) throws IOException;
        String query(String key);
    }
    
    private static abstract class TestHttpHandler implements HttpHandler {
        @Override
        public final void handle(final HttpExchange t) throws IOException {
            try {
                final Map<String, String> queryParameters = queryToMap(t);
                
                if (queryParameters.containsKey(DELAY_QUERY_PARAM)) {
                    Long delay = Long.parseLong(queryParameters.get(DELAY_QUERY_PARAM));
                    if (delay != null) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(delay);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                
                handle(new RequestContext() {
                    @Override
                    public void response(int code, String body) throws IOException {
                        OutputStream os = t.getResponseBody();
                        t.sendResponseHeaders(code, body.length());
                        os.write(body.getBytes());
                        os.close();
                    }
    
                    @Override
                    public String query(String key) {
                        return queryParameters.get(key);
                    } 
                });
            }
            catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                String body = sw.toString();
                
                OutputStream os = t.getResponseBody();
                t.sendResponseHeaders(500, body.length());
                os.write(body.getBytes());
                os.close();
            }
        }
        
        protected abstract void handle(RequestContext context) throws IOException;
        
        private static Map<String, String> queryToMap(HttpExchange t) {
            String queryString = t.getRequestURI().getQuery();
            Map<String, String> result = new HashMap<String, String>();
            if (queryString != null) {
                for (String param : queryString.split("&")) {
                    String pair[] = param.split("=");
                    if (pair.length>1) {
                        result.put(pair[0], pair[1]);
                    }
                    else{
                        result.put(pair[0], "");
                    }
                }
            }
            return result;
        }

    }
    
    public void before(final Description description) throws Exception {
        this.service = Executors.newFixedThreadPool(
                threadCount, 
                new ThreadFactoryBuilder().setDaemon(true).setNameFormat("TestHttpServer-%d").build());
        
        InetSocketAddress inetSocketAddress = new InetSocketAddress("localhost", 0);
        server = HttpServer.create(inetSocketAddress, 0);
        server.setExecutor(service);

        for (Entry<String, HttpHandler> handler : handlers.entrySet()) {
            server.createContext(handler.getKey(), handler.getValue());
        }
        
        server.start();
        localHttpServerPort = server.getAddress().getPort();
        
        System.out.println(description.getClassName() + " TestServer is started: " + getServerUrl());
    }

    public void after(final Description description) {
        try{
            server.stop(0);
            ((ExecutorService) server.getExecutor()).shutdownNow();
            
            System.out.println(description.getClassName() + " TestServer is shutdown: " + getServerUrl());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return Get the root server URL
     */
    public String getServerUrl() {
        return "http://localhost:" + localHttpServerPort;
    }

    /**
     * @return Get the root server URL
     * @throws URISyntaxException 
     */
    public URI getServerURI() throws URISyntaxException {
        return new URI(getServerUrl());
    }

    /**
     * @param path
     * @return  Get a path to this server
     */
    public String getServerPath(String path) {
        return getServerUrl() + path;
    }

    /**
     * @param path
     * @return  Get a path to this server
     */
    public URI getServerPathURI(String path) throws URISyntaxException {
        return new URI(getServerUrl() + path);
    }

    /**
     * @return Return the ephemeral port used by this server
     */
    public int getServerPort() {
        return localHttpServerPort;
    }
}