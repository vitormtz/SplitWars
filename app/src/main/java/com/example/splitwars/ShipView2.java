package com.example.splitwars;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class ShipView2 extends View {
    private CamadaNave nave;
    private List<Tiro> tiros;
    private List<Explosao> explosoes;
    private EfeitoPropulsao propulsao;
    private boolean animacaoRodando = false;
    private Paint paintTransparente;
    private long ultimoTempo = 0;
    private SoundPool soundPool;
    private int shotSoundId;
    private boolean soundLoaded = false;
    private ShipEventListener eventListener;

    private static class CamadaNave {
        Bitmap spriteSheet;
        boolean viva = true;
        float posX = 0;
        float posY = 0;
        float speedX = 0;
        float speedY = 0;
        RectF limites = null;

        CamadaNave(Bitmap sprite) {
            this.spriteSheet = sprite;
        }
    }

    private static class Tiro {
        float posX, posY;
        float speedX, speedY;
        boolean ativo = false;
        boolean ehDoJogador = true;
        Bitmap spriteSheet;
        int frameAtual = 0;
        float contadorFrame = 0;
        long tempoVida = 0;
        boolean jaProcessado = false;

        Tiro(Bitmap sprite) {
            this.spriteSheet = sprite;
        }

        void disparar(float startX, float startY, float velX, float velY, boolean doJogador) {
            posX = startX;
            posY = startY;
            speedX = velX;
            speedY = velY;
            ehDoJogador = doJogador;
            ativo = true;
            jaProcessado = false;
            frameAtual = 0;
            contadorFrame = 0;
            tempoVida = System.currentTimeMillis();
        }

        void atualizar(float deltaTime) {
            if (ativo) {
                posX += speedX * deltaTime * 60;
                posY += speedY * deltaTime * 60;

                // Animação do tiro (6 frames)
                contadorFrame += 12.0f * deltaTime;
                while (contadorFrame >= 1.0f) {
                    frameAtual = (frameAtual + 1) % 6;
                    contadorFrame -= 1.0f;
                }

                // Remove tiro após 5 segundos
                if (System.currentTimeMillis() - tempoVida > 5000) {
                    ativo = false;
                }
            }
        }

        void desenhar(Canvas canvas, Paint paint) {
            if (ativo && spriteSheet != null) {
                paint.setAlpha(255);

                int frameLargura = spriteSheet.getWidth() / 6;
                int frameAltura = spriteSheet.getHeight();

                Rect origem = new Rect(
                        frameAtual * frameLargura,
                        0,
                        (frameAtual + 1) * frameLargura,
                        frameAltura
                );

                int larguraFinal = (int) (frameLargura * 0.9f);
                int alturaFinal = (int) (frameAltura * 0.9f);

                float rotacao = ehDoJogador ? 0.0f : 180.0f;
                if (rotacao != 0.0f) {
                    canvas.save();
                    canvas.rotate(rotacao, posX, posY);
                }

                Rect destino = new Rect(
                        (int) (posX - larguraFinal / 2),
                        (int) (posY - alturaFinal / 2),
                        (int) (posX + larguraFinal / 2),
                        (int) (posY + alturaFinal / 2)
                );

                canvas.drawBitmap(spriteSheet, origem, destino, paint);

                if (rotacao != 0.0f) {
                    canvas.restore();
                }
            }
        }

        boolean estaForaDaTela(int larguraTela, int alturaTela) {
            return posX < -50 || posX > larguraTela + 50 || posY < -50 || posY > alturaTela + 50;
        }

        boolean atingiuBordaSuperior() {
            return ehDoJogador && posY <= 0 && !jaProcessado;
        }

        boolean atingiuBordaInferior(int alturaTela) {
            return !ehDoJogador && posY >= alturaTela && !jaProcessado;
        }
    }

    private static class Explosao {
        Bitmap spriteSheet;
        int frameAtual = 0;
        float posX, posY;
        boolean ativa = false;
        float contadorFrame = 0;

        Explosao(Bitmap sprite) {
            this.spriteSheet = sprite;
        }

        void iniciar(float x, float y) {
            posX = x;
            posY = y;
            frameAtual = 0;
            contadorFrame = 0;
            ativa = true;
        }

        void atualizar(float deltaTime) {
            if (ativa) {
                contadorFrame += 15.0f * deltaTime;

                while (contadorFrame >= 1.0f) {
                    frameAtual++;
                    contadorFrame -= 1.0f;

                    if (frameAtual >= 16) { // 16 frames de explosão para nave 2
                        ativa = false;
                        frameAtual = 0;
                        break;
                    }
                }
            }
        }

        void desenhar(Canvas canvas, Paint paint) {
            if (ativa && spriteSheet != null) {
                paint.setAlpha(255);

                int frameLargura = spriteSheet.getWidth() / 16;
                int frameAltura = spriteSheet.getHeight();

                Rect origem = new Rect(
                        frameAtual * frameLargura,
                        0,
                        (frameAtual + 1) * frameLargura,
                        frameAltura
                );

                int larguraFinal = (int) (frameLargura * 1.2f);
                int alturaFinal = (int) (frameAltura * 1.2f);

                Rect destino = new Rect(
                        (int) (posX - larguraFinal / 2),
                        (int) (posY - alturaFinal / 2),
                        (int) (posX + larguraFinal / 2),
                        (int) (posY + alturaFinal / 2)
                );

                canvas.drawBitmap(spriteSheet, origem, destino, paint);
            }
        }
    }

    private static class EfeitoPropulsao {
        Bitmap spriteSheet;
        int frameAtual = 0;
        float contadorFrame = 0;
        boolean ativo = false;

        EfeitoPropulsao(Bitmap sprite) {
            this.spriteSheet = sprite;
        }

        void atualizar(float deltaTime) {
            if (ativo && spriteSheet != null) {
                contadorFrame += 15.0f * deltaTime;

                while (contadorFrame >= 1.0f) {
                    frameAtual = (frameAtual + 1) % 8; // 8 frames de propulsão
                    contadorFrame -= 1.0f;
                }
            }
        }

        void desenhar(Canvas canvas, Paint paint, float naveX, float naveY) {
            if (ativo && spriteSheet != null) {
                paint.setAlpha(225);

                int frameLargura = spriteSheet.getWidth() / 8;
                int frameAltura = spriteSheet.getHeight();

                Rect origem = new Rect(
                        frameAtual * frameLargura,
                        0,
                        (frameAtual + 1) * frameLargura,
                        frameAltura
                );

                int larguraFinal = (int) (frameLargura * 0.55f);
                int alturaFinal = (int) (frameAltura * 0.55f);

                Rect destino = new Rect(
                        (int) (naveX - larguraFinal / 2),
                        (int) (naveY + 35),
                        (int) (naveX + larguraFinal / 2),
                        (int) (naveY + 35 + alturaFinal)
                );

                canvas.drawBitmap(spriteSheet, origem, destino, paint);
                paint.setAlpha(255);
            }
        }
    }

    public interface ShipEventListener {
        void onShipHit();
        void onProjectileFired(float x, float y, float speedX, float speedY);
        void onProjectileHitBorder(float x, float y, float speedX, float speedY);
    }

    public ShipView2(Context context) {
        super(context);
        inicializar();
    }

    public ShipView2(Context context, AttributeSet attrs) {
        super(context, attrs);
        inicializar();
    }

    public ShipView2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inicializar();
    }

    private void inicializar() {
        paintTransparente = new Paint();
        paintTransparente.setAntiAlias(true);

        tiros = new ArrayList<>();
        explosoes = new ArrayList<>();

        criarNave();
        criarTiros();
        criarExplosoes();
        criarPropulsao();
        initializeSound();
    }

    private void initializeSound() {
        try {
            soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
            soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    soundLoaded = (status == 0);
                }
            });
            shotSoundId = soundPool.load(getContext(), R.raw.laser_shot, 1);
        } catch (Exception e) {
            soundLoaded = false;
        }
    }

    private void playShotSound() {
        if (soundLoaded && soundPool != null) {
            soundPool.play(shotSoundId, 0.7f, 0.7f, 1, 0, 1.0f);
        }
    }

    public void releaseSound() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    private void criarNave() {
        try {
            Bitmap sprite = BitmapFactory.decodeResource(getResources(), R.drawable.spr_ship2);
            if (sprite == null) {
                sprite = BitmapFactory.decodeResource(getResources(), R.drawable.spr_ship);
            }
            nave = new CamadaNave(sprite);
        } catch (Exception e) {
            nave = new CamadaNave(null);
        }
    }

    private void criarTiros() {
        try {
            Bitmap tiroSprite = BitmapFactory.decodeResource(getResources(), R.drawable.spr_bullet2);
            if (tiroSprite == null) {
                tiroSprite = BitmapFactory.decodeResource(getResources(), R.drawable.spr_bullet);
            }
            for (int i = 0; i < 12; i++) {
                tiros.add(new Tiro(tiroSprite));
            }
        } catch (Exception e) {
            for (int i = 0; i < 12; i++) {
                tiros.add(new Tiro(null));
            }
        }
    }

    private void criarExplosoes() {
        try {
            Bitmap explosaoSprite = BitmapFactory.decodeResource(getResources(), R.drawable.spr_explosion2);
            if (explosaoSprite == null) {
                explosaoSprite = BitmapFactory.decodeResource(getResources(), R.drawable.spr_explosion);
            }
            explosoes.add(new Explosao(explosaoSprite));
        } catch (Exception e) {
            explosoes.add(new Explosao(null));
        }
    }

    private void criarPropulsao() {
        try {
            Bitmap propulsaoSprite = BitmapFactory.decodeResource(getResources(), R.drawable.spr_thruster2);
            if (propulsaoSprite == null) {
                propulsaoSprite = BitmapFactory.decodeResource(getResources(), R.drawable.spr_thruster);
            }
            propulsao = new EfeitoPropulsao(propulsaoSprite);
        } catch (Exception e) {
            propulsao = new EfeitoPropulsao(null);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (propulsao != null && nave != null) {
            propulsao.desenhar(canvas, paintTransparente, nave.posX, nave.posY - 165);
        }

        if (nave != null) {
            desenharNave(canvas);
        }

        for (Tiro tiro : tiros) {
            if (tiro.ativo) {
                tiro.desenhar(canvas, paintTransparente);
            }
        }

        for (Explosao explosao : explosoes) {
            if (explosao.ativa) {
                explosao.desenhar(canvas, paintTransparente);
            }
        }
    }

    private void desenharNave(Canvas canvas) {
        if (nave == null || !nave.viva || nave.spriteSheet == null) {
            return;
        }

        paintTransparente.setAlpha(255);

        int largura = nave.spriteSheet.getWidth();
        int altura = nave.spriteSheet.getHeight();
        int posXFinal = (int) nave.posX;
        int posYFinal = (int) nave.posY;

        // Centraliza a nave se ainda não foi posicionada
        if ((nave.posX == 0 && nave.posY == 0) ||
                (getWidth() > 0 && getHeight() > 0 &&
                        (nave.posX < largura/2 || nave.posY < altura/2))) {
            posXFinal = getWidth() / 2;
            posYFinal = getHeight() / 2;
            nave.posX = posXFinal;
            nave.posY = posYFinal;
        }

        Rect destino = new Rect(
                posXFinal - largura / 2,
                posYFinal - altura / 2,
                posXFinal + largura / 2,
                posYFinal + altura / 2
        );

        canvas.drawBitmap(nave.spriteSheet, null, destino, paintTransparente);
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

                atualizarNave(deltaTime);
                atualizarTiros(deltaTime);
                atualizarExplosoes(deltaTime);
                atualizarPropulsao(deltaTime);

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

    private void atualizarNave(float deltaTime) {
        if (nave != null) {
            nave.posX += nave.speedX * deltaTime;
            nave.posY += nave.speedY * deltaTime;

            int larguraTela = getWidth();
            int alturaTela = getHeight();

            if (larguraTela > 0 && alturaTela > 0) {
                int larguraNave = nave.spriteSheet != null ? nave.spriteSheet.getWidth() : 85;
                int alturaNave = nave.spriteSheet != null ? nave.spriteSheet.getHeight() : 45;

                if (nave.limites != null) {
                    if (nave.posX - larguraNave / 2 < nave.limites.left) nave.posX = nave.limites.left + larguraNave / 2;
                    if (nave.posX + larguraNave / 2 > nave.limites.right) nave.posX = nave.limites.right - larguraNave / 2;
                    if (nave.posY - alturaNave / 2 < nave.limites.top) nave.posY = nave.limites.top + alturaNave / 2;
                    if (nave.posY + alturaNave / 2 > nave.limites.bottom) nave.posY = nave.limites.bottom - alturaNave / 2;
                } else {
                    if (nave.posX - larguraNave / 2 < 0) nave.posX = larguraNave / 2;
                    if (nave.posX + larguraNave / 2 > larguraTela) nave.posX = larguraTela - larguraNave / 2;
                    if (nave.posY - alturaNave / 2 < 0) nave.posY = alturaNave / 2;
                    if (nave.posY + alturaNave / 2 > alturaTela) nave.posY = alturaTela - alturaNave / 2;
                }
            }
        }
    }

    private void atualizarTiros(float deltaTime) {
        for (Tiro tiro : tiros) {
            if (tiro.ativo) {
                tiro.atualizar(deltaTime);

                if (tiro.atingiuBordaSuperior()) {
                    tiro.jaProcessado = true;
                    if (eventListener != null) {
                        eventListener.onProjectileHitBorder(tiro.posX, tiro.posY, tiro.speedX, tiro.speedY);
                    }
                    tiro.ativo = false;
                } else if (tiro.atingiuBordaInferior(getHeight())) {
                    tiro.jaProcessado = true;
                    tiro.ativo = false;
                } else if (tiro.estaForaDaTela(getWidth(), getHeight())) {
                    tiro.ativo = false;
                }

                if (tiro.ativo && !tiro.ehDoJogador && nave != null && nave.viva) {
                    if (verificarColisaoTiroNave(tiro)) {
                        tiro.ativo = false;
                        criarExplosao(nave.posX, nave.posY);
                        if (eventListener != null) {
                            eventListener.onShipHit();
                        }
                    }
                }
            }
        }
    }

    private boolean verificarColisaoTiroNave(Tiro tiro) {
        if (nave == null || !nave.viva) return false;

        int larguraNave = nave.spriteSheet != null ? nave.spriteSheet.getWidth() : 85;
        int alturaNave = nave.spriteSheet != null ? nave.spriteSheet.getHeight() : 45;

        // Área de colisão reduzida para ser mais justa (45% do tamanho original)
        RectF naveBounds = new RectF(
                nave.posX - larguraNave * 0.225f,
                nave.posY - alturaNave * 0.225f,
                nave.posX + larguraNave * 0.225f,
                nave.posY + alturaNave * 0.225f
        );

        RectF tiroBounds = new RectF(
                tiro.posX - 12,
                tiro.posY - 12,
                tiro.posX + 12,
                tiro.posY + 12
        );

        if (RectF.intersects(naveBounds, tiroBounds)) {
            nave.viva = false;
            return true;
        }

        return false;
    }

    private void atualizarExplosoes(float deltaTime) {
        for (Explosao explosao : explosoes) {
            if (explosao.ativa) {
                explosao.atualizar(deltaTime);
            }
        }
    }

    private void atualizarPropulsao(float deltaTime) {
        if (propulsao != null) {
            if (nave != null && nave.viva) {
                boolean movimento = Math.abs(nave.speedX) > 1.0f || Math.abs(nave.speedY) > 1.0f;
                propulsao.ativo = movimento;
            } else {
                propulsao.ativo = false;
            }
            propulsao.atualizar(deltaTime);
        }
    }

    public void setVelocidadeNave(float speedX, float speedY) {
        if (nave != null) {
            nave.speedX = speedX;
            nave.speedY = speedY;
        }
    }

    public void setLimitesNave(RectF limites) {
        if (nave != null) {
            nave.limites = limites;
        }
    }

    public void disparar() {
        if (nave == null) return;

        playShotSound();

        for (Tiro tiro : tiros) {
            if (!tiro.ativo) {
                tiro.disparar(nave.posX, nave.posY, 0, -8, true);
                if (eventListener != null) {
                    eventListener.onProjectileFired(tiro.posX, tiro.posY, tiro.speedX, tiro.speedY);
                }
                break;
            }
        }
    }

    public void criarExplosao(float x, float y) {
        for (Explosao explosao : explosoes) {
            if (!explosao.ativa) {
                explosao.iniciar(x, y);
                break;
            }
        }
    }

    public void receberTiroInimigo(float relativeX, float speedX, float speedY) {
        float x = relativeX * getWidth();

        for (Tiro tiro : tiros) {
            if (!tiro.ativo) {
                tiro.disparar(x, 0, speedX, Math.abs(speedY), false);
                break;
            }
        }
    }

    public boolean isNaveViva() {
        return nave != null && nave.viva;
    }

    public void resetarNave() {
        if (nave != null) {
            nave.viva = true;
            if (propulsao != null) {
                propulsao.ativo = false;
            }
        }
    }

    public void setEventListener(ShipEventListener listener) {
        this.eventListener = listener;
    }

    public void centralizarNave() {
        if (nave != null) {
            int largura = getWidth();
            int altura = getHeight();

            if (largura == 0 || altura == 0) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        centralizarNave();
                    }
                });
                return;
            }

            nave.posX = largura / 2f;
            nave.posY = altura / 2f;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (nave != null && w > 0 && h > 0) {
            nave.posX = w / 2f;
            nave.posY = h / 2f;
        }
    }
}