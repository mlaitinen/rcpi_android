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


public class TouchControlActivity extends Activity {

    private static final String TAG = TouchControlActivity.class.getName();

    private MessageDispatcher mDispatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_touchcontrol);

    }

    @Override
    protected void onStart() {
        super.onStart();

        mDispatcher = new MessageDispatcher((WifiManager) getSystemService(WIFI_SERVICE));

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
