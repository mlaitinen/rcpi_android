package fi.ninjaware.udpcarcontroller;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;


public class TiltTouchActivity extends Activity implements SensorEventListener {

    private static final String TAG = TiltTouchActivity.class.getName();

    private MessageDispatcher mDispatcher;

    private SensorManager mSensorManager;

    private Sensor mAccelerometer;

    private Sensor mMagnetometer;

    private float[] mGravity = null;

    private float[] mGeomagnetic = null;

    private byte previousTurn = 0;

    private ArrowImageView mImageAccel;

    private ArrowImageView mImageTurn;

    private int height;

    private TextView mTextAccel;

    private TextView mTextTurn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_accel);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Get the wifi manager.
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);

        // Fill in the access point name.
        ((TextView) findViewById(R.id.text_accesspoint)).setText(
                "Access point: " + wm.getConnectionInfo().getSSID());

        // Create a new UDP message dispatcher.
        mDispatcher = new MessageDispatcher(wm);

        // Get the accelerometer and magnetic field sensors.
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mTextAccel = (TextView) findViewById(R.id.text_accel);
        mTextTurn = (TextView) findViewById(R.id.text_turn);

        mImageAccel = (ArrowImageView) findViewById(R.id.image_accel);
        mImageTurn = (ArrowImageView) findViewById(R.id.image_turn);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        height = metrics.heightPixels;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDispatcher.close();
    }


    // finetune is for not having to reach the end of the screen for full throttle.
    private static final float finetune = 1.5f;

    private int mPointerId = -1;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int pointerId = event.getPointerId(0);
        if(mPointerId != -1 && pointerId != mPointerId) {
            return false;
        }

        byte accel = 0;
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPointerId = pointerId;

            case MotionEvent.ACTION_MOVE:
                float percentage = event.getY() / height * 100;
                int rawAccel = (int) ((100 - percentage * 2) * finetune);
                if(rawAccel < -100) rawAccel = -100;
                if(rawAccel > 100) rawAccel = 100;

                accel = (byte) (rawAccel);
                break;

            case MotionEvent.ACTION_UP:
                mPointerId = -1;
                accel = 0;
                break;
        }

        mDispatcher.sendMessage(ControlType.ACCELERATION, accel);

        mTextAccel.setText(String.format("Acceleration: %d", accel));
        mImageAccel.setMagnitude(accel);
        mImageAccel.invalidate();

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values;
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = event.values;
        }

        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3]; // azimuth, pitch, roll.
                SensorManager.getOrientation(R, orientation);

                byte turn = convertPitchToTurn(orientation[1]);
                if(turn != previousTurn) {
                    mDispatcher.sendMessage(ControlType.TURN, turn);

                    mTextTurn.setText(String.format("Turn: %d", turn));
                    mImageTurn.setMagnitude(turn);
                    mImageTurn.invalidate();

                    previousTurn = turn;
                }
            }
        }
    }

    private byte convertPitchToTurn(float rawPitch) {
        int turn = Math.round(rawPitch*150.0f);

        if(turn > 100) turn = 100;
        else if(turn < -100) turn = -100;
        else if(turn > -10 && turn < 10) turn = 0;
        else turn = (turn - turn/Math.abs(turn)*10); // -10 or 10 + turn

        return (byte) turn;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

}
