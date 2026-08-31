package com.example.splitwars;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class LobbyActivity extends AppCompatActivity {
    private static final int TCP_PORT = 7777;
    private List<Player> playerList = new ArrayList<>();
    private PlayerAdapter playerAdapter;
    private Handler handler = new Handler(Looper.getMainLooper());
    private String myName;
    private String myIp;
    private int selectedShipType = 1;
    private ExecutorService executor;
    private ServerSocket serverSocket;
    private AtomicBoolean isServerRunning = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Carregar nave selecionada
        SharedPreferences prefs = getSharedPreferences("SplitWarsPrefs", MODE_PRIVATE);
        selectedShipType = prefs.getInt("selected_ship", 1);

        executor = Executors.newCachedThreadPool();

        // Configurar nome e IP
        myName = "Jogador_" + (int)(Math.random() * 1000);
        myIp = getLocalIpAddress();

        ((TextView) findViewById(R.id.player_name)).setText("Nome: " + myName);
        ((TextView) findViewById(R.id.player_ip)).setText("IP: " + myIp);

        // Configurar botão de seleção de nave
        Button selectShipButton = findViewById(R.id.select_ship_button);
        updateShipButtonText(selectShipButton);
        selectShipButton.setOnClickListener(v -> showShipSelectionDialog(selectShipButton));

        // Configurar RecyclerView
        RecyclerView playerListView = findViewById(R.id.player_list);
        playerListView.setLayoutManager(new LinearLayoutManager(this));
        playerAdapter = new PlayerAdapter(playerList, this::connectAndInvite);
        playerListView.setAdapter(playerAdapter);

        // Configurar botão adicionar jogador
        findViewById(R.id.refresh_button).setOnClickListener(v -> showAddPlayerDialog());

        startTcpServer();
    }

    private void updateShipButtonText(Button button) {
        String shipName = selectedShipType == 1 ? "Nave 1" : "Nave 2";
        button.setText(shipName);
    }

    private void showShipSelectionDialog(Button selectShipButton) {
        View dialogView = getLayoutInflater().inflate(R.layout.ship_selection, null);

        View ship1Option = dialogView.findViewById(R.id.ship_option_1);
        View ship2Option = dialogView.findViewById(R.id.ship_option_2);
        TextView ship1Selected = dialogView.findViewById(R.id.ship_1_selected);
        TextView ship2Selected = dialogView.findViewById(R.id.ship_2_selected);

        // Mostrar seleção atual
        updateShipSelection(ship1Selected, ship2Selected);

        final int[] tempSelectedShip = {selectedShipType};

        ship1Option.setOnClickListener(v -> {
            tempSelectedShip[0] = 1;
            ship1Selected.setVisibility(View.VISIBLE);
            ship2Selected.setVisibility(View.GONE);
        });

        ship2Option.setOnClickListener(v -> {
            tempSelectedShip[0] = 2;
            ship1Selected.setVisibility(View.GONE);
            ship2Selected.setVisibility(View.VISIBLE);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.confirm_button).setOnClickListener(v -> {
            selectedShipType = tempSelectedShip[0];

            // Salvar seleção
            getSharedPreferences("SplitWarsPrefs", MODE_PRIVATE)
                    .edit()
                    .putInt("selected_ship", selectedShipType)
                    .apply();

            updateShipButtonText(selectShipButton);
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.cancel_button).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateShipSelection(TextView ship1Selected, TextView ship2Selected) {
        if (selectedShipType == 1) {
            ship1Selected.setVisibility(View.VISIBLE);
            ship2Selected.setVisibility(View.GONE);
        } else {
            ship1Selected.setVisibility(View.GONE);
            ship2Selected.setVisibility(View.VISIBLE);
        }
    }

    private void showAddPlayerDialog() {
        EditText input = new EditText(this);
        input.setHint("Endereço IP (ex: 192.168.1.100)");

        new AlertDialog.Builder(this)
                .setTitle("Adicionar jogador por IP")
                .setView(input)
                .setPositiveButton("Adicionar", (dialog, which) -> {
                    String ip = input.getText().toString().trim();
                    if (!ip.isEmpty()) {
                        playerList.add(new Player("Jogador em " + ip, ip));
                        playerAdapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void startTcpServer() {
        if (isServerRunning.get()) return;

        isServerRunning.set(true);
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(TCP_PORT);

                while (isServerRunning.get() && !Thread.interrupted()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        executor.execute(() -> handleConnection(clientSocket));
                    } catch (IOException e) {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeServer();
            }
        }).start();
    }

    private void handleConnection(Socket socket) {
        try {
            byte[] buffer = new byte[1024];
            int bytesRead = socket.getInputStream().read(buffer);

            if (bytesRead > 0) {
                String message = new String(buffer, 0, bytesRead);
                String senderIp = socket.getInetAddress().getHostAddress();
                processMessage(message, senderIp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processMessage(String message, String senderIp) {
        String[] parts = message.split("\\|");
        if (parts.length < 3 || !"SPLITWARS".equals(parts[0])) return;

        String type = parts[1];
        String name = parts[2];

        switch (type) {
            case "PRESENCE":
                handlePresenceMessage(name, senderIp);
                break;
            case "INVITE":
                handler.post(() -> showInviteDialog(name, senderIp));
                break;
            case "ACCEPT":
                handler.post(() -> startGame(senderIp, true));
                break;
        }
    }

    private void handlePresenceMessage(String name, String ip) {
        handler.post(() -> {
            boolean found = playerList.stream().anyMatch(p -> p.getIp().equals(ip));
            if (!found) {
                playerList.add(new Player(name, ip));
                playerAdapter.notifyDataSetChanged();
            }
        });
    }

    private void connectAndInvite(int position) {
        Player player = playerList.get(position);
        sendMessage(player.getIp(), "SPLITWARS|INVITE|" + myName,
                () -> Toast.makeText(this, "Jogador não encontrado", Toast.LENGTH_LONG).show());
    }

    private void sendMessage(String targetIp, String message, Runnable onError) {
        executor.execute(() -> {
            try (Socket socket = new Socket(targetIp, TCP_PORT)) {
                socket.getOutputStream().write(message.getBytes());
            } catch (IOException e) {
                if (onError != null) {
                    handler.post(onError);
                }
            }
        });
    }

    private void showInviteDialog(String playerName, String playerIp) {
        new AlertDialog.Builder(this)
                .setTitle("Convite para jogar")
                .setMessage(playerName + " convidou você para uma partida!")
                .setPositiveButton("Aceitar", (dialog, which) -> {
                    sendMessage(playerIp, "SPLITWARS|ACCEPT|" + myName, null);
                    startGame(playerIp, false);
                })
                .setNegativeButton("Recusar", null)
                .setCancelable(false)
                .show();
    }

    private void startGame(String opponentIp, boolean isHost) {
        Intent gameIntent = new Intent(this, GameActivity.class);
        gameIntent.putExtra("OPPONENT_IP", opponentIp);
        gameIntent.putExtra("IS_HOST", isHost);
        gameIntent.putExtra("PLAYER_NAME", myName);
        gameIntent.putExtra("SELECTED_SHIP_TYPE", selectedShipType);
        startActivity(gameIntent);
    }

    private void closeServer() {
        isServerRunning.set(false);
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeServer();
        if (executor != null) {
            executor.shutdown();
        }
    }

    private String getLocalIpAddress() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int ip = wifiManager.getConnectionInfo().getIpAddress();

            String ipAddress = String.format("%d.%d.%d.%d",
                    (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));

            if (!ipAddress.equals("0.0.0.0")) {
                return ipAddress;
            }
        } catch (Exception e) {
            // Fallback para NetworkInterface
        }

        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface intf = en.nextElement();
                if (!intf.isUp() || intf.isLoopback() || intf.isVirtual()) continue;

                Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                while (enumIpAddr.hasMoreElements()) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() &&
                            !inetAddress.isLinkLocalAddress() &&
                            inetAddress.getHostAddress().indexOf(':') == -1) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "127.0.0.1";
    }

    // Classes internas
    public static class Player {
        private String name;
        private String ip;

        public Player(String name, String ip) {
            this.name = name;
            this.ip = ip;
        }

        public String getName() { return name; }
        public String getIp() { return ip; }
    }

    public interface OnInviteClickListener {
        void onInviteClick(int position);
    }

    public class PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder> {
        private List<Player> players;
        private OnInviteClickListener listener;

        public PlayerAdapter(List<Player> players, OnInviteClickListener listener) {
            this.players = players;
            this.listener = listener;
        }

        @NonNull
        @Override
        public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.player_item, parent, false);
            return new PlayerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
            holder.nameText.setText(players.get(position).getName());
        }

        @Override
        public int getItemCount() {
            return players.size();
        }

        public class PlayerViewHolder extends RecyclerView.ViewHolder {
            TextView nameText;

            public PlayerViewHolder(@NonNull View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.player_name_text);

                itemView.findViewById(R.id.invite_button).setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onInviteClick(position);
                    }
                });
            }
        }
    }
}