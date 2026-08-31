package com.example.splitwars;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class MoonView extends View {

    private static class CamadaLua {
        Bitmap spriteSheet;
        int frameAtual = 0;
        int frameLargura;
        int frameAltura;
        float contadorFrame = 0;

        CamadaLua(Bitmap sprite) {
            this.spriteSheet = sprite;
            if (sprite != null) {
                this.frameLargura = sprite.getWidth() / 60; // 60 frames horizontais
                this.frameAltura = sprite.getHeight();
            }
        }
    }

    private CamadaLua lua;
    private boolean animacaoRodando = false;
    private Paint paintTransparente;
    private long ultimoTempo = 0;

    public MoonView(Context context) {
        super(context);
        inicializar();
    }

    public MoonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inicializar();
    }

    public MoonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inicializar();
    }

    private void inicializar() {
        paintTransparente = new Paint();
        paintTransparente.setAntiAlias(true);
        criarLua();
    }

    private void criarLua() {
        try {
            Bitmap sprite = BitmapFactory.decodeResource(getResources(), R.drawable.spr_moon);
            lua = new CamadaLua(sprite);
        } catch (Exception e) {
            lua = new CamadaLua(null);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (lua != null && lua.spriteSheet != null) {
            desenharLua(canvas);
        }
    }

    private void desenharLua(Canvas canvas) {
        paintTransparente.setAlpha(180); // Transparência fixa usada no GameActivity

        Rect origem = new Rect(
                lua.frameAtual * lua.frameLargura,
                0,
                (lua.frameAtual + 1) * lua.frameLargura,
                lua.frameAltura
        );

        // Tamanho final com escala de 0.25f
        int larguraFinal = (int) (lua.frameLargura * 0.25f);
        int alturaFinal = (int) (lua.frameAltura * 0.25f);

        // Sempre centralizado
        int posX = (getWidth() - larguraFinal) / 2;
        int posY = (getHeight() - alturaFinal) / 2;

        Rect destino = new Rect(
                posX,
                posY,
                posX + larguraFinal,
                posY + alturaFinal
        );

        canvas.drawBitmap(lua.spriteSheet, origem, destino, paintTransparente);
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

                if (lua != null && lua.spriteSheet != null) {
                    atualizarLua(deltaTime);
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

    private void atualizarLua(float deltaTime) {
        float velocidade = 2.0f; // Velocidade fixa usada no GameActivity
        lua.contadorFrame += velocidade * deltaTime;

        while (lua.contadorFrame >= 1.0f) {
            lua.frameAtual = (lua.frameAtual + 1) % 60; // 60 frames total
            lua.contadorFrame -= 1.0f;
        }
    }
}