package uri.egr.biosensing.arduinologgingapp.gatt_attributes;

import java.util.UUID;

/**
 * Created by mcons on 11/14/2016.
 */

public class GattCharacteristics {
    public static final UUID TX_CHARACTERISTIC = UUID.fromString("713d0003-503e-4c75-ba94-3148f18d941e");
    public static final UUID RX_CHARACTERISTIC = UUID.fromString("713d0002-503e-4c75-ba94-3148f18d941e");

    public static final UUID WIOT_CHARACTERISTIC = UUID.fromString("713d0004-503e-4c75-ba94-3148f18d941e");
}
