package com.craftpass.net;

import com.craftpass.CraftPassPlugin;
import com.craftpass.config.PluginConfig;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * High-concurrency TCP Server for CraftPass client connections.
 * Completely independent of HTTP / Web, bypassing all ICP requirements.
 */
public class TcpServer {
    private final CraftPassPlugin plugin;
    private final PluginConfig config;
    private final Logger logger;

    private ServerSocket serverSocket;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final ConcurrentHashMap<String, Integer> ipConnectionCounts = new ConcurrentHashMap<>();
    private volatile boolean running = false;
    private Thread acceptThread;

    public TcpServer(CraftPassPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getPluginConfig();
        this.logger = plugin.getLogger();
    }

    public synchronized void start() {
        if (running) return;
        try {
            InetAddress bindAddr = InetAddress.getByName(config.getBindAddress());
            this.serverSocket = new ServerSocket(config.getPort(), 50, bindAddr);
            this.running = true;

            this.acceptThread = new Thread(this::acceptLoop, "CraftPass-TcpServer-Acceptor");
            this.acceptThread.setDaemon(true);
            this.acceptThread.start();

            logger.info("[TcpServer] Listening on " + config.getBindAddress() + ":" + config.getPort() + " (Non-HTTP Raw TCP, ICP-exempt)");
        } catch (IOException e) {
            logger.severe("[TcpServer] Failed to bind TCP server on port " + config.getPort() + ": " + e.getMessage());
        }
    }

    private void acceptLoop() {
        while (running && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                String ip = clientSocket.getInetAddress().getHostAddress();

                // Check max connections per IP
                int currentCount = ipConnectionCounts.compute(ip, (k, v) -> (v == null ? 1 : v + 1));
                if (currentCount > config.getMaxConnectionsPerIp()) {
                    ipConnectionCounts.computeIfPresent(ip, (k, v) -> v - 1);
                    clientSocket.close();
                    continue;
                }

                threadPool.execute(() -> {
                    try {
                        ClientSession session = new ClientSession(plugin, clientSocket);
                        session.run();
                    } finally {
                        ipConnectionCounts.computeIfPresent(ip, (k, v) -> (v <= 1 ? null : v - 1));
                    }
                });
            } catch (IOException e) {
                if (!running) break;
            }
        }
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}

        threadPool.shutdownNow();
        logger.info("[TcpServer] TCP Server stopped.");
    }
}
