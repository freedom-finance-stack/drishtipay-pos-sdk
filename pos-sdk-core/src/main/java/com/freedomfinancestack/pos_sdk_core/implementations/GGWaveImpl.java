package com.freedomfinancestack.pos_sdk_core.implementations;

import com.freedomfinancestack.pos_sdk_core.interfaces.IGGWave;

public class GGWaveImpl implements IGGWave {


    // core implementations for GGWAVE
    @Override
    public boolean receive(String text) {
        return false;
    }

    @Override
    public boolean send(String text) {
        return false;
    }
}
