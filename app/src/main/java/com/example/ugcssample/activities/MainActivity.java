package com.example.ugcssample.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
        for (DroneBridgeImpl.BroadcastActions action : DroneBridgeImpl.BroadcastActions.values()) {
            EVENT_FILTER.addAction(action.name());
        }
    }
    private Intent sIntent;
    private ServiceConnection sConn;
    private Button btnSimulator;
    private Button btnUploadMission;
    private Button btnStartMission;
    private Button btnTakeOff;
    private Button btnLand;
    private TextView tvMissionDebug;
    protected DjiAppMainService appMainService;
    public static final int REQUEST_PERMISSION_CODE = 2358;
    LocalBroadcastManager broadcastManager;
    private VideoViewFragment primaryVideoFeedView;

    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final DroneBridgeImpl.BroadcastActions action = DroneBridgeImpl.BroadcastActions.valueOf(intent.getAction());
            if (action == null)
                return;
            switch (action){
                case ON_SIMULATOR_START:
                    btnSimulator.setEnabled(false);
                    btnSimulator.setText("Simulator is \nON");
                break;
                case ON_DRONE_CONNECTED:
                    btnSimulator.setEnabled(true);
                    btnUploadMission.setEnabled(true);
                    btnSimulator.setEnabled(true);
                    btnTakeOff.setEnabled(true);
                    primaryVideoFeedView.registerLiveVideo(VideoFeeder.getInstance().getPrimaryVideoFeed(), true);
                break;
                case ON_MISSION_STATUS:
                    String descr = intent.getStringExtra("desc");
                    int code = intent.getIntExtra("code", 0);

                    switch (code) {
                        case 1:
                            btnStartMission.setEnabled(true);
                            tvMissionDebug.setText("Mission upload: "+descr);
                        break;
                        case 2:
                            tvMissionDebug.setText("Mission run: "+descr);
                        break;
                        case 3:
                            tvMissionDebug.setText(descr);
                        break;
                        default:
                            tvMissionDebug.setText("Mission error: "+descr);
                        break;

                    }
                break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sIntent = new Intent(this, DjiAppMainServiceImpl.class);
        sConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                MainActivity.this.onMainServiceConnected(name, binder);
            }

            public void onServiceDisconnected(ComponentName name) {
                MainActivity.this.onMainServiceDisconnected();
            }
        };
        tvMissionDebug = findViewById(R.id.mission_debug_tv);
        primaryVideoFeedView = findViewById(R.id.video_view_primary_video_feed);
        btnSimulator = findViewById(R.id.btn_simulator);
        btnSimulator.setOnClickListener(v -> appMainService.startSimulator());

        btnUploadMission = findViewById(R.id.btn_upload_mission);
        btnUploadMission.setOnClickListener(v -> {
            btnStartMission.setEnabled(false);
            appMainService.uploadMission();
        });

        btnStartMission = findViewById(R.id.btn_start_mission);
        btnStartMission.setOnClickListener(v -> {
            appMainService.startMission();
            btnLand.setEnabled(true);
        });

        btnTakeOff = findViewById(R.id.btn_takeoff);
        btnTakeOff.setOnClickListener(v -> {
            appMainService.takeOff();
            btnLand.setEnabled(true);
        });

        btnLand = findViewById(R.id.btn_land);
        btnLand.setOnClickListener(v -> {
            appMainService.land();
            btnTakeOff.setEnabled(true);
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