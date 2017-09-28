package com.camera.simplewebCam;

import android.Manifest;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.Surface;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.dialog.MessageDialogFragment;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.utils.PermissionCheck;
import com.serenegiant.widget.CameraViewInterface;

public class MainActivity extends BaseActivity implements CameraDialog.CameraDialogParent {

    private ToggleButton mCameraButton;
    private CameraViewInterface mUVCCameraView;

    private static final int PREVIEW_WIDTH = 640;
    private static final int PREVIEW_HEIGHT = 480;
    private static final int PREVIEW_MODE = 1;
    private static final boolean USE_SURFACE_ENCODER = false;

    private static final String TAG = "SimpleWebCam";

    private UVCCameraHandler mCameraHandler;
    private USBMonitor mUSBMonitor;

    protected static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 0x12345;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraButton = (ToggleButton)findViewById(R.id.camera_button);
        mCameraButton.setOnCheckedChangeListener(mOnCheckedChangeListener);
        final View view = findViewById(R.id.camera_view);
        view.setOnLongClickListener(mOnLongClickListener);
        mUVCCameraView = (CameraViewInterface)view;
        mUVCCameraView.setAspectRatio(PREVIEW_WIDTH /(float)PREVIEW_HEIGHT);

        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        mCameraHandler = UVCCameraHandler.createHandler(this, mUVCCameraView,
                USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mUSBMonitor.register();
        if (mUVCCameraView != null) {
            mUVCCameraView.onResume();
        }
    }

    @Override
    protected void onStop() {
        mCameraHandler.close();
        if (null != mUVCCameraView) {
            mUVCCameraView.onPause();
        }
        setCameraButton(false);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (null != mCameraHandler) {
            mCameraHandler.release();
            mCameraHandler = null;
        }

        if (null != mUSBMonitor) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }

        mUVCCameraView = null;
        super.onDestroy();
    }

    protected boolean checkPermissionWriteExternalStorage() {
        if (!PermissionCheck.hasWriteExternalStorage(this)) {
            MessageDialogFragment.showDialog(this, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
                    R.string.permission_title, R.string.permission_ext_storage_request,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE});
            return false;
        }
        return true;
    }

    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            setCameraButton(false);
        }
    }

    private void updateItems() {
        runOnUiThread(mUpdateItemsOnUITask, 100);
    }

    private boolean isActive() {
        return mCameraHandler != null && mCameraHandler.isOpened();
    }

    private void startPreview() {
        final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
        mCameraHandler.startPreview(new Surface(st));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
        updateItems();
    }

    private final Runnable mUpdateItemsOnUITask = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) {
                return;
            }

            final int visible_active = isActive() ? View.VISIBLE : View.INVISIBLE;
        }
    };

    private void setCameraButton(final boolean isOn) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mCameraButton != null) {
                    try {
                        mCameraButton.setOnCheckedChangeListener(null);
                        mCameraButton.setChecked(isOn);
                    }finally {
                        mCameraButton.setOnCheckedChangeListener(mOnCheckedChangeListener);
                    }
                }

            }
        }, 0);
    }

    private final CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener
            = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            switch (compoundButton.getId()) {
                case R.id.camera_button: {
                    if (isChecked && !mCameraHandler.isOpened()) {
                        CameraDialog.showDialog(MainActivity.this);
                    } else {
                        mCameraHandler.close();
                        setCameraButton(false);
                    }
                }
                break;
            }
        }
    };

    private final View.OnLongClickListener mOnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            switch (view.getId()) {
                case R.id.camera_view: {
                    if (mCameraHandler.isOpened()) {
                        if (checkPermissionWriteExternalStorage()) {
                            mCameraHandler.captureStill();
                        }
                    }
                }
                break;
            }
            return false;
        }
    };

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(UsbDevice device) {
            Toast.makeText(MainActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDettach(UsbDevice device) {
            Toast.makeText(MainActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
            mCameraHandler.open(ctrlBlock);
            startPreview();
            updateItems();
        }

        @Override
        public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
            if (mCameraHandler != null) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mCameraHandler.close();
                    }
                }, 0);
                setCameraButton(false);
                updateItems();
            }
        }

        @Override
        public void onCancel(UsbDevice device) {
            setCameraButton(false);
        }
    };
}
