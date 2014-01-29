package fi.ninjaware.udpcarcontroller;

/**
 * Created by miku on 1/26/14.
 */
public enum ControlType {

    TURN(1),
    ACCELERATION(2);

    ControlType(int controlCode) {
        this.controlCode = (byte) controlCode;
    }

    private byte controlCode;

    public byte getControlCode() {
        return controlCode;
    }

}
