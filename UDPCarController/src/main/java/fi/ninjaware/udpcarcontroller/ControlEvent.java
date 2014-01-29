package fi.ninjaware.udpcarcontroller;

/**
 * Created by miku on 1/26/14.
 */
public class ControlEvent {

    private ControlType type;

    private byte magnitude;

    public ControlEvent(ControlType type, byte magnitude) {
        this.type = type;
        this.magnitude = magnitude;
    }

    public ControlType getType() {
        return type;
    }

    public byte getControlCode() {
        return type.getControlCode();
    }

    public byte getMagnitude() {
        return magnitude;
    }
}
