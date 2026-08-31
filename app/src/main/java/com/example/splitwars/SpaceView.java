package com.example.splitwars;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class SpaceView extends View {

    private static class CamadaPoeira {
        Bitmap spriteSheet;
        int frameAtual = 0;
        int frameLargura;
        int frameAltura;
        float contadorFrame = 0;

        CamadaPoeira(Bitmap sprite) {
            this.spriteSheet = sprite;
            if (sprite != null) {
                this.frameLargura = sprite.getWidth() / 9; // 9 frames horizontais
                this.frameAltura = sprite.getHeight();
            }
        }
    }

    private CamadaPoeira poeira;
    private boolean animacaoRodando = false;
    private Paint paintTransparente;
    private long ultimoTempo = 0;

    public SpaceView(Context context) {
        super(context);
        inicializar();
    }

    public SpaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inicializar();
    }

    private void inicializar() {
        paintTransparente = new Paint();
        criarPoeira();
    }

    private void criarPoeira() {
        try {
            Bitmap sprite = BitmapFactory.decodeResource(getResources(), R.drawable.spr_space);
            poeira = new CamadaPoeira(sprite);
        } catch (Exception e) {
            poeira = new CamadaPoeira(null);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Fundo azul escuro do espaço
        canvas.drawARGB(255, 0, 0, 30);

        if (poeira != null && poeira.spriteSheet != null) {
            desenharPoeira(canvas);
        }
    }

    private void desenharPoeira(Canvas canvas) {
        paintTransparente.setAlpha(255); // Opacidade total

        Rect origem = new Rect(
                poeira.frameAtual * poeira.frameLargura,
                0,
                (poeira.frameAtual + 1) * poeira.frameLargura,
                poeira.frameAltura
        );

        // Tamanho final com escala de 1.10f
        int larguraFinal = (int) (poeira.frameLargura * 1.10f);
        int alturaFinal = (int) (poeira.frameAltura * 1.10f);

        // Sempre centralizado
        int posX = (getWidth() - larguraFinal) / 2;
        int posY = (getHeight() - alturaFinal) / 2;

        Rect destino = new Rect(
                posX,
                posY,
                posX + larguraFinal,
                posY + alturaFinal
        );

        canvas.drawBitmap(poeira.spriteSheet, origem, destino, paintTransparente);
    }

    public void iniciarAnimacao() {
        if (!animacaoRodando) {
            animacaoRodando = true;
            ultimoTempo = System.currentTimeMillis();
            new Thread(new AnimacaoThread()).start();
        }
    }

    public void pararAnimacao() {
        animacaoRodando = false;
    }

    private class AnimacaoThread implements Runnable {
        @Override
        public void run() {
            while (animacaoRodando) {
                long tempoAtual = System.currentTimeMillis();
                float deltaTime = (tempoAtual - ultimoTempo) / 1000.0f;
                ultimoTempo = tempoAtual;

                if (poeira != null && poeira.spriteSheet != null) {
                    atualizarPoeira(deltaTime);
                }

                post(new Runnable() {
                    @Override
                    public void run() {
                        invalidate();
                    }
                });

                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    private void atualizarPoeira(float deltaTime) {
        float velocidade = 2.5f; // Velocidade fixa
        poeira.contadorFrame += velocidade * deltaTime;

        while (poeira.contadorFrame >= 1.0f) {
            poeira.frameAtual = (poeira.frameAtual + 1) % 9; // 9 frames total
            poeira.contadorFrame -= 1.0f;
        }
    }
}