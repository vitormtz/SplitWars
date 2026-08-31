package com.example.splitwars;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class GameActivity extends AppCompatActivity implements SensorEventListener {
    private GameView gameView;
    private ShipView shipView;
    private ShipView2 shipView2;
    private View currentShipView;
    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private NetworkManager networkManager;
    private MediaPlayer backgroundMusic;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isConnectionCheckerRunning = false;
    private boolean wasConnectedBefore = false;
    private int selectedShipType = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Força o modo claro e mantém a tela sempre ligada
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_game);

        // Recebe os dados passados pela tela anterior
        boolean isHost = getIntent().getBooleanExtra("IS_HOST", false);
        String opponentIp = getIntent().getStringExtra("OPPONENT_IP");
        selectedShipType = getIntent().getIntExtra("SELECTED_SHIP_TYPE", 1);

        // Configura qual nave será usada e prepara os sensores
        initializeSelectedShip();
        initializeSensor();

        // Inicializa a conexão de rede e a tela do jogo
        networkManager = new NetworkManager(this);
        gameView = findViewById(R.id.game_view);
        gameView.initialize(networkManager, isHost, currentShipView);

        // Configura os eventos da nave
        setupShipEventListener();
        startConnectionChecker();

        // Conecta com o outro jogador baseado em quem é o host
        if (isHost) {
            // Cria o jogo e espera conexão
            networkManager.createGame();
        } else {
            // Conecta no IP do host
            networkManager.connectToHost(opponentIp);
        }

        // Prepara a música de fundo com volume baixo
        backgroundMusic = MediaPlayer.create(this, R.raw.background_music);
        if (backgroundMusic != null) {
            backgroundMusic.setLooping(true);
            backgroundMusic.setVolume(0.3f, 0.3f);
        }

        // Envia o tamanho da tela para o outro jogador
        handler.postDelayed(() -> {
            if (networkManager.isConnected()) {
                networkManager.sendScreenSize(gameView.getWidth(), gameView.getHeight());
            }
        }, 2000);
    }

    private void initializeSelectedShip() {
        // Encontra as duas naves disponíveis no layout
        shipView = findViewById(R.id.ship_view);
        shipView2 = findViewById(R.id.ship_view2);

        // Escolhe qual nave mostrar baseado na seleção do jogador
        if (selectedShipType == 1) {
            currentShipView = shipView;
            shipView.setVisibility(View.VISIBLE);
            shipView2.setVisibility(View.GONE);
        } else {
            currentShipView = shipView2;
            shipView2.setVisibility(View.VISIBLE);
            shipView.setVisibility(View.GONE);
        }
    }

    private void initializeSensor() {
        // Obtém o gerenciador de sensores do sistema Android
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Tenta usar o giroscópio primeiro (sensor mais preciso para rotação)
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Se o celular não tem giroscópio, tenta usar o acelerômetro como alternativa
        if (gyroscopeSensor == null) {
            gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            // Se não tem nem giroscópio nem acelerômetro, fecha o jogo
            if (gyroscopeSensor == null) {
                finish();
            }
        }
    }

    private void setupShipEventListener() {
        // Cria um listener de toque na tela
        View.OnTouchListener touchListener = (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // Só quando pressionar (não soltar)
                if (selectedShipType == 1) {
                    shipView.disparar();
                } else {
                    shipView2.disparar();
                }
                return true;
            }
            return false;
        };

        if (selectedShipType == 1) {
            // Define o que acontece quando algo ocorre com a nave 1
            shipView.setEventListener(new ShipView.ShipEventListener() {
                @Override
                public void onShipHit() {
                    // Nave foi atingida - processa o hit
                    gameView.onShipHit();
                }
                @Override
                public void onProjectileFired(float x, float y, float speedX, float speedY) {
                    // Tiro foi disparado
                    gameView.onProjectileFired(x, y, speedX, speedY);
                }
                @Override
                public void onProjectileHitBorder(float x, float y, float speedX, float speedY) {
                    // Tiro saiu da tela
                    gameView.onProjectileHitBorder(x, y, speedX, speedY);
                }
            });
            // Ativa o toque para disparar
            shipView.setOnTouchListener(touchListener);
        } else {
            // Mesma configuração, mas para a nave 2
            shipView2.setEventListener(new ShipView2.ShipEventListener() {
                @Override
                public void onShipHit() {
                    // Nave foi atingida
                    gameView.onShipHit();
                }
                @Override
                public void onProjectileFired(float x, float y, float speedX, float speedY) {
                    // Tiro disparado
                    gameView.onProjectileFired(x, y, speedX, speedY);
                }
                @Override
                public void onProjectileHitBorder(float x, float y, float speedX, float speedY) {
                    // Tiro saiu da tela
                    gameView.onProjectileHitBorder(x, y, speedX, speedY);
                }
            });
            // Ativa o toque para disparar
            shipView2.setOnTouchListener(touchListener);
        }
    }

    private void startConnectionChecker() {
        // Se já está verificando a conexão, não inicia outro verificador
        if (isConnectionCheckerRunning) return;

        isConnectionCheckerRunning = true;

        // Cria uma thread separada para não travar a tela do jogo
        new Thread(() -> {
            // Fica verificando enquanto o jogo estiver rodando
            while (isConnectionCheckerRunning && !isFinishing()) {
                try {
                    // Verifica se ainda está conectado com o outro jogador
                    boolean connected = networkManager.isConnected();

                    // Se conectou pela primeira vez, marca que já esteve conectado
                    if (connected && !wasConnectedBefore) {
                        wasConnectedBefore = true;
                    }

                    // Se já esteve conectado antes mas agora perdeu a conexão
                    if (wasConnectedBefore && !connected) {
                        // Volta para a thread principal e fecha o jogo
                        handler.post(() -> {
                            // Sai do jogo
                            if (!isFinishing()) finish();
                        });
                        break;
                    }

                    // Espera 3 segundos antes de verificar novamente
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reativa o sensor de movimento (giroscópio/acelerômetro) com velocidade de jogo
        sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);

        // Reinicia todas as animações do jogo
        gameView.resume();
        ((SpaceView) findViewById(R.id.space_view)).iniciarAnimacao();
        ((PlanetView) findViewById(R.id.planet_view)).iniciarAnimacao();
        ((MoonView) findViewById(R.id.moon_view)).iniciarAnimacao();

        // Inicia a animação da nave escolhida pelo jogador
        if (selectedShipType == 1) {
            shipView.iniciarAnimacao();
        } else {
            shipView2.iniciarAnimacao();
        }

        // Retoma a música de fundo se ela não estiver tocando
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Para de escutar o sensor de movimento
        sensorManager.unregisterListener(this);

        // Pausa todas as animações do jogo
        gameView.pause();
        ((SpaceView) findViewById(R.id.space_view)).pararAnimacao();
        ((PlanetView) findViewById(R.id.planet_view)).pararAnimacao();
        ((MoonView) findViewById(R.id.moon_view)).pararAnimacao();

        // Para a animação da nave que está sendo usada
        if (selectedShipType == 1) {
            shipView.pararAnimacao();
        } else {
            shipView2.pararAnimacao();
        }

        // Pausa a música de fundo se ela estiver tocando
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Para o verificador de conexão
        isConnectionCheckerRunning = false;

        // Libera os recursos de som das duas naves
        shipView.releaseSound();
        shipView2.releaseSound();

        // Fecha a conexão de rede com o outro jogador
        if (networkManager != null) {
            networkManager.closeConnection();
        }

        // Para e libera completamente a música de fundo
        if (backgroundMusic != null) {
            if (backgroundMusic.isPlaying()) {
                backgroundMusic.stop();
            }
            backgroundMusic.release();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float speedX, speedY;

        // Verifica qual tipo de sensor está sendo usado e ajusta os valores
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            // Giroscópio: mais preciso para detectar rotação
            speedX = event.values[0] * 1000.0f;
            speedY = -event.values[1] * 550.0f;
        } else {
            // Acelerômetro: alternativa para celulares sem giroscópio
            speedX = -event.values[1] * 1000.0f;
            speedY = event.values[0] * 550.0f;
        }

        // Aplica a velocidade calculada na nave que está sendo usada
        if (selectedShipType == 1) {
            shipView.setVelocidadeNave(speedX, speedY);
        } else {
            shipView2.setVelocidadeNave(speedX, speedY);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void mostrarGameOver(String mensagem, boolean ganhou) {
        // Executa na thread principal
        runOnUiThread(() -> {
            // Para a música de fundo e volta para o início
            if (backgroundMusic != null && backgroundMusic.isPlaying()) {
                backgroundMusic.pause();
                backgroundMusic.seekTo(0);
            }

            // Para de escutar o sensor de movimento
            sensorManager.unregisterListener(GameActivity.this);

            // Cria e mostra a tela de fim de jogo
            new AlertDialog.Builder(GameActivity.this)
                    .setTitle(ganhou ? " VITÓRIA " : " DERROTA ")
                    .setMessage(mensagem)
                    .setCancelable(false)
                    .setPositiveButton("Jogar Novamente", (dialog, id) -> reiniciarJogo())
                    .setNegativeButton("Voltar ao Menu", (dialog, id) -> finish())
                    .show();
        });
    }

    private void reiniciarJogo() {
        // Reinicia o jogo
        gameView.reiniciarJogo();

        // Reativa todas as animações do cenário
        ((SpaceView) findViewById(R.id.space_view)).iniciarAnimacao();
        ((PlanetView) findViewById(R.id.planet_view)).iniciarAnimacao();
        ((MoonView) findViewById(R.id.moon_view)).iniciarAnimacao();

        // Reinicia a animação da nave escolhida pelo jogador
        if (selectedShipType == 1) {
            shipView.iniciarAnimacao();
        } else {
            shipView2.iniciarAnimacao();
        }

        // Retoma a música de fundo do início
        if (backgroundMusic != null) {
            backgroundMusic.start();
        }

        // Reativa o controle por movimento
        sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
    }
}