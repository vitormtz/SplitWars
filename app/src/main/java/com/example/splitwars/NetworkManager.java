package com.example.splitwars;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkManager {

    private Context context;
    private Handler mainHandler;
    private ExecutorService networkExecutor;
    private ServerSocket serverSocket;
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private AtomicBoolean isRunning = new AtomicBoolean(true);
    private Thread readThread;
    private NetworkMessage lastReceivedMessage = null;
    private final Object messageLock = new Object();

    public NetworkManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.networkExecutor = Executors.newCachedThreadPool();
    }

    public void createGame() {
        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    closeServerSocket();

                    serverSocket = new ServerSocket();
                    serverSocket.setReuseAddress(true);
                    serverSocket.bind(new InetSocketAddress(8889));
                    serverSocket.setSoTimeout(30000);

                    try {
                        socket = serverSocket.accept();
                        setupStreams();
                        isConnected.set(true);
                        startReadThread();
                    } catch (SocketTimeoutException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    showToast("Erro ao criar o jogo: " + e.getMessage());
                }
            }
        });
    }

    public void connectToHost(final String hostIP) {
        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                closeSocket();

                try {
                    socket = new Socket();
                    boolean connected = false;
                    Exception lastException = null;

                    for (int attempt = 1; attempt <= 3 && !connected && isRunning.get(); attempt++) {
                        try {
                            socket.connect(new InetSocketAddress(hostIP, 8889), 10000);
                            connected = true;
                        } catch (IOException e) {
                            lastException = e;

                            if (socket != null) {
                                try {
                                    socket.close();
                                } catch (IOException closeException) {
                                }
                                socket = new Socket();
                            }

                            try {
                                Thread.sleep(attempt * 1000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }

                    if (connected && isRunning.get()) {
                        setupStreams();
                        isConnected.set(true);
                        startReadThread();
                    } else {
                        if (lastException != null) {
                            throw lastException;
                        } else {
                            throw new IOException("Falha ao conectar após várias tentativas");
                        }
                    }
                } catch (Exception e) {
                    showToast("Erro ao conectar: " + e.getMessage());
                }
            }
        });
    }

    private void setupStreams() {
        try {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            isConnected.set(false);
        }
    }

    private void startReadThread() {
        if (readThread != null && readThread.isAlive()) {
            readThread.interrupt();
        }

        readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning.get() && isConnected.get() && !Thread.currentThread().isInterrupted()) {
                    try {
                        if (inputStream != null) {
                            Object obj = inputStream.readObject();

                            if (obj instanceof NetworkMessage) {
                                synchronized (messageLock) {
                                    lastReceivedMessage = (NetworkMessage) obj;
                                }
                            }
                        } else {
                            isConnected.set(false);
                            break;
                        }
                    } catch (IOException e) {
                        if (isRunning.get()) {
                            isConnected.set(false);
                            showToast("Oponente se desconectou");
                        }
                        break;
                    } catch (ClassNotFoundException e) {
                        // Ignorar - não deveria acontecer com NetworkMessage
                    }
                }
            }
        });

        readThread.start();
    }

    public void sendProjectile(float relativeX, float speedX, float speedY) {
        sendMessage(new Runnable() {
            @Override
            public void run() {
                NetworkMessage message = new NetworkMessage(NetworkMessage.PROJECTILE);
                message.setRelativeX(relativeX);
                message.setSpeedX(speedX);
                message.setSpeedY(speedY);
                sendMessageObject(message);
            }
        });
    }

    public void sendHit() {
        sendMessage(new Runnable() {
            @Override
            public void run() {
                sendMessageObject(new NetworkMessage(NetworkMessage.HIT));
            }
        });
    }

    public void sendScreenSize(int width, int height) {
        sendMessage(new Runnable() {
            @Override
            public void run() {
                NetworkMessage message = new NetworkMessage(NetworkMessage.SCREEN_SIZE);
                message.setScreenWidth(width);
                message.setScreenHeight(height);
                sendMessageObject(message);
            }
        });
    }

    private void sendMessage(Runnable messageTask) {
        if (isConnected.get() && outputStream != null) {
            networkExecutor.execute(messageTask);
        }
    }

    private void sendMessageObject(NetworkMessage message) {
        try {
            outputStream.writeObject(message);
            outputStream.flush();
        } catch (IOException e) {
            isConnected.set(false);
        }
    }

    public NetworkMessage receiveMessage() {
        synchronized (messageLock) {
            NetworkMessage message = lastReceivedMessage;
            lastReceivedMessage = null;
            return message;
        }
    }

    public void closeConnection() {
        isRunning.set(false);
        isConnected.set(false);

        if (readThread != null) {
            readThread.interrupt();
            readThread = null;
        }

        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (outputStream != null) {
                    try {
                        sendMessageObject(new NetworkMessage(NetworkMessage.DISCONNECT));
                    } catch (Exception e) {
                        // Ignorar erros ao desconectar
                    }
                }

                closeStreams();
                closeSocket();
                closeServerSocket();
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                    networkExecutor.shutdown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    private void closeStreams() {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            outputStream = null;
        }

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = null;
        }
    }

    private void closeSocket() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e){
                e.printStackTrace();
            }
            socket = null;
        }
    }

    private void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverSocket = null;
        }
    }

    private void showToast(final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public boolean isConnected() {
        return isConnected.get();
    }
}