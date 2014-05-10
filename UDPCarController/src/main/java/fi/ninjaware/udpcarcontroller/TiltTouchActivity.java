package fi.ninjaware.udpcarcontroller;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class TiltTouchActivity extends Activity implements SensorEventListener {

    private enum ControlMode {
        BOTH,
        STEERING,
        ENGINE
    }

    private static final String TAG = TiltTouchActivity.class.getName();

    private MessageDispatcher mDispatcher;

    private SensorManager mSensorManager;

    private Sensor mAccelerometer;

    private Sensor mMagnetometer;

    private float[] mGravity = new float[3];

    private float[] mGeomagnetic = new float[3];

    private byte previousTurn = 0;

    private float previousPitch = 0.0f;

    private byte previousAccel = 0;

    private ArrowImageView mImageAccel;

    private ArrowImageView mImageTurn;

    private int height;

    private TextView mTextAccel;

    private TextView mTextTurn;

    private ScheduledFuture<?> wifiSignalSchedule;

    private ControlMode currentControlMode = ControlMode.BOTH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_accel);

        ((Button) findViewById(R.id.button_max_angle)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float pitch = Math.abs(previousPitch);
                angleFactor = 100 / pitch;

                Toast.makeText(TiltTouchActivity.this, String.format("Max angle: %.2f", pitch),
                        Toast.LENGTH_SHORT).show();
            }
        });

        final Button controlButton = ((Button) findViewById(R.id.button_controls_enabled));
        controlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /* Cycle:
                    1. Engine + steering
                    2. Steering
                    3. Engine
                    */

                switch(currentControlMode) {
                    case BOTH:
                        currentControlMode = ControlMode.STEERING;
                        controlButton.setText("Steering");
                        break;
                    case STEERING:
                        currentControlMode = ControlMode.ENGINE;
                        controlButton.setText("Engine");
                        break;
                    case ENGINE:
                        currentControlMode = ControlMode.BOTH;
                        controlButton.setText("Eng + steer");
                        break;
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Get the wifi manager.
        final WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);

        // Check the wifi signal strength every second
        ScheduledExecutorService wifiSignalService = Executors.newSingleThreadScheduledExecutor();
        wifiSignalSchedule = wifiSignalService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        WifiInfo info = wm.getConnectionInfo();
                        int strength = WifiManager.calculateSignalLevel(info.getRssi(), 100);
                        String strengthStr = String.format("Signal strength: %d", strength);

                        // Fill in the signal strength.
                        ((TextView) findViewById(R.id.text_signal_strength)).setText(strengthStr);

                        // Fill in the access point name.
                        ((TextView) findViewById(R.id.text_accesspoint)).setText(
                                "Access point: " + info.getSSID());
                    }
                });
            }
        }, 0, 1, TimeUnit.SECONDS);

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
        wifiSignalSchedule.cancel(false);
    }


    // finetune is for not having to reach the end of the screen for full throttle.
    private static final float finetune = 1.5f;

    private int mPointerId = -1;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if(currentControlMode.equals(ControlMode.STEERING)) {
            return true;
        }

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

        if(accel != previousAccel) {
            mDispatcher.sendMessage(new ControlEvent(ControlType.ACCELERATION, accel));

            mTextAccel.setText(String.format("Acceleration: %d", accel));
            mImageAccel.setMagnitude(accel);
            mImageAccel.invalidate();

            previousAccel = accel;
        }

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if(currentControlMode == ControlMode.ENGINE) {
            return;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values.clone();
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = event.values.clone();
        }

        float R[] = new float[9];
        float I[] = new float[9];
        boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
        if (success) {
            float orientation[] = new float[3]; // azimuth, pitch, roll.
            SensorManager.getOrientation(R, orientation);

            byte turn = convertPitchToTurn(orientation[1]);
            previousPitch = orientation[1];
            if(turn != previousTurn) {

                mDispatcher.sendMessage(new ControlEvent(ControlType.TURN, turn));

                mTextTurn.setText(String.format("Steering: %d", turn));
                mImageTurn.setMagnitude(turn);
                mImageTurn.invalidate();

                previousTurn = turn;
            }
        }
    }

    private float angleFactor = 150.0f;

    private byte convertPitchToTurn(float rawPitch) {
        int turn = Math.round(rawPitch* angleFactor);

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
