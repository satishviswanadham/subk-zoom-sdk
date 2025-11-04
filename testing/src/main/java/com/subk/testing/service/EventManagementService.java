package com.subk.testing.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.gson.reflect.TypeToken;
import com.subk.testing.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import co.subk.zoomsdk.event.CeFormAnswerDataEvent;
import co.subk.zoomsdk.event.IsAnalyticsBoxChecked;
import co.subk.zoomsdk.event.LocationEvent;
import co.subk.zoomsdk.event.SessionEndedEvent;
import co.subk.zoomsdk.event.SessionJoinedEvent;
import co.subk.zoomsdk.event.ShareScreenEvent;
import co.subk.zoomsdk.meeting.models.CeFormQuestion;

public class EventManagementService extends Service {
    private static final String TAG = EventManagementService.class.getName();
    List<CeFormQuestion> questionResponses;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "STARTING " + TAG);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        Log.i(TAG, "STOPPING " + TAG);
        super.onDestroy();
//        stopMeetingService();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Subscribe
    public void onSessionStarted(SessionJoinedEvent sessionJoinedEvent) {
        Toast.makeText(this, "SessionJoinedEvent received with sessionId : " + sessionJoinedEvent.sessionId, Toast.LENGTH_SHORT).show();
    }

    @Subscribe
    public void onSessionEnded(SessionEndedEvent sessionEndedEvent) {
        Toast.makeText(this, "SessionEndedEvent received", Toast.LENGTH_SHORT).show();
    }

    @Subscribe
    public void onShareLocationEventReceived(LocationEvent locationEvent) {
        Toast.makeText(this, "Location updated : " + locationEvent.latitude + ", " + locationEvent.longitude + " with " + locationEvent.accuracy + " accuracy ", Toast.LENGTH_LONG).show();
    }

    @Subscribe
    public void onShareScreenEventReceived(ShareScreenEvent shareScreenEvent) {
        Toast.makeText(this, "Screen sharing started", Toast.LENGTH_LONG).show();
    }

    @Subscribe
    public void onCeFormAnswerEventReceived(CeFormAnswerDataEvent ceFormAnswerDataEvent) {
        String jsonData = Utils.loadJSONFromAsset(this, "questions2.json");
        Type listType = TypeToken.getParameterized(List.class, CeFormQuestion.class).getType();
        Log.i(TAG, "onCeFormAnswerEventReceived: ");
//        questionResponses = new Gson().fromJson(jsonData, listType);
        if (questionResponses == null) {
            EventBus.getDefault().post(new ArrayList<>());
            Log.e("print answer", "onAnswerEventReceived: " + 0 );
        } else {
            EventBus.getDefault().post(questionResponses);
        }
    }
    @Subscribe
    public void onRunVideoAnalytics(IsAnalyticsBoxChecked is_boxChecked) {
        if (is_boxChecked.status) {
            Log.i(TAG, "onRunVideoAnalytics: "+is_boxChecked);
        }
    }

}
