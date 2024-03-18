package com.tcvdev.lpr;

import static org.opencv.core.CvType.CV_8UC4;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.usb.UsbDevice;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.arksine.libusbtv.DeviceParams;
import com.arksine.libusbtv.IUsbTvDriver;
import com.arksine.libusbtv.UsbTv;
import com.arksine.libusbtv.UsbTvFrame;
import com.codekidlabs.storagechooser.Content;
import com.codekidlabs.storagechooser.StorageChooser;
import com.frank.ffmpeg.VideoPlayer;
import com.lpr.ua.UALPREngine;
import com.tcvdev.lpr.common.USBTVRenderer;
import com.tcvdev.lpr.common.Util;
import com.tcvdev.lpr.element.DetectView;
import com.tcvdev.lpr.model.CARPLATEDATA;

import org.opencv.core.Mat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback,
        VideoPlayer.FFMPEGCallback, USBTVRenderer.USBRenderCallback, UALPREngine.UALPRCallback {
    private static final int STATUS_STOP = 0;
    private static final int STATUS_PAUSE = 1;
    private static final int STATUS_PLAY = 2;
    private EditText m_etVideoPath, m_etUSBPath;
    private Button m_btnVideoOpen, m_btnUSBOpen, m_btnPlay, m_btnPause, m_btnStop;
    private Button m_btnRewind30, m_btnRewind10, m_btnForward30, m_btnForward10;
    private RadioButton m_rbVideo, m_rbUSB;
    private Spinner m_spinCountry;
    private SurfaceView m_sfImageView;
    private DetectView m_vwDetect;
    private SurfaceHolder m_surfaceHolder;
    private TextView m_tvPlayingTime;
    private SeekBar m_sbTime;
    private VideoPlayer m_videoPlayer;
    private RelativeLayout m_rlImageView;
    private MediaMetadataRetriever mediaMetadataRetriever;
    private int m_nVideoWidth = 0;
    private int m_nVideoHeight = 0;
    private boolean m_bVideoPaused = false;
    private boolean m_bUSBPaused = false;
    private long mDuration;
    private boolean m_bFirstOpen = true;
    private byte[] byteVideoFrame;
    private byte[] byteUSBFrame;
    private UsbDevice m_device = null;
    private IUsbTvDriver mUSBDriver;
    private Surface mPreviewSurface;
    private boolean m_bCameraOpened = false;
    private final Object CAM_LOCK = new Object();
    private CARPLATEDATA m_prevCarPlateData;
    AtomicBoolean mIsStreaming = new AtomicBoolean(false);
    private USBTVRenderer mRenderer = null;
    private int m_nPlayStatus;
    private int m_nNotFoundCnt;
    private final StorageChooser.Builder builder = new StorageChooser.Builder();
    private final ArrayList<String> mCountryList = new ArrayList<>();

    private static final String[] perms = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        initVariable();
        m_videoPlayer = new VideoPlayer();
        m_videoPlayer.setFFMPEGCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 444) {
            boolean granted = true;
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    granted = false;
                    break;
                }
            }
            if (!granted) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.str_title_permission_denied);
                builder.setMessage(R.string.str_msg_permission_denied);
                builder.setNegativeButton(R.string.str_close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                AlertDialog dlg = builder.create();
                dlg.setCanceledOnTouchOutside(false);
                dlg.show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_video_open) {
            onBtnVideoOpen();
        }
        if (v.getId() == R.id.btn_usb_open) {
            onBtnUSBOpen();
        }
        if (v.getId() == R.id.btn_video_play) {
            onBtnVideoPlay();
        }

        if (v.getId() == R.id.btn_video_pause) {
            onBtnVideoPause();
        }

        if (v.getId() == R.id.btn_video_stop) {
            onBtnVideoStop();
        }

        if (v.getId() == R.id.btn_rewind_10) {
            setSeekStatus(-1);
        }

        if (v.getId() == R.id.btn_rewind_30) {
            setSeekStatus(-2);
        }

        if (v.getId() == R.id.btn_forward_10) {
            setSeekStatus(1);
        }

        if (v.getId() == R.id.btn_forward_30) {
            setSeekStatus(2);
        }
    }

    private void onBtnUSBOpen() {
        onBtnVideoStop();
        m_btnPlay.setEnabled(false);
        openUSBCamera();
    }

    private void setSeekStatus(int nSeekState) {
        if (m_videoPlayer != null)
            m_videoPlayer.setSeekStatus(nSeekState);
    }

    private final UsbTv.onFrameReceivedListener mOnFrameReceivedListener = new UsbTv.onFrameReceivedListener() {
        @Override
        public void onFrameReceived(UsbTvFrame frame) {
            if (mRenderer != null) {
                mRenderer.processFrame(frame);
            }
        }
    };
    private final UsbTv.DriverCallbacks mCallbacks = new UsbTv.DriverCallbacks() {
        @Override
        public void onOpen(IUsbTvDriver driver, final boolean status) {
            Timber.i("UsbTv Open Status: %b", status);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "UsbTv Open Status: " + status, Toast.LENGTH_SHORT).show();
                }
            });
            m_bCameraOpened = true;
            synchronized (CAM_LOCK) {
                mUSBDriver = driver;

                if (mUSBDriver != null) {
                    mUSBDriver.setOnFrameReceivedListener(mOnFrameReceivedListener);
                    final int frameWidth = mUSBDriver.getDeviceParams().getFrameWidth();
                    final int frameHeight = mUSBDriver.getDeviceParams().getFrameHeight();
                    byteUSBFrame = new byte[frameWidth * frameHeight * 4];
                    m_vwDetect.setImageSize(frameWidth, frameHeight);
                    m_sfImageView.post(new Runnable() {
                        @Override
                        public void run() {

                            int containerWidth = m_rlImageView.getWidth();
                            int containerHeight = m_rlImageView.getHeight();

                            float scaleX = frameWidth / (float) containerWidth;
                            float scaleY = frameHeight / (float) containerHeight;
                            float scale = Math.max(scaleX, scaleY);

                            int realWidth = (int) (frameWidth / scale);
                            int realHeight = (int) (frameHeight / scale);
                            setVideoSize(realWidth, realHeight);
                        }
                    });

                    // If I have a preview surface, we can fetch the renderer and start it
                    if (mPreviewSurface != null) {
                        mIsStreaming.set(true);
                        if (mRenderer == null) {
                            mRenderer = new USBTVRenderer(getApplicationContext(), mPreviewSurface);
                            mRenderer.setUSBRenderCallback(MainActivity.this);
                        } else {
                            mRenderer.setSurface(mPreviewSurface);
                        }
                        mRenderer.startRenderer(mUSBDriver.getDeviceParams(), byteUSBFrame);
                        mUSBDriver.startStreaming();
                    }
                }
            }
        }

        @Override
        public void onClose() {
            Timber.i("UsbTv Device Closed");
            m_bCameraOpened = false;
            if (mRenderer != null) {
                mRenderer.stopRenderer();
            }

            if (mPreviewSurface != null) {
                mPreviewSurface.release();
            }
            // Unregister Library Receiver
            UsbTv.unregisterUsbReceiver(MainActivity.this);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
        }

        @Override
        public void onError() {
            Timber.i("Error received");
        }
    };

    private void onBtnVideoStop() {
        m_videoPlayer.stop();

        m_btnPlay.setEnabled(true);
        m_btnPause.setEnabled(false);
        m_btnStop.setEnabled(false);
        m_nPlayStatus = STATUS_STOP;
        setPlayingTime(0, 0);
    }

    private void setPlayingTime(int cur_time, int duration) {
        final String strTime = Util.getTextFromSecond(cur_time) + " / " + Util.getTextFromSecond(duration);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                m_tvPlayingTime.setText(strTime);
                m_sbTime.setMax(duration);
                m_sbTime.setProgress(cur_time);
            }
        });
    }

    private void onBtnVideoPause() {
        m_nPlayStatus = STATUS_PAUSE;

        m_btnPlay.setEnabled(true);
        m_btnPause.setEnabled(false);
        m_btnStop.setEnabled(true);

        m_videoPlayer.pause();
    }

    private void onBtnVideoPlay() {
        m_btnPlay.setEnabled(false);
        m_btnPause.setEnabled(true);
        m_btnStop.setEnabled(true);
        if (m_nPlayStatus == STATUS_STOP) {
            try {
                loadVideo();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        m_videoPlayer.play();
        m_nPlayStatus = STATUS_PLAY;
    }

    void onBtnVideoOpen() {

        Content c = new Content();
        c.setCreateLabel("Create");
        c.setInternalStorageText("My Storage");
        c.setCancelLabel("Cancel");
        c.setSelectLabel("Select");
        c.setOverviewHeading("Choose Drive");

        builder.withActivity(this)
                .withFragmentManager(getFragmentManager())
                .setMemoryBarHeight(1.5f)
                .disableMultiSelect()
                .withContent(c);

        ArrayList<String> formats = new ArrayList<>();
        formats.add("mp4");
        formats.add("avi");
        builder.customFilter(formats);

        StorageChooser chooser = builder.build();

        chooser.setOnSelectListener(new StorageChooser.OnSelectListener() {
            @Override
            public void onSelect(String path) {
                UsbTv.closeCamera();
                m_etVideoPath.setText(path);
                m_btnPlay.setEnabled(true);
                m_btnPause.setEnabled(false);
                m_btnStop.setEnabled(false);

                if (!m_bFirstOpen)
                    onBtnVideoStop();
                m_bFirstOpen = false;
                onBtnVideoPlay();
            }
        });
        chooser.show();
    }

    void initUI() {

        m_rbVideo = findViewById(R.id.rb_video);
        m_rbUSB = findViewById(R.id.rb_usb);
        m_spinCountry = findViewById(R.id.spin_country);
        m_etVideoPath = findViewById(R.id.et_video_path);
        m_btnVideoOpen = findViewById(R.id.btn_video_open);
        m_etUSBPath = findViewById(R.id.et_usb_path);
        m_btnUSBOpen = findViewById(R.id.btn_usb_open);
        m_sfImageView = findViewById(R.id.sf_imageview);
        m_rlImageView = findViewById(R.id.rl_preview);
        m_tvPlayingTime = findViewById(R.id.tv_playing_time);
        m_sbTime = findViewById(R.id.sb_time);
        m_btnPlay = findViewById(R.id.btn_video_play);
        m_btnPause = findViewById(R.id.btn_video_pause);
        m_btnStop = findViewById(R.id.btn_video_stop);
        m_btnRewind10 = findViewById(R.id.btn_rewind_10);
        m_btnRewind30 = findViewById(R.id.btn_rewind_30);
        m_btnForward10 = findViewById(R.id.btn_forward_10);
        m_btnForward30 = findViewById(R.id.btn_forward_30);
        m_vwDetect = findViewById(R.id.vw_detect);


        m_sbTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setSeekSec(seekBar.getProgress());
            }
        });

        m_surfaceHolder = m_sfImageView.getHolder();
        m_surfaceHolder.addCallback(this);
        m_rbVideo.setChecked(true);
        m_rbUSB.setChecked(false);

        m_rbUSB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableViews(2);
            }
        });

        m_rbVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRenderer != null) {
                    mRenderer.stopRenderer();
                }
                if (mUSBDriver != null && mUSBDriver.isOpen()) {
                    mUSBDriver.close();
                }
                enableViews(1);
            }
        });

        m_btnVideoOpen.setOnClickListener(this);
        m_btnUSBOpen.setOnClickListener(this);
        m_btnPlay.setOnClickListener(this);
        m_btnPause.setOnClickListener(this);
        m_btnStop.setOnClickListener(this);

        m_btnRewind10.setOnClickListener(this);
        m_btnRewind30.setOnClickListener(this);
        m_btnForward10.setOnClickListener(this);
        m_btnForward30.setOnClickListener(this);
        m_btnPlay.setEnabled(false);
        m_btnPause.setEnabled(false);
        m_btnStop.setEnabled(false);

        m_spinCountry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        enableViews(1);
        UsbTv.registerUsbReceiver(this);
    }

    void setSeekSec(float fltSec) {

        if (m_videoPlayer != null)
            m_videoPlayer.setSeekSec(fltSec);
    }

    void initVariable() {
        builder.allowCustomPath(true);
        builder.setType(StorageChooser.FILE_PICKER);
        builder.shouldResumeSession(true);

        Collections.addAll(mCountryList, getResources().getStringArray(R.array.arr_country));
        ArrayAdapter<String> mCountryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mCountryList);
        m_spinCountry.setAdapter(mCountryAdapter);
    }

    void enableViews(int mode) {

        if (mode == 1) {    //video
            m_rbUSB.setChecked(false);
            m_etUSBPath.setEnabled(false);
            m_btnUSBOpen.setEnabled(false);

            m_etVideoPath.setEnabled(true);
            m_btnVideoOpen.setEnabled(true);
        } else if (mode == 2) {     //USB

            m_rbVideo.setChecked(false);
            m_etVideoPath.setEnabled(false);
            m_btnVideoOpen.setEnabled(false);

            m_etUSBPath.setEnabled(true);
            m_btnUSBOpen.setEnabled(true);
        }
    }

    private void checkPermissions() {
        boolean granted = true;
        for (String perm : perms) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, perm) == PackageManager.PERMISSION_DENIED) {
                granted = false;
                break;
            }
        }
        if (!granted) {
            ActivityCompat.requestPermissions(MainActivity.this, perms, 444);
        }
    }

    private void loadVideo() throws IOException {
        m_nNotFoundCnt = 0;
        final String strVideoPath = m_etVideoPath.getText().toString();
        try {
            mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(this, Uri.parse(strVideoPath));
            m_nVideoWidth = Integer.parseInt(Objects.requireNonNull(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)));
            m_nVideoHeight = Integer.parseInt(Objects.requireNonNull(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)));
            m_vwDetect.setImageSize(m_nVideoWidth, m_nVideoHeight);
            m_sfImageView.post(new Runnable() {
                @Override
                public void run() {

                    int containerWidth = m_rlImageView.getWidth();
                    int containerHeight = m_rlImageView.getHeight();

                    float scaleX = m_nVideoWidth / (float) containerWidth;
                    float scaleY = m_nVideoHeight / (float) containerHeight;
                    float scale = Math.max(scaleX, scaleY);

                    int realWidth = (int) (m_nVideoWidth / scale);
                    int realHeight = (int) (m_nVideoHeight / scale);

                    setVideoSize(realWidth, realHeight);
                }
            });
            byteVideoFrame = new byte[m_nVideoWidth * m_nVideoHeight * 4];

            new Thread(new Runnable() {
                @Override
                public void run() {
                    m_videoPlayer.loadVideo(strVideoPath, m_surfaceHolder.getSurface(), byteVideoFrame);
                }
            }).start();
        } catch (Exception e) {
            Toast.makeText(this, "Can't load the video", Toast.LENGTH_SHORT).show();
            mediaMetadataRetriever.release();
            return;
        }

        String strDuration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        assert strDuration != null;
        mDuration = Long.parseLong(strDuration);
        mediaMetadataRetriever.release();
    }

    private void setVideoSize(int width, int height) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) m_sfImageView.getLayoutParams();
        params.height = height;
        params.width = width;
        m_sfImageView.setLayoutParams(params);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        if (m_rbVideo.isChecked() && m_bVideoPaused) {
            m_videoPlayer.setSurface(m_surfaceHolder.getSurface());
            m_videoPlayer.play();
            m_bVideoPaused = false;
        }
        if (m_bCameraOpened && m_rbUSB.isChecked() && m_bUSBPaused) {
            openUSBCamera();
            m_bUSBPaused = false;
        }
    }

    private void openUSBCamera() {
        ArrayList<UsbDevice> devList = UsbTv.enumerateUsbtvDevices(this);

        if (!devList.isEmpty()) {
            m_device = devList.get(0);
        } else {
            Timber.i("Dev List Empty");
        }

        if (m_device == null) {
            Timber.i("Can't open");
            return;
        }
        DeviceParams params = new DeviceParams.Builder()
                .setUsbDevice(m_device)
                .setDriverCallbacks(mCallbacks)
                .setInput(UsbTv.InputSelection.COMPOSITE)
                .setScanType(UsbTv.ScanType.PROGRESSIVE)
                .setTvNorm(UsbTv.TvNorm.NTSC)
                .build();
        m_surfaceHolder.setFixedSize(params.getFrameWidth(), params.getFrameHeight());
        m_surfaceHolder.setFormat(PixelFormat.RGBA_8888);

        Timber.i("Open Device");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Open Device", Toast.LENGTH_SHORT).show();
            }
        });
        UsbTv.open(this, params);
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        if (m_rbUSB.isChecked()) {

            if ((width == 0) || (height == 0)) return;
            Timber.v("Camera surfaceChanged:");
            mPreviewSurface = holder.getSurface();
            synchronized (CAM_LOCK) {
                if (mUSBDriver != null) {
                    if (mRenderer == null) {
                        mRenderer = new USBTVRenderer(getApplicationContext(), mPreviewSurface);
                        mRenderer.setUSBRenderCallback(MainActivity.this);
                    } else {
                        mRenderer.setSurface(mPreviewSurface);
                    }

                    final int frameWidth = mUSBDriver.getDeviceParams().getFrameWidth();
                    final int frameHeight = mUSBDriver.getDeviceParams().getFrameHeight();
                    byteUSBFrame = new byte[frameWidth * frameHeight * 4];
                    m_vwDetect.setImageSize(frameWidth, frameHeight);

                    m_sfImageView.post(new Runnable() {
                        @Override
                        public void run() {

                            int containerWidth = m_rlImageView.getWidth();
                            int containerHeight = m_rlImageView.getHeight();

                            float scaleX = frameWidth / (float) containerWidth;
                            float scaleY = frameHeight / (float) containerHeight;
                            float scale = Math.max(scaleX, scaleY);

                            int realWidth = (int) (frameWidth / scale);
                            int realHeight = (int) (frameHeight / scale);

                            setVideoSize(realWidth, realHeight);
                        }
                    });

                    // If not streaming, start
                    if (mIsStreaming.compareAndSet(false, true)) {
                        mRenderer.startRenderer(mUSBDriver.getDeviceParams(), byteUSBFrame);
                        mUSBDriver.startStreaming();
                    }
                }
            }
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        if (m_rbVideo.isChecked()) {
            m_videoPlayer.pause();
            m_bVideoPaused = true;
        }
        if (m_rbUSB.isChecked()) {

            synchronized (CAM_LOCK) {
                if (mRenderer != null) {
                    mRenderer.stopRenderer();
                }

                if (mUSBDriver != null && mIsStreaming.get()) {
                    mUSBDriver.stopStreaming();
                }
                mIsStreaming.set(false);
                mPreviewSurface = null;
                m_bUSBPaused = true;
                if (mUSBDriver != null)
                    mUSBDriver.close();
                mRenderer = null;

                UsbTv.closeCamera();
            }
        }
    }

    @Override
    public void onGrabFrame(int cur_time, int duration) {
        setPlayingTime(cur_time, duration);
    }

    @Override
    public void onPlayStatus(int play_status) {
        if (play_status == 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBtnVideoStop();
                }
            });
        }
    }

    @Override
    public void onUSBFrameGrab(int width, int height) {
        Mat matFrame = new Mat(height, width, CV_8UC4);
        matFrame.put(0, 0, byteUSBFrame);
    }

    @Override
    public void onUALPRResult(byte[] byteArray) {
        CARPLATEDATA carplatedata = new CARPLATEDATA();
        CARPLATEDATA.parseFromByte(byteArray, carplatedata);

        if (carplatedata.nPlate == 0) {
            if (m_nNotFoundCnt < 10) {
                m_nNotFoundCnt++;
                m_vwDetect.setLPRResult(m_prevCarPlateData);
            } else {

                m_prevCarPlateData = null;
                m_vwDetect.setLPRResult(m_prevCarPlateData);
                m_nNotFoundCnt = 0;
            }
        } else {
            m_nNotFoundCnt = 0;
            m_vwDetect.setLPRResult(carplatedata);
            m_prevCarPlateData = carplatedata;
        }
    }
}