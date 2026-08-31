package com.example.splitwars;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class PlanetView extends View {

    private static class CamadaPlaneta {
        Bitmap spriteSheet;
        int frameAtual = 0;
        int frameLargura;
        int frameAltura;
        float contadorFrame = 0;

        CamadaPlaneta(Bitmap sprite) {
            this.spriteSheet = sprite;
            if (sprite != null) {
                this.frameLargura = sprite.getWidth() / 60; // 60 frames horizontais
                this.frameAltura = sprite.getHeight();
            }
        }
    }

    private CamadaPlaneta planeta;
    private boolean animacaoRodando = false;
    private Paint paintTransparente;
    private long ultimoTempo = 0;

    public PlanetView(Context context) {
        super(context);
        inicializar();
    }

    public PlanetView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inicializar();
    }

    private void inicializar() {
        paintTransparente = new Paint();
        criarPlaneta();
    }

    private void criarPlaneta() {
        try {
            Bitmap sprite = BitmapFactory.decodeResource(getResources(), R.drawable.spr_planet);
            planeta = new CamadaPlaneta(sprite);
        } catch (Exception e) {
            planeta = new CamadaPlaneta(null);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (planeta != null && planeta.spriteSheet != null) {
            desenharPlaneta(canvas);
        }
    }

    private void desenharPlaneta(Canvas canvas) {
        paintTransparente.setAlpha(255); // Opacidade total

        Rect origem = new Rect(
                planeta.frameAtual * planeta.frameLargura,
                0,
                (planeta.frameAtual + 1) * planeta.frameLargura,
                planeta.frameAltura
        );

        // Tamanho final com escala de 0.5f
        int larguraFinal = (int) (planeta.frameLargura * 0.5f);
        int alturaFinal = (int) (planeta.frameAltura * 0.5f);

        // Sempre centralizado
        int posX = (getWidth() - larguraFinal) / 2;
        int posY = (getHeight() - alturaFinal) / 2;

        Rect destino = new Rect(
                posX,
                posY,
                posX + larguraFinal,
                posY + alturaFinal
        );

        canvas.drawBitmap(planeta.spriteSheet, origem, destino, paintTransparente);
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

                if (planeta != null && planeta.spriteSheet != null) {
                    atualizarPlaneta(deltaTime);
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

    private void atualizarPlaneta(float deltaTime) {
        float velocidade = 4.5f; // Velocidade fixa
        planeta.contadorFrame += velocidade * deltaTime;

        while (planeta.contadorFrame >= 1.0f) {
            planeta.frameAtual = (planeta.frameAtual + 1) % 60; // 60 frames total
            planeta.contadorFrame -= 1.0f;
        }
    }
}