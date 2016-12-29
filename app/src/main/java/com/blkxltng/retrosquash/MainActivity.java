package com.blkxltng.retrosquash;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    Canvas mCanvas;
    SquashCourtView mSquashCourtView;

    //Sound
    private SoundPool mSoundPool;
    int sample1 = -1;
    int sample2 = -1;
    int sample3 = -1;
    int sample4 = -1;

    //Variables for screen details
    Display mDisplay;
    Point size;
    int screenWidth;
    int screenHeight;

    //Objects for game
    int racketWidth;
    int racketHeight;
    Point racketPosition;

    int ballWidth;
    Point ballPosition;

    //Movement of ball
    boolean ballIsMovingLeft;
    boolean ballIsMovingRight;
    boolean ballIsMovingUp;
    boolean ballIsMovingDown;

    //Movement of racket
    boolean racketIsMovingLeft;
    boolean racketIsMovingRight;

    //Statistics
    long lastFrameTime;
    int fps;
    int score;
    int lives;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            mSquashCourtView.pause();
            finish();
            return true;
        }
        return false;
        //return super.onKeyDown(keyCode, event);
    }

    /**
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are <em>not</em> resumed.  This means
     * that in some cases the previous state may still be saved, not allowing
     * fragment transactions that modify the state.  To correctly interact
     * with fragments in their proper state, you should instead override
     * {@link #onResumeFragments()}.
     */
    @Override
    protected void onResume() {
        super.onResume();
        mSquashCourtView.resume();
    }

    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mSquashCourtView.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        while(true) {
            mSquashCourtView.pause();
            break;
        }
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSquashCourtView = new SquashCourtView(this);
        setContentView(mSquashCourtView);

        //Code for sound
        mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        try {
            AssetManager assetManager = getAssets();
            AssetFileDescriptor descriptor;

            //load sounds into memory
            descriptor = assetManager.openFd("sample1.ogg");
            sample1 = mSoundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("sample2.ogg");
            sample2 = mSoundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("sample3.ogg");
            sample3 = mSoundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("sample4.ogg");
            sample4 = mSoundPool.load(descriptor, 0);

        } catch(IOException e) {
            //Catch exceptions
        }

        //Get the screen size
        mDisplay = getWindowManager().getDefaultDisplay();
        size = new Point();
        mDisplay.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        //Game objects
        racketPosition = new Point();
        racketPosition.x = screenWidth / 2;
        racketPosition.y = screenHeight - 20;
        racketWidth = screenWidth / 8;
        racketHeight = 10;

        ballWidth = screenWidth / 35;
        ballPosition = new Point();
        ballPosition.x = screenWidth / 2;
        ballPosition.y = ballWidth + 1;

        lives = 3;
    }

    class SquashCourtView extends SurfaceView implements Runnable {

        Thread mThread = null;
        SurfaceHolder mHolder;
        volatile boolean playingSquash;
        Paint mPaint;

        public SquashCourtView(Context context) {
            super(context);
            mHolder = getHolder();
            mPaint = new Paint();
            ballIsMovingDown = true;

            //Make the ball move in a random direction
            Random random = new Random();
            int ballDirection = random.nextInt(3);
            switch(ballDirection) {
                case 0:
                    ballIsMovingLeft = true;
                    ballIsMovingRight = false;
                    break;

                case 1:
                    ballIsMovingLeft = false;
                    ballIsMovingRight = true;
                    break;

                case 2:
                    ballIsMovingLeft = false;
                    ballIsMovingRight = false;
                    break;
            }
        }

        @Override
        public void run() {
            while(playingSquash) {
                updateCourt();
                drawCourt();
                controlFPS();
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            switch (event.getAction() & MotionEvent.ACTION_MASK) {

                case MotionEvent.ACTION_DOWN:
                    if(event.getX() >= screenWidth / 2) {
                        racketIsMovingRight = true;
                        racketIsMovingLeft = false;
                    } else {
                        racketIsMovingLeft = true;
                        racketIsMovingRight = false;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    racketIsMovingLeft = false;
                    racketIsMovingRight = false;
                    break;
            }
            return true;
            //return super.onTouchEvent(event);
        }

        public void updateCourt() {
            if(racketIsMovingRight) {
                racketPosition.x += 10;
            }

            if(racketIsMovingLeft) {
                racketPosition.x -= 10;
            }

            //Collision detection

            //hit right of screen
            if(ballPosition.x + ballWidth > screenWidth) {
                ballIsMovingLeft = true;
                ballIsMovingRight = false;
                mSoundPool.play(sample1, 1, 1, 0, 0, 1);
            }

            //hit left of screen
            if(ballPosition.x < 0) {
                ballIsMovingLeft = false;
                ballIsMovingRight = true;
                mSoundPool.play(sample1, 1, 1, 0, 0, 1);
            }

            //Edge of ball has hit bottom of the screen
            if(ballPosition.y > screenHeight - ballWidth) {
                lives -= 1;
                if(lives == 0) { //Player has  used up all lives
                    lives = 3;
                    score = 0;
                    mSoundPool.play(sample4, 1, 1, 0, 0, 1);
                }
                ballPosition.y = 1 + ballWidth; //Go to the top of the screen

                //choose new direction for next ball
                Random random = new Random();
                int startX = random.nextInt(screenWidth - ballWidth) + 1;
                ballPosition.x = startX + ballWidth;

                int ballDirection = random.nextInt(3);
                switch(ballDirection) {
                    case 0:
                        ballIsMovingLeft = true;
                        ballIsMovingRight = false;
                        break;

                    case 1:
                        ballIsMovingLeft = false;
                        ballIsMovingRight = true;
                        break;

                    case 2:
                        ballIsMovingLeft = false;
                        ballIsMovingRight = false;
                        break;
                }
            }

            //If the ball hits the top of the screen
            if(ballPosition.y <= 0) {
                ballIsMovingDown = true;
                ballIsMovingUp = false;
                ballPosition.y = 1;
                mSoundPool.play(sample2, 1, 1, 0, 0, 1);
            }

            //Depending on the directions we should be moving in, adjust x positions
            if(ballIsMovingDown) {
                ballPosition.y += 6;
            }
            if(ballIsMovingUp) {
                ballPosition.y -= 10;
            }
            if(ballIsMovingLeft) {
                ballPosition.x -= 12;
            }
            if(ballIsMovingRight) {
                ballPosition.x += 12;
            }

            //Has the ball hit the racket?
            if(ballPosition.y + ballWidth >= (racketPosition.y - racketHeight / 2)) {
                int halfRacket = racketWidth / 2;
                if(ballPosition.x + ballWidth > (racketPosition.x - halfRacket) &&
                        ballPosition.x - ballWidth < (racketPosition.x + halfRacket)) {
                    //rebound the ball vertically and play a sound
                    mSoundPool.play(sample3, 1, 1, 0, 0, 1);
                    score++;
                    ballIsMovingUp = true;
                    ballIsMovingDown = false;
                    //How will we rebound the ball?
                    if(ballPosition.x > racketPosition.x) {
                        ballIsMovingRight = true;
                        ballIsMovingLeft = false;
                    } else {
                        ballIsMovingLeft = true;
                        ballIsMovingRight = false;
                    }
                }
            }
        }

        public void drawCourt() {
            if(mHolder.getSurface().isValid()) {
                mCanvas = mHolder.lockCanvas();
                mCanvas.drawColor(Color.BLACK); //Background
                mPaint.setColor(Color.argb(255, 255, 255, 255)); //Color for racket and ball
                mPaint.setTextSize(45);
                mCanvas.drawText("Score: " + score + " Lives: " + lives + " FPS: " + fps, 20, 40, mPaint);

                //Draw the Squash racket
                mCanvas.drawRect(racketPosition.x - (racketWidth / 2), racketPosition.y - (racketHeight / 2),
                        racketPosition.x + (racketWidth / 2), racketPosition.y + racketHeight, mPaint);

                //Draw the ball
                mCanvas.drawRect(ballPosition.x, ballPosition.y, ballPosition.x + ballWidth, ballPosition.y + ballWidth, mPaint);

                mHolder.unlockCanvasAndPost(mCanvas);
            }
        }

        public void controlFPS() {
            long timeThisFrame = (System.currentTimeMillis() - lastFrameTime);
            long timeToSleep = 15 - timeThisFrame;
            if(timeThisFrame > 0) {
                fps = (int) (1000 / timeThisFrame);
            }
            if(timeToSleep > 0) {
                try {
                    mThread.sleep(timeToSleep);
                } catch (InterruptedException e) {

                }
            }

            lastFrameTime = System.currentTimeMillis();
        }

        public void pause() {
            playingSquash = false;
            try {
                mThread.join();
            } catch (InterruptedException e) {

            }
        }

        public void resume() {
            playingSquash = true;
            mThread = new Thread(this);
            mThread.start();
        }
    }
}
