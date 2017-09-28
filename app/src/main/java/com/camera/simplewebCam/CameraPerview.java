package com.camera.simplewebCam;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.SurfaceHolder;

/**
 * Created by Administrator on 2017/9/27.
 */

public class CameraPerview extends Surface implements SurfaceHolder.Callback, Runnable {

    public CameraPerview(SurfaceTexture surfaceTexture) {
        super(surfaceTexture);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void run() {

    }
}
