package com.example.splitwars;

import android.content.Context;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class GameView extends View {
    private RectF playableRect = new RectF();
    private int playableWidth;
    private int playableHeight;
    private int offsetX = 0;
    private int offsetY = 0;
    private NetworkManager networkManager;
    private View currentShipView;
    private int shipType = 1;
    private int screenWidth;
    private int screenHeight;
    private boolean pausarJogo = false;
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    public GameView(Context context) {
        super(context);
    }

    public GameView(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize(NetworkManager networkManager, boolean isHost, View shipView) {
        this.networkManager = networkManager;
        this.currentShipView = shipView;
        this.shipType = (shipView instanceof ShipView) ? 1 : 2;

        post(() -> {
            if (getWidth() > 0 && getHeight() > 0) {
                centralizarNave();
                updatePlayableRect();
            }
        });
    }

    private void centralizarNave() {
        if (currentShipView instanceof ShipView) {
            ((ShipView) currentShipView).centralizarNave();
        } else {
            ((ShipView2) currentShipView).centralizarNave();
        }
    }

    private void setLimitesNave(RectF limites) {
        if (currentShipView instanceof ShipView) {
            ((ShipView) currentShipView).setLimitesNave(limites);
        } else {
            ((ShipView2) currentShipView).setLimitesNave(limites);
        }
    }

    private void resetarNave() {
        if (currentShipView instanceof ShipView) {
            ((ShipView) currentShipView).resetarNave();
        } else {
            ((ShipView2) currentShipView).resetarNave();
        }
    }

    private void iniciarAnimacaoNave() {
        if (currentShipView instanceof ShipView) {
            ((ShipView) currentShipView).iniciarAnimacao();
        } else {
            ((ShipView2) currentShipView).iniciarAnimacao();
        }
    }

    private void pararAnimacaoNave() {
        if (currentShipView instanceof ShipView) {
            ((ShipView) currentShipView).pararAnimacao();
        } else {
            ((ShipView2) currentShipView).pararAnimacao();
        }
    }

    private void receberTiroInimigo(float relativeX, float speedX, float speedY) {
        if (currentShipView instanceof ShipView) {
            ((ShipView) currentShipView).receberTiroInimigo(relativeX, speedX, speedY);
        } else {
            ((ShipView2) currentShipView).receberTiroInimigo(relativeX, speedX, speedY);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;

        post(() -> {
            centralizarNave();
            updatePlayableRect();
        });
    }

    public void resume() {
        pausarJogo = false;
        new Thread(() -> {
            while (!pausarJogo) {
                if (networkManager != null) {
                    processNetworkMessages();
                }
                try {
                    Thread.sleep(17);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    public void pause() {
        pausarJogo = true;
    }

    public void reiniciarJogo() {
        pausarJogo = false;
        resetarNave();
        iniciarAnimacaoNave();
        resume(); // CORREÇÃO: Reativar o processamento de mensagens de rede
    }

    public void onShipHit() {
        uiHandler.postDelayed(() -> {
            pausarJogo = true;
            pararAnimacaoNave();

            if (getContext() instanceof GameActivity) {
                ((GameActivity) getContext()).mostrarGameOver("VOCÊ PERDEU!", false);
            }
        }, 2500);

        if (networkManager != null && networkManager.isConnected()) {
            networkManager.sendHit();
        }
    }

    public void onProjectileFired(float x, float y, float speedX, float speedY) {
        // Método mantido para compatibilidade com ShipView listeners
    }

    public void onProjectileHitBorder(float x, float y, float speedX, float speedY) {
        if (networkManager != null && networkManager.isConnected()) {
            float relativeX = x / screenWidth;
            networkManager.sendProjectile(relativeX, speedX, Math.abs(speedY));
        }
    }

    private void processNetworkMessages() {
        NetworkMessage message = networkManager.receiveMessage();
        if (message == null) return;

        switch (message.getType()) {
            case NetworkMessage.PROJECTILE:
                receberTiroInimigo(message.getRelativeX(), message.getSpeedX(), message.getSpeedY());
                break;

            case NetworkMessage.HIT:
                uiHandler.postDelayed(() -> {
                    pausarJogo = true;
                    pararAnimacaoNave();

                    if (getContext() instanceof GameActivity) {
                        ((GameActivity) getContext()).mostrarGameOver("VOCÊ GANHOU!", true);
                    }
                }, 2000);
                break;

            case NetworkMessage.DISCONNECT:
                uiHandler.post(() ->
                        Toast.makeText(getContext(), "Oponente desconectou!", Toast.LENGTH_LONG).show()
                );
                break;

            case NetworkMessage.SCREEN_SIZE:
                adjustPlayableArea(message.getScreenWidth(), message.getScreenHeight());
                break;
        }
    }

    private void adjustPlayableArea(int opponentWidth, int opponentHeight) {
        if (screenWidth == opponentWidth && screenHeight == opponentHeight) {
            playableWidth = screenWidth;
            playableHeight = screenHeight;
            offsetX = offsetY = 0;
            updatePlayableRect();
            return;
        }

        float myAspectRatio = (float) screenWidth / screenHeight;
        float opponentAspectRatio = (float) opponentWidth / opponentHeight;

        if (myAspectRatio > opponentAspectRatio) {
            float scaleRatio = (float) screenHeight / opponentHeight;
            playableWidth = (int) (opponentWidth * scaleRatio);
            playableHeight = screenHeight;
            offsetX = (screenWidth - playableWidth) / 2;
            offsetY = 0;
        } else {
            float scaleRatio = (float) screenWidth / opponentWidth;
            playableWidth = screenWidth;
            playableHeight = (int) (opponentHeight * scaleRatio);
            offsetX = 0;
            offsetY = (screenHeight - playableHeight) / 2;
        }

        playableWidth = Math.min(playableWidth, screenWidth);
        playableHeight = Math.min(playableHeight, screenHeight);
        updatePlayableRect();
    }

    private void updatePlayableRect() {
        playableRect.set(offsetX, offsetY, offsetX + playableWidth, offsetY + playableHeight);
        setLimitesNave(playableRect);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (currentShipView instanceof ShipView) {
                ((ShipView) currentShipView).disparar();
            } else {
                ((ShipView2) currentShipView).disparar();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }
}