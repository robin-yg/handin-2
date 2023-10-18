package com.example;

import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Scanner;

public class Client {

    private static final String[] protocols = new String[]{"TLSv1.3"};
    private static final String[] cipher_suites = new String[]{"TLS_AES_128_GCM_SHA256"};
    private static final String keystorePath = "client.keystore"; // Update with the path to your client keystore file
    private static final String keystorePassword = "password"; // Update with the keystore password

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java Client <name>");
            return;
        }
        String name = args[0];

        SSLSocket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        try {
            // Create a custom TrustManager that trusts all certificates
            TrustManager[] trustAllCertificates = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };

            // Create an SSLContext that trusts all certificates
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCertificates, new SecureRandom());

            // Step: 2
            socket = (SSLSocket) sslContext.getSocketFactory().createSocket("127.0.0.1", 8980);

            // Step: 3
            socket.setEnabledProtocols(protocols);
            socket.setEnabledCipherSuites(cipher_suites);

            // Step: 4
            socket.startHandshake();

            // Step: 5
            out = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    socket.getOutputStream())));

            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Thread serverThread = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = inFromServer.readLine()) != null) {
                        System.out.println(msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            serverThread.start();

            // Read and send messages
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter your messages or type 'exit' to quit: ");
            out.println();
            while (true) {
                String message = scanner.nextLine();
                if ("exit".equalsIgnoreCase(message)) {
                    break;
                }
                out.println(name + ": " + message);
                out.println();
                out.flush();
            }

            if (out.checkError()) {
                System.out.println("SSLSocketClient:  java.io.PrintWriter error");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }
}