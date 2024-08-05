package org.example;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

public class ProxyServer {
    private static final int PORT = 8080;
    private static final Logger logger = Logger.getLogger(ProxyServer.class.getName());
    private static boolean running = false;
    private static final Set<String> blockedHosts = new HashSet<>();
    private static final Map<String, CacheEntry> cache = new HashMap<>();
    private static final Set<String> requestLog = new HashSet<>();

    public static void start() {
        running = true;
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Proxy server started on port " + PORT);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not start server", e);
        }
    }
    public static void stop() {
        running = false;
        logger.info("Proxy server stopped.");
    }
    public static void addBlockedHost(String host) {
        blockedHosts.add(host);
    }
    public static Set<String> getBlockedHosts() {
        return blockedHosts;
    }
    public static void generateReport() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("report.txt"))) {
            for (String logEntry : requestLog) {
                writer.write(logEntry);
                writer.newLine();
            }
            logger.info("Report generated: report.txt");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not generate report", e);
        }
    }

    //handle each client connection in a thread
    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (InputStream clientInput = clientSocket.getInputStream();
                 OutputStream clientOutput = clientSocket.getOutputStream()) {

                BufferedReader reader = new BufferedReader(new InputStreamReader(clientInput));
                String requestLine = reader.readLine();

                if (requestLine != null && !requestLine.isEmpty()) {
                    logger.info("Request: " + requestLine);
                    String[] requestParts = requestLine.split(" ");
                    String method = requestParts[0];
                    String url = requestParts[1];

                    if (!method.equals("CONNECT")) {
                        requestLog.add(requestLine);
                    }

                    if (isBlocked(url)) {
                        clientOutput.write("HTTP/1.1 401 Unauthorized\r\n\r\nBlocked by proxy\r\n".getBytes());
                    } else {
                        switch (method) {
                            case "CONNECT":
                                handleConnectRequest(clientOutput, clientSocket, url);
                                break;
                            case "GET":
                                handleGetRequest(clientOutput, url);
                                break;
                            case "HEAD":
                                handleHeadRequest(clientOutput, url);
                                break;
                            case "OPTIONS":
                                handleOptionsRequest(clientOutput, url);
                                break;
                            case "POST":
                                handlePostRequest(clientOutput, url, reader, clientInput);
                                break;
                            default:
                                clientOutput.write("HTTP/1.1 405 Method Not Allowed\r\n\r\n".getBytes());
                                break;
                        }
                    }
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error handling client request", e);
            }
        }

        private boolean isBlocked(String url) {
            try {
                URL parsedUrl = new URL(url);
                return blockedHosts.contains(parsedUrl.getHost());
            } catch (MalformedURLException e) {
                logger.log(Level.WARNING, "Malformed URL", e);
                return false;
            }
        }

        private void handleConnectRequest(OutputStream clientOutput, Socket clientSocket, String url) {
            String[] urlParts = url.split(":");
            String host = urlParts[0];
            int port = Integer.parseInt(urlParts[1]);

            try (Socket proxySocket = new Socket(host, port)) {
                clientOutput.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());

                InputStream proxyInput = proxySocket.getInputStream();
                OutputStream proxyOutput = proxySocket.getOutputStream();

                // Create threads for data transfer
                Thread clientToProxy = new Thread(() -> {
                    try {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = clientSocket.getInputStream().read(buffer)) != -1) {
                            proxyOutput.write(buffer, 0, bytesRead);
                        }
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Error in client to proxy stream", e);
                    }
                });

                Thread proxyToClient = new Thread(() -> {
                    try {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = proxyInput.read(buffer)) != -1) {
                            clientOutput.write(buffer, 0, bytesRead);
                        }
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Error in proxy to client stream", e);
                    }
                });

                // Start data transfer threads
                clientToProxy.start();
                proxyToClient.start();

                // Wait for both threads to complete
                clientToProxy.join();
                proxyToClient.join();

            } catch (IOException | InterruptedException e) {
                logger.log(Level.SEVERE, "Error handling CONNECT request", e);
                try {
                    clientOutput.write("HTTP/1.1 500 Internal Server Error\r\n\r\n".getBytes());
                } catch (IOException ioException) {
                    logger.log(Level.SEVERE, "Error writing error response", ioException);
                }
            }
        }

        private void handleGetRequest(OutputStream clientOutput, String url) {
            handleHttpRequest(clientOutput, url, "GET");
        }

        private void handleHeadRequest(OutputStream clientOutput, String url) {
            handleHttpRequest(clientOutput, url, "HEAD");
        }

        private void handleOptionsRequest(OutputStream clientOutput, String url) {
            handleHttpRequest(clientOutput, url, "OPTIONS");
        }

        private void handlePostRequest(OutputStream clientOutput, String url, BufferedReader reader, InputStream clientInput) {
            try {
                URL remoteURL = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) remoteURL.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                try (OutputStream proxyOutput = connection.getOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while (clientInput.available() > 0 && (bytesRead = clientInput.read(buffer)) != -1) {
                        proxyOutput.write(buffer, 0, bytesRead);
                    }
                }

                handleServerResponse(clientOutput, connection);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error handling POST request", e);
                try {
                    clientOutput.write("HTTP/1.1 500 Internal Server Error\r\n\r\n".getBytes());
                } catch (IOException ioException) {
                    logger.log(Level.SEVERE, "Error writing error response", ioException);
                }
            }
        }

        private void handleHttpRequest(OutputStream clientOutput, String url, String method) {
            HttpURLConnection connection = null;
            try {
                URL remoteURL = new URL(url);
                connection = (HttpURLConnection) remoteURL.openConnection();
                connection.setRequestMethod(method);

                if (method.equals("GET")) {
                    CacheEntry cacheEntry = cache.get(url);
                    if (cacheEntry != null) {
                        logger.info("Cache hit for URL: " + url);
                        connection.setRequestProperty("If-Modified-Since", cacheEntry.getLastModified());
                    } else {
                        logger.info("Cache miss for URL: " + url);
                    }
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    CacheEntry cacheEntry = cache.get(url);
                    if (cacheEntry != null) {
                        clientOutput.write("HTTP/1.1 304 Not Modified\r\n".getBytes());
                        clientOutput.write("\r\n".getBytes());
                        clientOutput.write(cacheEntry.getData());
                        return;
                    }
                }

                clientOutput.write(("HTTP/1.1 " + responseCode + " " + connection.getResponseMessage() + "\r\n").getBytes());
                connection.getHeaderFields().forEach((key, values) -> {
                    if (key != null) {
                        values.forEach(value -> {
                            try {
                                clientOutput.write((key + ": " + value + "\r\n").getBytes());
                            } catch (IOException e) {
                                logger.log(Level.SEVERE, "Error writing header to client", e);
                            }
                        });
                    }
                });
                clientOutput.write("\r\n".getBytes());

                try (InputStream remoteInput = connection.getInputStream()) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] data = new byte[65536]; // Increased buffer size to 64 KB
                    int bytesRead;
                    while ((bytesRead = remoteInput.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, bytesRead);
                        clientOutput.write(data, 0, bytesRead);
                    }
                    buffer.flush();

                    if (method.equals("GET") && responseCode == HttpURLConnection.HTTP_OK) {
                        String lastModified = connection.getHeaderField("Last-Modified");
                        if (lastModified != null) {
                            cache.put(url, new CacheEntry(buffer.toByteArray(), lastModified));
                            String logEntry = "Cached URL: " + url + " with Last-Modified: " + lastModified;
                            logger.info(logEntry);
                            requestLog.add(logEntry);
                        }
                    }
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error reading response body", e);
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error handling " + method + " request", e);
                try {
                    clientOutput.write("HTTP/1.1 500 Internal Server Error\r\n\r\n".getBytes());
                } catch (IOException ioException) {
                    logger.log(Level.SEVERE, "Error writing error response", ioException);
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        private void handleServerResponse(OutputStream clientOutput, HttpURLConnection connection) throws IOException {
            int responseCode = connection.getResponseCode();
            clientOutput.write(("HTTP/1.1 " + responseCode + " " + connection.getResponseMessage() + "\r\n").getBytes());

            connection.getHeaderFields().forEach((key, value) -> {
                if (key != null) {
                    try {
                        clientOutput.write((key + ": " + String.join(", ", value) + "\r\n").getBytes());
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Error writing header", e);
                    }
                }
            });

            clientOutput.write("\r\n".getBytes());

            if (responseCode != HttpURLConnection.HTTP_NOT_MODIFIED) {
                try (InputStream remoteInput = connection.getInputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = remoteInput.read(buffer)) != -1) {
                        clientOutput.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
    }
    //store cached data and last modified date
    private static class CacheEntry {
        private final byte[] data;
        private final String lastModified;

        CacheEntry(byte[] data, String lastModified) {
            this.data = data;
            this.lastModified = lastModified;
        }
        public byte[] getData() {
            return data;
        }

        public String getLastModified() {
            return lastModified;
        }
    }
}
