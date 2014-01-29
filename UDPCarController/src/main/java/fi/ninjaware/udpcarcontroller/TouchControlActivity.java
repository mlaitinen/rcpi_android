package fi.ninjaware.udpcarcontroller;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;


public class TouchControlActivity extends Activity implements SensorEventListener {

    private static final String TAG = TouchControlActivity.class.getName();

    private MessageDispatcher mDispatcher;

    private SensorManager mSensorManager;

    private Sensor mAccelerometer;

    private Sensor mMagnetometer;

    private float[] mGravity = null;

    private float[] mGeomagnetic = null;

    private byte previousTurn = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_touchcontrol);

    }

    @Override
    protected void onStart() {
        super.onStart();

        mDispatcher = new MessageDispatcher((WifiManager) getSystemService(WIFI_SERVICE));

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        final SeekBar accel = (SeekBar) findViewById(R.id.control_accel);
        accel.setOnSeekBarChangeListener(new ControlChangeListener(ControlType.ACCELERATION));
        accel.setProgress(accel.getMax() / 2);

        final SeekBar turn = (SeekBar) findViewById(R.id.control_turn);
        turn.setOnSeekBarChangeListener(new ControlChangeListener(ControlType.TURN));
        turn.setProgress(turn.getMax() / 2);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDispatcher.close();
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

    private class ControlChangeListener implements SeekBar.OnSeekBarChangeListener {

        private ControlType controlType;

        private ControlChangeListener(ControlType type) {
            this.controlType = type;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
            mDispatcher.sendMessage(controlType, getMagnitude(seekBar.getMax(), value));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int progress = seekBar.getMax() / 2;
            seekBar.setProgress(progress);
            mDispatcher.sendMessage(controlType, getMagnitude(seekBar.getMax(), progress));
        }

        private byte getMagnitude(int max, int rawValue) {
            return (byte) (rawValue - max / 2);
        }

    }

}
