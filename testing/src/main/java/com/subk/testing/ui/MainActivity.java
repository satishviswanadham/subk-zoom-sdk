package com.subk.testing.ui;

import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_ALLOW_TO_CE_FORM_CAPTURE_DATA;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_ALLOW_TO_END_MEETING;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_ALLOW_TO_GET_LOCATION;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_ALLOW_TO_HIDE_VIDEO;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_ALLOW_TO_INVITE_ATTENDEE;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_ALLOW_TO_MUTE_AUDIO;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_ALLOW_TO_SHARE_SCREEN;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_ALLOW_TO_TAKE_SCREENSHOT;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_PASSWORD;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_CE_FORM_QUESTION_ANSWER_LIST;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_RENDER_TYPE;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_SESSION_NAME;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_SHOW_CONSENT;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_SHOW_END_MEETING_DIALOG;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_TOKEN;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_USERNAME;
import static co.subk.zoomsdk.ZoomSdkHelper.RENDER_TYPE_ZOOMRENDERER;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.subk.testing.R;
import com.subk.testing.Utils;
import com.subk.testing.databinding.ActivityMainBinding;
import com.subk.testing.service.EventManagementService;


import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import co.subk.zoomsdk.MeetingActivity;
import co.subk.zoomsdk.meeting.models.CeFormQuestion;

public class MainActivity extends AppCompatActivity {
    protected final static int REQUEST_VIDEO_AUDIO_CODE = 1010;

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    List<CeFormQuestion> questionResponses;
    boolean showMeetingDialog = false;


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        String jsonData = Utils.getJsonFromAssets(this, R.raw.questions);
        Type listType =  TypeToken.getParameterized(List.class, CeFormQuestion.class).getType();
        questionResponses = new Gson().fromJson(jsonData,listType);
        ArrayList<Meetings> lists = new ArrayList<>(
                Arrays.asList(
                        new Meetings("R1230","Disbursement_Confirmation_View"),
                        new Meetings("R1220","Disbursement_Confirmation_Record"),
                        new Meetings("R1210","Balance_Confirmation_View"),
                        new Meetings("R1200","Balance_Confirmation_Record"),
                        new Meetings("R1100","Loan_Appraise_Video"),
                        new Meetings("R0660","Loan_Collection_Authentication"),
                        new Meetings("R3924","Loan_Appraise")
            )
        );

        for (Meetings meeting: lists) {
            if (meeting.getName().equalsIgnoreCase("Loan_Appraise")) {
                showMeetingDialog = true;
                break;
            }
        }

        Log.i("TAG", "onCreate: showmeeting"+showMeetingDialog);
        Log.i("print ques", "onCreate: " + questionResponses);
        binding.fab.setOnClickListener(view -> {
            String sessionName = "VHV for 123456";
            String name = "Murali VVN (3954)";
            String password = "789789789";
            String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhcHBfa2V5Ijoib0tNc1JJSWFNbElIeUZ3TkM1dzJBUG12clVLNkdEU1p5T2RJIiwicm9sZV90eXBlIjoxLCJ0cGMiOiJWSFYgZm9yIDEyMzQ1NiIsInZlcnNpb24iOjEsImlhdCI6MTc2MjMzNDAxOSwiZXhwIjoxNzYyMzQxMjE5LCJ1c2VyX2lkZW50aXR5IjoiNTAyMyIsInNlc3Npb25fa2V5IjoiZGJkMDE4YzItODgyYS00MmJkLWIyY2QtZTdkNjBhMjk5OTNlIiwicHdkIjoiNzg5Nzg5Nzg5IiwiY2xvdWRfcmVjb3JkaW5nX29wdGlvbiI6MCwiY2xvdWRfcmVjb3JkaW5nX2VsZWN0aW9uIjoxfQ.hxl-ZY4c4pf_5VBle4XjKVEbzthJKGtQbUmFX-KHHkY";

            Intent intent = new Intent(MainActivity.this, MeetingActivity.class);
            intent.putExtra(PARAM_USERNAME, name);
            intent.putExtra(PARAM_TOKEN, token);
            intent.putExtra(PARAM_PASSWORD, password);
            intent.putExtra(PARAM_SESSION_NAME, sessionName);
            intent.putExtra(PARAM_RENDER_TYPE, RENDER_TYPE_ZOOMRENDERER);
            intent.putExtra(PARAM_ALLOW_TO_INVITE_ATTENDEE, true);
            intent.putExtra(PARAM_SHOW_CONSENT, true);
            intent.putExtra(PARAM_ALLOW_TO_SHARE_SCREEN, true);
            intent.putExtra(PARAM_ALLOW_TO_MUTE_AUDIO, true);
            intent.putExtra(PARAM_ALLOW_TO_HIDE_VIDEO, true);
            intent.putExtra(PARAM_ALLOW_TO_END_MEETING, true);
            intent.putExtra(PARAM_ALLOW_TO_TAKE_SCREENSHOT, true);
            intent.putExtra(PARAM_ALLOW_TO_GET_LOCATION, true);
            intent.putExtra(PARAM_ALLOW_TO_CE_FORM_CAPTURE_DATA, true);
            Log.i("TAG", "onCreate: showmeeting"+showMeetingDialog);
            intent.putExtra(PARAM_SHOW_END_MEETING_DIALOG, showMeetingDialog);
            intent.putExtra(PARAM_CE_FORM_QUESTION_ANSWER_LIST, new Gson().toJson(questionResponses));
            startActivity(intent);
        });

        requestPermission();



        startService(new Intent(this, EventManagementService.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopService(new Intent(this, EventManagementService.class));
    }

    protected void requestPermission() {

        String[] permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions = new String[]{
                    android.Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.BLUETOOTH_CONNECT
            };
        }

        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_VIDEO_AUDIO_CODE);
                return;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_VIDEO_AUDIO_CODE) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                    Build.VERSION.SDK_INT >= 31 &&
                    checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                onPermissionGranted();
            }
        }
    }

    public void onPermissionGranted() {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}