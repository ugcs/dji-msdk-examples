package com.example.ugcssample.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

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
import dji.ux.widget.FPVOverlayWidget;
import dji.ux.widget.FPVWidget;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private static final IntentFilter EVENT_FILTER = new IntentFilter();

    static {
        EVENT_FILTER.addAction(DroneBridgeImpl.ON_DRONE_CONNECTED);
    }
    private Intent sIntent;
    private ServiceConnection sConn;
    private Button btnSimulator;
    private Button bindRc;
    private Button wideButton;
    private Button zoomButton;
    private Button bindCustomMedia;
    private Button bindWidgets;
    private FPVWidget fpvWidget;
    private FPVOverlayWidget fpvOverlayWidget;
    private FPVWidget secondaryFpvWidget;
    private boolean switched = false;
    private ImageButton btnPreferences;
    protected DjiAppMainService appMainService;
    public static final int REQUEST_PERMISSION_CODE = 2358;
    LocalBroadcastManager broadcastManager;
  //  private VideoViewFragment primaryVideoFeedView;

    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();
            if (action == null)
                return;

            if (DroneBridgeImpl.ON_DRONE_CONNECTED.equals(action)) {
               // btnSimulator.setEnabled(true);
                bindRc.setEnabled(true);
              //  bindCustomMedia.setEnabled(true);
                bindWidgets.setEnabled(true);
                wideButton.setEnabled(true);
                zoomButton.setEnabled(true);
               // primaryVideoFeedView.registerLiveVideo(VideoFeeder.getInstance().getPrimaryVideoFeed(), true);
            }
        }
    };


    @Override
    @SuppressLint("CommitPrefEdits")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String list_preference_C1 = prefs.getString("list_preference_C1", "");
        String list_preference_C2 = prefs.getString("list_preference_C2", "");
        String list_preference_C1_C2_release = prefs.getString("list_preference_C1_C2_release", "");
        String list_preference_5d_left = prefs.getString("list_preference_5d_left", "");
        String list_preference_5d_right = prefs.getString("list_preference_5d_right", "");
        String list_preference_5d_up = prefs.getString("list_preference_5d_up", "");
        String list_preference_5d_down = prefs.getString("list_preference_5d_down", "");
        String list_preference_5d_push = prefs.getString("list_preference_5d_push", "");
        String list_preference_5d_middle = prefs.getString("list_preference_5d_middle", "");

       // <item>ZoomInContinue</item>
       // <item>ZoomOutContinue</item>
      //  <item>ZoomStop</item>
      //  <item>CamUp</item>
      //  <item>CamDown</item>
      //  <item>CamLeft</item>
      //  <item>CamRight</item>
       // <item>CamStop</item>

        SharedPreferences.Editor editor = prefs.edit();
        if (list_preference_C1.equals("")) {
            editor.putString("list_preference_C1", "ZoomInContinue");
            editor.commit();
        }
        if (list_preference_C2.equals("")) {
            editor.putString("list_preference_C2", "ZoomOutContinue");
            editor.commit();
        }
        if (list_preference_C1_C2_release.equals("")) {
            editor.putString("list_preference_C1_C2_release", "ZoomStop");
            editor.commit();
        }
        if (list_preference_5d_left.equals("")) {
            editor.putString("list_preference_5d_left", "CamLeft");
            editor.commit();
        }
        if (list_preference_5d_right.equals("")) {
            editor.putString("list_preference_5d_right", "CamRight");
            editor.commit();
        }
        if (list_preference_5d_up.equals("")) {
            editor.putString("list_preference_5d_up", "CamUp");
            editor.commit();
        }
        if (list_preference_5d_down.equals("")) {
            editor.putString("list_preference_5d_down", "CamDown");
            editor.commit();
        }
        if (list_preference_5d_push.equals("")) {
            editor.putString("list_preference_5d_push", "CamResetToMiddle");
            editor.commit();
        }
        if (list_preference_5d_middle.equals("")) {
            editor.putString("list_preference_5d_middle", "CamStop");
            editor.commit();
        }

        sIntent = new Intent(this, DjiAppMainServiceImpl.class);
        sConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                MainActivity.this.onMainServiceConnected(name, binder);
            }

            public void onServiceDisconnected(ComponentName name) {
                MainActivity.this.onMainServiceDisconnected();
            }
        };

     //   primaryVideoFeedView = (VideoViewFragment) findViewById(R.id.video_view_primary_video_feed);
      //  btnSimulator = (Button) findViewById(R.id.btn_simulator);
      //  btnSimulator.setOnClickListener(v -> {
      //      appMainService.startSimulator();
      //  });

        wideButton = (Button) findViewById(R.id.wide);
        wideButton.setOnClickListener(v -> {
            appMainService.wide();
        });


        zoomButton = (Button) findViewById(R.id.zoom);
        zoomButton.setOnClickListener(v -> {
            appMainService.zoom();
        });

        bindRc = (Button) findViewById(R.id.btn_rc);
        bindRc.setOnClickListener(v -> {
            appMainService.bindRC();
        });
       // bindCustomMedia = (Button) findViewById(R.id.btn_custom_media);
       // bindCustomMedia.setOnClickListener(v -> {
       //     appMainService.setMedia("Custom media info");
       // });

        fpvWidget = (FPVWidget)findViewById(R.id.fpv_custom_widget);
        fpvOverlayWidget = (FPVOverlayWidget) findViewById(R.id.fpv_overlay_widget);
        secondaryFpvWidget = (FPVWidget)findViewById(R.id.secondary_fpv_custom_widget);

        bindWidgets = (Button) findViewById(R.id.switch_button_primary);
        bindWidgets.setOnClickListener(v -> {
            if (!switched) {
                fpvWidget.setVideoSource(FPVWidget.VideoSource.SECONDARY);
                secondaryFpvWidget.setVideoSource(FPVWidget.VideoSource.PRIMARY);
            } else {
                fpvWidget.setVideoSource(FPVWidget.VideoSource.PRIMARY);
                secondaryFpvWidget.setVideoSource(FPVWidget.VideoSource.SECONDARY);
            }
            switched = !switched;
        });

        btnPreferences = findViewById(R.id.btn_preferences);
        btnPreferences.setOnClickListener((v -> {
            Intent intent = new Intent(this, PreferencesActivity.class);
            startActivity(intent);
        }));
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