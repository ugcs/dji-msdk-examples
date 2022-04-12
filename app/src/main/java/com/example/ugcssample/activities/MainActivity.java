package com.example.ugcssample.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.ugcssample.R;
import com.example.ugcssample.drone.DroneBridgeImpl;
import com.example.ugcssample.fragment.VideoViewFragment;
import com.example.ugcssample.services.DjiAppMainService;
import com.example.ugcssample.services.DjiAppMainServiceBinder;
import com.example.ugcssample.services.DjiAppMainServiceImpl;
import com.example.ugcssample.utils.ArrayUtils;
import com.example.ugcssample.utils.PermissionUtils;

import java.util.List;

import dji.sdk.camera.VideoFeeder;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private static final IntentFilter EVENT_FILTER = new IntentFilter();

    static {
        EVENT_FILTER.addAction(DroneBridgeImpl.ON_DRONE_CONNECTED);
    }
    private Intent sIntent;
    private ServiceConnection sConn;
    private Button btnSimulator, btnTakeCapture;
    private EditText editTextXmpTag;
    protected DjiAppMainService appMainService;
    public static final int REQUEST_PERMISSION_CODE = 2358;
    LocalBroadcastManager broadcastManager;
    private VideoViewFragment primaryVideoFeedView;

    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();
            if (action == null)
                return;

            if (DroneBridgeImpl.ON_DRONE_CONNECTED.equals(action)) {
                btnSimulator.setEnabled(true);
                btnTakeCapture.setEnabled(true);
                editTextXmpTag.setEnabled(true);
                primaryVideoFeedView.registerLiveVideo(VideoFeeder.getInstance().getPrimaryVideoFeed(), true);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sIntent = new Intent(this, DjiAppMainServiceImpl.class);
        sConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                MainActivity.this.onMainServiceConnected(name, binder);
            }

            public void onServiceDisconnected(ComponentName name) {
                MainActivity.this.onMainServiceDisconnected();
            }
        };
        primaryVideoFeedView = (VideoViewFragment) findViewById(R.id.video_view_primary_video_feed);
        btnSimulator = (Button) findViewById(R.id.btn_simulator);
        btnSimulator.setOnClickListener(v -> appMainService.startSimulator());
        btnTakeCapture = (Button) findViewById(R.id.btn_take_capture);
        editTextXmpTag = (EditText) findViewById(R.id.edit_text_xmp_tag);
        btnTakeCapture.setOnClickListener(v -> {
            String xmpTag = editTextXmpTag.getText().toString();
            appMainService.takeCapture(new Handler(), xmpTag);
        });
    }

    protected void onMainServiceConnected(ComponentName name, IBinder binder) {
        appMainService = ((DjiAppMainServiceBinder)binder).getService();
        checkAndRequestAndroidPermissions();
    }

    protected void onMainServiceDisconnected() {
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(sIntent, sConn, BIND_AUTO_CREATE);
        broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(eventReceiver, EVENT_FILTER);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unbindService(sConn);
        super.onDestroy();
    }

    private void checkAndRequestAndroidPermissions() {
        List<String> missing = PermissionUtils.checkForMissingPermission(getApplicationContext());
        if (!ArrayUtils.isEmpty(missing)) {
            String[] missingPermissions = missing.toArray(new String[0]);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this, missingPermissions, REQUEST_PERMISSION_CODE);
            } else {
                //failed
            }
        } else {
            //all android permissions granted;
            onAndroidPermissionsValid();
        }
    }
    protected void onAndroidPermissionsValid() {
        appMainService.init();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            Timber.d("onRequestPermissionsResult...");
            checkAndRequestAndroidPermissions();
        }
    }
}