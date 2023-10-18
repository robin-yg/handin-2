package com.example;

import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final String[] protocols = new String[]{"TLSv1.3"};
    private static final String[] cipher_suites = new String[]{"TLS_AES_128_GCM_SHA256"};
    private static final String keystorePath = "server.keystore"; // Update with the path to your server keystore file
    private static final String keystorePassword = "password"; // Update with the keystore password

    public static void main(String[] args) throws Exception {

        SSLServerSocket serverSocket = null;
        ExecutorService executorService = Executors.newFixedThreadPool(10); // Set the desired number of threads

        try {
            // Load the server keystore
            KeyStore keystore = KeyStore.getInstance("JKS");
            FileInputStream keystoreFile = new FileInputStream(keystorePath);
            keystore.load(keystoreFile, keystorePassword.toCharArray());

            // Create and initialize the SSLContext with the server keystore
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keystore, keystorePassword.toCharArray());

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

            // Step: 2
            serverSocket = (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket(8980);

            // Step: 3
            serverSocket.setEnabledProtocols(protocols);
            serverSocket.setEnabledCipherSuites(cipher_suites);

            while (true) {
                // Accept a new client connection
                final SSLSocket sslSocket = (SSLSocket) serverSocket.accept();

                // Use a thread to handle the client connection
                executorService.execute(() -> handleClientConnection(sslSocket));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }
    private static List<PrintWriter> clientWriters = new CopyOnWriteArrayList<>();


    private static void handleClientConnection(SSLSocket sslSocket) {
        try {
            InputStream inputStream = sslSocket.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            PrintWriter out = new PrintWriter(sslSocket.getOutputStream(), true);

            // Add the client's output stream to the list
            clientWriters.add(out);

            String request;
            while ((request = bufferedReader.readLine()) != null) {
                System.out.println(request);

                // Broadcast the message to all connected clients
                for (PrintWriter clientWriter : clientWriters) {
                    clientWriter.println(request);
                    clientWriter.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                sslSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
