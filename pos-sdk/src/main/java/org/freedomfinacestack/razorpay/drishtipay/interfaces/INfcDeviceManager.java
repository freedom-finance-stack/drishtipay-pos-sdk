package org.freedomfinacestack.razorpay.drishtipay.interfaces;

import android.nfc.NdefMessage;
public interface INfcDeviceManager {

    /**
     * Starts listening for NFC taps that contain NDEF data.
     *
     * @param callback The callback to be invoked when an NDEF message is discovered.
     */
    void startListening(NdefCallback callback);

    /**
     * Stops listening for NFC taps.
     */
    void stopListening();

    /**
     * A callback interface to handle NDEF message detection. This is much more
     * direct for your use case than dealing with a raw channel.
     */
    interface NdefCallback {
        /**
         * Called when a tag containing a valid NDEF message is tapped.
         *
         * @param message The NDEF message read from the NFC tag.
         */
        void onNdefMessageDiscovered(NdefMessage message);

        /**
         * Called if a tag is tapped but an error occurs.
         * @param errorMessage A description of the error.
         */
        void onError(String errorMessage);
    }
} 