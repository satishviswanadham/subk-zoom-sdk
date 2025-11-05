package co.subk.zoomsdk.meeting;

import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_ALLOW_TO_END_MEETING;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_ALLOW_TO_GET_LOCATION;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_ALLOW_TO_HIDE_VIDEO;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_ALLOW_TO_INVITE_ATTENDEE;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_ALLOW_TO_MUTE_AUDIO;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_ALLOW_TO_SHARE_SCREEN;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_ALLOW_TO_TAKE_SCREENSHOT;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_CE_FORM_TYPE_MCQ;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_CE_FORM_TYPE_NUMBER;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_CE_FORM_TYPE_TEXT;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_MEETING_ENTITY_ID;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_PASSWORD;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_RENDER_TYPE;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_SESSION_NAME;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_SHOW_CONSENT;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_SHOW_END_MEETING_DIALOG;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_TASK_ID;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_TOKEN;
import static co.subk.zoomsdk.ZoomSdkHelper.PARAM_USERNAME;
import static co.subk.zoomsdk.ZoomSdkHelper.RENDER_TYPE_OPENGLES;
import static co.subk.zoomsdk.ZoomSdkHelper.RENDER_TYPE_ZOOMRENDERER;
import static co.subk.zoomsdk.ZoomSdkHelper.REQUEST_SELECT_ORIGINAL_PIC;
import static co.subk.zoomsdk.ZoomSdkHelper.REQUEST_SHARE_SCREEN_PERMISSION;
import static co.subk.zoomsdk.ZoomSdkHelper.REQUEST_SYSTEM_ALERT_WINDOW;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.location.LocationManager;
import android.media.projection.MediaProjectionManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.subk.zoomsdk.NetworkChangeReceiver;
import co.subk.zoomsdk.R;
import co.subk.zoomsdk.cmd.CmdHandler;
import co.subk.zoomsdk.cmd.CmdHelper;
import co.subk.zoomsdk.cmd.CmdLowerThirdRequest;
import co.subk.zoomsdk.cmd.CmdReactionRequest;
import co.subk.zoomsdk.cmd.CmdRequest;
import co.subk.zoomsdk.cmd.EmojiReactionType;
import co.subk.zoomsdk.event.CeFormAnswerDataEvent;
import co.subk.zoomsdk.event.InternetEvent;
import co.subk.zoomsdk.event.InviteAttendeeEvent;
import co.subk.zoomsdk.event.IsAnalyticsBoxChecked;
import co.subk.zoomsdk.event.LocationEvent;
import co.subk.zoomsdk.event.SessionEndedEvent;
import co.subk.zoomsdk.event.SessionJoinedEvent;
import co.subk.zoomsdk.event.ShareScreenEvent;
import co.subk.zoomsdk.meeting.feedback.data.FeedbackDataManager;
import co.subk.zoomsdk.meeting.feedback.view.FeedbackResultDialog;
import co.subk.zoomsdk.meeting.feedback.view.FeedbackSubmitDialog;
import co.subk.zoomsdk.meeting.models.CeFormAnswer;
import co.subk.zoomsdk.meeting.models.CeFormQuestion;
import co.subk.zoomsdk.meeting.notification.NotificationMgr;
import co.subk.zoomsdk.meeting.notification.NotificationService;
import co.subk.zoomsdk.meeting.screenshare.ShareToolbar;
import co.subk.zoomsdk.meeting.util.ErrorMsgUtil;
import co.subk.zoomsdk.meeting.util.NetworkUtil;
import co.subk.zoomsdk.meeting.util.SharePreferenceUtil;
import co.subk.zoomsdk.meeting.util.UserHelper;
import co.subk.zoomsdk.meeting.util.ZMAdapterOsBugHelper;
import co.subk.zoomsdk.meeting.view.ChatMsgAdapter;
import co.subk.zoomsdk.meeting.view.KeyBoardLayout;
import co.subk.zoomsdk.meeting.view.LowerThirdLayout;
import co.subk.zoomsdk.meeting.view.UserVideoAdapter;
import us.zoom.sdk.ZoomVideoSDK;
import us.zoom.sdk.ZoomVideoSDKAnnotationHelper;
import us.zoom.sdk.ZoomVideoSDKAudioHelper;
import us.zoom.sdk.ZoomVideoSDKAudioOption;
import us.zoom.sdk.ZoomVideoSDKAudioRawData;
import us.zoom.sdk.ZoomVideoSDKAudioStatus;
import us.zoom.sdk.ZoomVideoSDKCRCCallStatus;
import us.zoom.sdk.ZoomVideoSDKChatHelper;
import us.zoom.sdk.ZoomVideoSDKChatMessage;
import us.zoom.sdk.ZoomVideoSDKChatMessageDeleteType;
import us.zoom.sdk.ZoomVideoSDKChatPrivilegeType;
import us.zoom.sdk.ZoomVideoSDKDelegate;
import us.zoom.sdk.ZoomVideoSDKErrors;
import us.zoom.sdk.ZoomVideoSDKLiveStreamHelper;
import us.zoom.sdk.ZoomVideoSDKLiveStreamStatus;
import us.zoom.sdk.ZoomVideoSDKLiveTranscriptionHelper;
import us.zoom.sdk.ZoomVideoSDKMultiCameraStreamStatus;
import us.zoom.sdk.ZoomVideoSDKNetworkStatus;
import us.zoom.sdk.ZoomVideoSDKPasswordHandler;
import us.zoom.sdk.ZoomVideoSDKPhoneFailedReason;
import us.zoom.sdk.ZoomVideoSDKPhoneStatus;
import us.zoom.sdk.ZoomVideoSDKProxySettingHandler;
import us.zoom.sdk.ZoomVideoSDKRawDataPipe;
import us.zoom.sdk.ZoomVideoSDKRawDataPipeDelegate;
import us.zoom.sdk.ZoomVideoSDKRecordingConsentHandler;
import us.zoom.sdk.ZoomVideoSDKRecordingStatus;
import us.zoom.sdk.ZoomVideoSDKSSLCertificateInfo;
import us.zoom.sdk.ZoomVideoSDKSession;
import us.zoom.sdk.ZoomVideoSDKSessionContext;
import us.zoom.sdk.ZoomVideoSDKShareHelper;
import us.zoom.sdk.ZoomVideoSDKShareStatus;
import us.zoom.sdk.ZoomVideoSDKTestMicStatus;
import us.zoom.sdk.ZoomVideoSDKUser;
import us.zoom.sdk.ZoomVideoSDKUserHelper;
import us.zoom.sdk.ZoomVideoSDKVideoAspect;
import us.zoom.sdk.ZoomVideoSDKVideoCanvas;
import us.zoom.sdk.ZoomVideoSDKVideoHelper;
import us.zoom.sdk.ZoomVideoSDKVideoOption;
import us.zoom.sdk.ZoomVideoSDKVideoResolution;
import us.zoom.sdk.ZoomVideoSDKVideoStatisticInfo;
import us.zoom.sdk.ZoomVideoSDKVideoSubscribeFailReason;
import us.zoom.sdk.ZoomVideoSDKVideoView;


public class BaseMeetingActivity extends AppCompatActivity implements ZoomVideoSDKDelegate, ShareToolbar.Listener, KeyBoardLayout.KeyBoardListener
        , UserVideoAdapter.ItemTapListener, ChatMsgAdapter.ItemClickListener {

    protected static final String TAG = BaseMeetingActivity.class.getSimpleName();


    protected Display display;

    protected DisplayMetrics displayMetrics;

    protected RecyclerView userVideoList;

    protected LinearLayout videoListContain;

    protected UserVideoAdapter adapter;


    private Intent mScreenInfoData;

    protected ShareToolbar shareToolbar;

    protected ImageView iconShare;

    protected ImageView iconVideo;

    protected ImageView iconAudio;

    protected ImageView iconMore;

    protected TextView practiceText;

    protected TextView sessionNameText;

    // protected TextView mtvInput;

    protected ImageView iconLock;

    protected View actionBar;

    protected ScrollView actionBarScroll;

    protected View btnViewShare;

    protected KeyBoardLayout keyBoardLayout;

    protected RecyclerView chatListView;

    protected ProgressBar loader;

    private ChatMsgAdapter chatMsgAdapter;

    protected String myDisplayName = "";
    protected String meetingPwd = "";
    protected String sessionName;

    protected String taskId;
    protected String token = "";
    protected String meetingEntityId;

    protected boolean allowToInviteAttendee = false;
    protected boolean allowToShareScreen = false;

    protected boolean showConsent = false;
    protected boolean allowToMuteAudio = false;
    protected boolean allowToHideVideo = false;
    protected boolean allowToEndMeeting = false;
    protected boolean allowToTakeScreenshot = false;

    protected boolean allowToCaptureLocation = false;
    protected boolean showEndMeetingDialog = false;
    protected boolean allowToCaptureData = false;
    protected int renderType;

    protected ImageView videoOffView;

    private View shareViewGroup;

    private ImageView shareImageView;

    protected TextView text_fps;

    protected Handler handler = new Handler(Looper.getMainLooper());

    protected boolean isActivityPaused = false;

    protected ZoomVideoSDKUser mActiveUser;

    protected ZoomVideoSDKUser currentShareUser;

    protected ZoomVideoSDKSession session;

    protected boolean renderWithSurfaceView = true;

    protected boolean showCameraControl = false;

//    protected LinearLayout panelRecordBtn;
//    protected ZoomVideoSDKRecordingStatus status = ZoomVideoSDKRecordingStatus.Recording_Stop;

    private BroadcastReceiver mNetworkReceiver;
    private Dialog internetAlertDialog;

    int LOCATION_PERMISSION_ID = 44;

    ArrayList<LocationEvent> locationEvents = new ArrayList<>();
    @NonNull
    private List<CmdLowerThirdRequest> lowerThirdRequests = new ArrayList<>();

    private LowerThirdLayout lowerThirdLayout;

    protected CmdHandler emojiHandler = new CmdHandler() {
        @Override
        public void onCmdReceived(final CmdRequest request) {
            if (request instanceof CmdReactionRequest) {
                final CmdReactionRequest cmdReactionRequest = (CmdReactionRequest) request;
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        if (BaseMeetingActivity.this.isDestroyed()) {
                            return;
                        }
                        adapter.onEmojiReceived(cmdReactionRequest, userVideoList);
                    }
                });
            }
        }
    };

    protected CmdHandler lowerThirdHandler = new CmdHandler() {
        @Override
        public void onCmdReceived(CmdRequest request) {
            if (request instanceof CmdLowerThirdRequest) {
                final CmdLowerThirdRequest cmdLowerThirdRequest = (CmdLowerThirdRequest) request;
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        if (BaseMeetingActivity.this.isDestroyed()) {
                            return;
                        }
                        if (cmdLowerThirdRequest.user.equals(mActiveUser)) {
                            showLowerThird(cmdLowerThirdRequest);
                        }
                        boolean existRequest = false;
                        for (CmdLowerThirdRequest item : lowerThirdRequests) {
                            if (item.user.equals(cmdLowerThirdRequest.user)) {
                                item.name = cmdLowerThirdRequest.name;
                                item.companyName = cmdLowerThirdRequest.companyName;
                                item.rgb = cmdLowerThirdRequest.rgb;
                                existRequest = true;
                                break;
                            }
                        }
                        if (!existRequest) {
                            lowerThirdRequests.add(cmdLowerThirdRequest);
                        }
                    }
                });
            }
        }
    };

    boolean isVisitFirstTime = false;
    protected TextView ceFormQuestionText;
    protected RadioGroup ceFormRadioGroup;
    protected LinearLayout ceFormEnterAnswer;
    protected EditText ceFormEdittextAnswer;
    protected TextView ceFormBtnNext;
    protected TextView ceFormBtnPrev;
    protected LinearLayout ceFormQuestionLayout;
    protected ImageView ceFormClose;
    protected RelativeLayout ceAddLayoutAnswer;
    private List<CeFormQuestion> ceFormQuestions;
    private TextView ceFormSelectedAnswer = null;
    private int currentQuestionIndex = 0;
    private float dX, dY;
    protected Boolean responseFailed = false;
    protected String ceQuestionResponse = "";
    // Map to store answers with question IDs
    private Map<String, String> ceAnswersMap = new HashMap<>();

    protected LinearLayout manualConsentLayout;

    protected FusedLocationProviderClient locationProviderClient;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCeAnswerResponseReceived(List<CeFormQuestion> ceFormQuestionResponse) {
        // Handle the received question responses here
        // Update UI or perform any required actions
        if (!ceFormQuestionResponse.isEmpty()) {
            ceFormQuestions = ceFormQuestionResponse;
            responseFailed = false;
            Log.e("print ans response", "onQuestionResponseReceived: " + ceFormQuestions);
        } else {
            responseFailed = true;
        }

    }

    TextView text_end_meeting;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isVisitFirstTime = true;
        if (!renderWithSurfaceView) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }

        getWindow().addFlags(WindowManager.LayoutParams.
                FLAG_KEEP_SCREEN_ON);
        setContentView(getLayout());
        display = ((WindowManager) getSystemService(Service.WINDOW_SERVICE)).getDefaultDisplay();
        displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);

        this.locationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        setupZoom();

        session = ZoomVideoSDK.getInstance().getSession();
        ZoomVideoSDK.getInstance().addListener(BaseMeetingActivity.this);
        parseIntent();
        initView();
        initMeeting();
        updateSessionInfo();

        mNetworkReceiver = new NetworkChangeReceiver();
        registerNetworkBroadcastForNougat();

        /** Below 3 lned added by arul to mute audio and off video by default when join meeting*/
       /* ZoomVideoSDK.getInstance().getVideoHelper().stopVideo();
        ZoomVideoSDKUser zoomSDKUserInfo = session.getMySelf();
        ZoomVideoSDK.getInstance().getAudioHelper().muteAudio(zoomSDKUserInfo);*/

    }


    // Method to display the current question
    private void showQuestion(int index) {
        CeFormQuestion ceFormQuestion = ceFormQuestions.get(index);
//        ceFormQuestionText.setText(ceFormQuestion.getQuestionCode() + ". " + ceFormQuestion.getQuestion());
        ceFormQuestionText.setText(ceFormQuestion.getQuestion());

        // Clear existing options in the RadioGroup
        ceAddLayoutAnswer.removeAllViews();
        ceFormRadioGroup.removeAllViews();
        ceFormSelectedAnswer = null;
        // Update UI components based on the answer type
        switch (ceFormQuestion.getAnswerType()) {
            case PARAM_CE_FORM_TYPE_TEXT:
                if (ceFormQuestion.getAnswer() != null && !ceFormQuestion.getAnswer().isEmpty()) {
                    ceFormEdittextAnswer.setText(ceFormQuestion.getAnswer());
                } else {
                    ceFormEdittextAnswer.setText("");
                }
                ceAddLayoutAnswer.addView(ceFormEnterAnswer);
                ceFormEdittextAnswer.setInputType(InputType.TYPE_CLASS_TEXT);
                ceFormEdittextAnswer.setHint("Enter answer");
                break;
            case PARAM_CE_FORM_TYPE_NUMBER:
                if (ceFormQuestion.getAnswer() != null && !ceFormQuestion.getAnswer().isEmpty()) {
                    ceFormEdittextAnswer.setText(ceFormQuestion.getAnswer());
                } else {
                    ceFormEdittextAnswer.setText("");
                }
                ceAddLayoutAnswer.addView(ceFormEnterAnswer);
                ceFormEdittextAnswer.setInputType(InputType.TYPE_CLASS_NUMBER);
                ceFormEdittextAnswer.setHint("Enter number");
                break;
            case PARAM_CE_FORM_TYPE_MCQ:
                ceAddLayoutAnswer.addView(ceFormRadioGroup);
                for (int i = 0; i < ceFormQuestion.getAvailableAnswers().size(); i++) {
                    LinearLayout linearLayout = new LinearLayout(this);
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    layoutParams.setMargins(10, 15, 10, 15);
                    linearLayout.setLayoutParams(layoutParams);
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    TextView radioButton = (TextView) getLayoutInflater().inflate(R.layout.item_ce_form_answer, null);
                    radioButton.setText(ceFormQuestion.getAvailableAnswers().get(i));
                    radioButton.setId(i);
                    radioButton.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));

                    // Check if the current radio button matches the answer
                    if (ceFormQuestion.getAnswer() != null && !ceFormQuestion.getAnswer().isEmpty()
                            && ceFormQuestion.getAnswer().equals(ceFormQuestion.getAvailableAnswers().get(i))) {
                        // Set background for the selected answer
                        radioButton.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.bg_radio_button));
                        ceFormSelectedAnswer = (TextView) radioButton;
                    } else {
                        // Set background for other options
                        radioButton.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.bg_capture_data));
                    }
                    radioButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (ceFormSelectedAnswer != null) {
                                ceFormSelectedAnswer.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.bg_capture_data));
                            }
                            // Set background color of clicked TextView to green
                            view.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.bg_radio_button));
//                            Update the selected TextView
                            ceFormSelectedAnswer = (TextView) view;
                        }
                    });

                    linearLayout.addView(radioButton);
                    ceFormRadioGroup.addView(linearLayout);
                }
                break;
        }

        if (index == ceFormQuestions.size() - 1) {
            ceFormBtnNext.setText("Submit");
        } else {
            ceFormBtnNext.setText("Next");
        }

        ceFormQuestionLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;

                        // Restrict movement within activity bounds
                        if (newX > 0 && newX < (getWindow().getDecorView().getWidth() - view.getWidth())) {
                            view.animate()
                                    .x(newX)
                                    .setDuration(0)
                                    .start();
                        }
                        if (newY > 0 && newY < (getWindow().getDecorView().getHeight() - view.getHeight())) {
                            view.animate()
                                    .y(newY)
                                    .setDuration(0)
                                    .start();
                        }
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });
    }

    private void registerNetworkBroadcastForNougat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    protected void unregisterNetworkChanges() {
        try {
            unregisterReceiver(mNetworkReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void setupZoom() {
        Bundle bundle = getIntent().getExtras();
        String sessionName = "", name = "", password = "";
        if (null != bundle) {
            name = bundle.getString(PARAM_USERNAME);
            password = bundle.getString(PARAM_PASSWORD);
            sessionName = bundle.getString(PARAM_SESSION_NAME);
            taskId = bundle.getString(PARAM_TASK_ID);
            meetingEntityId = bundle.getString(PARAM_MEETING_ENTITY_ID);
            token = bundle.getString(PARAM_TOKEN);
        }

        ZoomVideoSDKAudioOption audioOption = new ZoomVideoSDKAudioOption();
        audioOption.connect = true;
        audioOption.mute = true; // it was false and i change it to true to mute mic when user join the meeting
        audioOption.isMyVoiceInMix = true;

        ZoomVideoSDKVideoOption videoOption = new ZoomVideoSDKVideoOption();
        videoOption.localVideoOn = false; // it was true and i change it to false to Off Camera when user join the meeting


        ZoomVideoSDKSessionContext sessionContext = new ZoomVideoSDKSessionContext();
        sessionContext.audioOption = audioOption;
        sessionContext.videoOption = videoOption;
        sessionContext.sessionName = sessionName;
        sessionContext.userName = name;
        sessionContext.token = token;
        sessionContext.sessionPassword = password;
        sessionContext.sessionIdleTimeoutMins = 40;

        ZoomVideoSDKSession session = ZoomVideoSDK.getInstance().joinSession(sessionContext);

        // Once the meeting starts, send a broadcast
        Intent intent = new Intent("co.subk.sarthi.MEETING_STARTED");
        sendBroadcast(intent);


        if (null == session) {
            Log.i(BaseMeetingActivity.class.getName(), "Session name :" + sessionContext.sessionName);
            Log.i(BaseMeetingActivity.class.getName(), "User name :" + sessionContext.userName);
            Log.i(BaseMeetingActivity.class.getName(), "Token :" + sessionContext.token);
            Log.i(BaseMeetingActivity.class.getName(), "Session password :" + sessionContext.sessionPassword);
            Log.e(BaseMeetingActivity.class.getName(), "Session is NULL");
        }
    }

    DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
        }

        @Override
        public void onDisplayChanged(int displayId) {
            refreshRotation();
        }

        @Override
        public void onDisplayRemoved(int displayId) {
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        parseIntent();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityPaused = true;
        unSubscribe();
        adapter.clear(false);
        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        displayManager.unregisterDisplayListener(mDisplayListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        unregisterReceiver(apiResponseReceiver);  // Unregister when activity stops
    }

    protected void parseIntent() {
        Bundle bundle = getIntent().getExtras();
        if (null != bundle) {
            myDisplayName = bundle.getString(PARAM_USERNAME);
            meetingPwd = bundle.getString(PARAM_PASSWORD);
            sessionName = bundle.getString(PARAM_SESSION_NAME);
            taskId = bundle.getString(PARAM_TASK_ID);
            meetingEntityId = bundle.getString(PARAM_MEETING_ENTITY_ID);
            renderType = bundle.getInt(PARAM_RENDER_TYPE, RENDER_TYPE_ZOOMRENDERER);
            allowToInviteAttendee = bundle.getBoolean(PARAM_ALLOW_TO_INVITE_ATTENDEE);
            allowToShareScreen = bundle.getBoolean(PARAM_ALLOW_TO_SHARE_SCREEN);
            showConsent = bundle.getBoolean(PARAM_SHOW_CONSENT);
            allowToMuteAudio = bundle.getBoolean(PARAM_ALLOW_TO_MUTE_AUDIO);
            allowToHideVideo = bundle.getBoolean(PARAM_ALLOW_TO_HIDE_VIDEO);
            allowToEndMeeting = bundle.getBoolean(PARAM_ALLOW_TO_END_MEETING);
            allowToTakeScreenshot = bundle.getBoolean(PARAM_ALLOW_TO_TAKE_SCREENSHOT);
            allowToCaptureLocation = bundle.getBoolean(PARAM_ALLOW_TO_GET_LOCATION);
            showEndMeetingDialog = bundle.getBoolean(PARAM_SHOW_END_MEETING_DIALOG);
            Log.i(TAG, "parseIntent: showEndMeetingDialog"+showEndMeetingDialog);

           // allowToCaptureData = bundle.getBoolean(PARAM_ALLOW_TO_CE_FORM_CAPTURE_DATA);
           // ceQuestionResponse = bundle.getString(PARAM_CE_FORM_QUESTION_ANSWER_LIST);
        }
    }

    // Receive API response and process it in the meeting activity
    private BroadcastReceiver apiResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
           // Toast.makeText(context, "Get Call Back form Main App ", Toast.LENGTH_SHORT).show();
            String apiResponse = intent.getStringExtra("API_RESPONSE");
            // Handle the API response inside the meeting activity
            processApiResponse(apiResponse);
        }
    };

    private void processApiResponse(String apiResponse) {
        // Logic to handle the API response in the meeting activity

         allowToCaptureData = true;
         ceQuestionResponse = apiResponse;

        if (!ceQuestionResponse.isEmpty()) {
            if (ceFormQuestions == null) {
                ceFormQuestions = new Gson().fromJson(ceQuestionResponse, TypeToken.getParameterized(List.class, CeFormQuestion.class).getType());
                showQuestion(currentQuestionIndex);

                // Restore saved answers when the activity is created

                if (currentQuestionIndex > 0) {
                    ceFormBtnPrev.setBackgroundResource(R.drawable.bg_button);
                    ceFormBtnPrev.setEnabled(true);
                } else {
                    ceFormBtnPrev.setBackgroundResource(R.drawable.bg_button_disable);
                    ceFormBtnPrev.setEnabled(false);
                }

                ceFormBtnPrev.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (currentQuestionIndex > 0) {
                            // Move to the previous question
                            currentQuestionIndex--;

                            // Show the previous question
                            showQuestion(currentQuestionIndex);

                            // Restore the previously entered answer
                            restorePreviousAnswer();
                            ceFormBtnPrev.setBackgroundResource(R.drawable.bg_button);
                            ceFormBtnPrev.setEnabled(true);
                            // If the current question index is 0, disable the previous button
                            if (currentQuestionIndex == 0) {
                                ceFormBtnPrev.setBackgroundResource(R.drawable.bg_button_disable);
                                ceFormBtnPrev.setEnabled(false);
                            }
                        }
                    }
                });

                ceFormClose.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ceFormQuestionLayout.setVisibility(View.GONE);
                    }
                });
                ceFormBtnNext.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String answer = "";

                        // Get the current question
                        CeFormQuestion ceFormQuestion = ceFormQuestions.get(currentQuestionIndex);
                        String questionId = ceFormQuestion.getId();

                        switch (ceFormQuestion.getAnswerType()) {
                            case PARAM_CE_FORM_TYPE_TEXT:
                            case PARAM_CE_FORM_TYPE_NUMBER:
                                answer = ceFormEdittextAnswer.getText().toString().trim();
                                break;
                            case PARAM_CE_FORM_TYPE_MCQ:
                                if (ceFormSelectedAnswer != null) {
                                    answer = ceFormSelectedAnswer.getText().toString().trim();
                                }
                                break;
                            default:
                                answer = "";
                        }

                       /* if (!answer.equalsIgnoreCase(""))
                        {
                            Toast.makeText(v.getContext(), "Khali Koni", Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            Toast.makeText(v.getContext(), "Khali Hai", Toast.LENGTH_LONG).show();
                        }*/

                        if(!answer.isEmpty()) {
                            // Store the answer in the map
                            ceAnswersMap.put(questionId, answer);

                            // Create QuestionAnswer object and add to list
                            List<CeFormAnswer> answers = new ArrayList<>();
                            answers.add(new CeFormAnswer(questionId, answer));

                            // Post event to pass the answer list
                            EventBus.getDefault().post(new CeFormAnswerDataEvent(taskId, token, answers));

                        }
                        else
                        {
                            // If the answer is empty, remove the entry from the map
                            ceAnswersMap.remove(questionId);
                        }

                        // Move to the next question or hide the form if there are no more questions
                        if (currentQuestionIndex < ceFormQuestions.size() - 1) {
                            // Move to the next question
                            currentQuestionIndex++;
                            showQuestion(currentQuestionIndex);
                            // Restore the previously entered answer
                            restorePreviousAnswer();
                            ceFormBtnPrev.setBackgroundResource(R.drawable.bg_button);
                            ceFormBtnPrev.setEnabled(true);
                        } else {
                            // Check if any answer is empty
                            boolean anyAnswerEmpty = false;
                            for (CeFormQuestion question : ceFormQuestions) {
                                String qId = question.getId();
                                if (!ceAnswersMap.containsKey(qId) || ceAnswersMap.get(qId).isEmpty()) {
                                    anyAnswerEmpty = true;
                                    break;
                                }
                            }

                            if (anyAnswerEmpty) {
                                Toast.makeText(BaseMeetingActivity.this, "Please answer all questions", Toast.LENGTH_SHORT).show();
                            } else {
                                // Hide the form if all questions are answered
                                ceFormQuestionLayout.setVisibility(View.GONE);
//                                allowToCaptureData = false;
                            }
                        }

                    }
                });
            }

        }

        //Log.d("MeetingActivity", "API response received: " + apiResponse);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isActivityPaused) {
            resumeSubscribe();
        }
        isActivityPaused = false;
        refreshRotation();
        updateActionBarLayoutParams();
        updateChatLayoutParams();

        if (ZoomVideoSDK.getInstance().isInSession()) {
            int size = UserHelper.getAllUsers().size();
            if (size > 0 && adapter.getItemCount() == 0) {
                adapter.addAll();
                updateVideoListLayout();
                refreshUserListAdapter();
            }
        }
        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        displayManager.registerDisplayListener(mDisplayListener, handler);
    }

    protected void resumeSubscribe() {
        if (null != currentShareUser) {
            subscribeShareByUser(currentShareUser);
        } else if (null != mActiveUser) {
            subscribeVideoByUser(mActiveUser);
        }

        if (ZoomVideoSDK.getInstance().isInSession()) {
            List<ZoomVideoSDKUser> userInfoList = UserHelper.getAllUsers();
            if (null != userInfoList && userInfoList.size() > 0) {
                List<ZoomVideoSDKUser> list = new ArrayList<>(userInfoList.size());
                for (ZoomVideoSDKUser userInfo : userInfoList) {
                    list.add(userInfo);
                }
                adapter.onUserJoin(list);
                selectAndScrollToUser(mActiveUser);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateFpsOrientation();
        refreshRotation();
        updateActionBarLayoutParams();
        updateChatLayoutParams();
        updateSmallVideoLayoutParams();

        adapter.notifyDataSetChanged();
        adapter.isOrientationChanges();
    }

    private void updateFpsOrientation() {
        text_fps.setVisibility(View.GONE);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            text_fps = findViewById(R.id.text_fps_landscape);
        } else {
            text_fps = findViewById(R.id.text_fps);
        }
        if (ZoomVideoSDK.getInstance().isInSession()) {
            text_fps.setVisibility(View.GONE); // Was Visible Here Change to GOne as per requirement on 08/07/23 @arul
        }
    }


    private void updateSmallVideoLayoutParams() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            videoListContain.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        } else {
            videoListContain.setGravity(Gravity.CENTER);
        }
    }

    private void updateChatLayoutParams() {
        if (chatMsgAdapter.getItemCount() > 0) {
            chatListView.scrollToPosition(chatMsgAdapter.getItemCount() - 1);
        }
    }

    private void updateActionBarLayoutParams() {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) actionBar.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            params.topMargin = (int) (35 * displayMetrics.scaledDensity);
//            params.gravity = Gravity.RIGHT | Gravity.BOTTOM;
//            params.bottomMargin = (int) (22 * displayMetrics.scaledDensity);
            actionBarScroll.scrollTo(0, 0);
        } else {
            params.topMargin = 0;
//            params.gravity = Gravity.RIGHT | Gravity.BOTTOM;
//            params.bottomMargin = getResources().getDimensionPixelSize(R.dimen.toolbar_bottom_margin);
        }
        actionBar.setLayoutParams(params);

    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onStart() {
        super.onStart();

        // Register to receive API response from BaseWebViewActivity
        IntentFilter filter = new IntentFilter("co.subk.sarthi.SEND_RESPONSE_TO_MEETING");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(apiResponseReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }


        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        unregisterNetworkChanges();
        if (null != shareToolbar) {
            shareToolbar.destroy();
        }
        if (ZMAdapterOsBugHelper.getInstance().isNeedListenOverlayPermissionChanged()) {
            ZMAdapterOsBugHelper.getInstance().stopListenOverlayPermissionChange(this);
        }
        ZoomVideoSDK.getInstance().removeListener(this);
        adapter.onDestroyed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_SHARE_SCREEN_PERMISSION:
                if (resultCode != RESULT_OK) {
                    break;
                }
                startShareScreen(data);
                break;
            case REQUEST_SYSTEM_ALERT_WINDOW:
                if (ZMAdapterOsBugHelper.getInstance().isNeedListenOverlayPermissionChanged()) {
                    ZMAdapterOsBugHelper.getInstance().stopListenOverlayPermissionChange(this);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if ((!Settings.canDrawOverlays(this)) && (!ZMAdapterOsBugHelper.getInstance().isNeedListenOverlayPermissionChanged() || !ZMAdapterOsBugHelper.getInstance().ismCanDraw())) {
                        return;
                    }
                }
                onStartShareScreen(mScreenInfoData);
                break;
            case REQUEST_SELECT_ORIGINAL_PIC: {
                if (resultCode == RESULT_OK) {
                    try {
                        Uri selectedImage = data.getData();
                        if (null != selectedImage) {
                            if (currentShareUser == null) {
                                shareImageView.setImageURI(selectedImage);
                                shareViewGroup.setVisibility(View.VISIBLE);
                                int ret = ZoomVideoSDK.getInstance().getShareHelper().startShareView(shareImageView);
                                if (ret == ZoomVideoSDKErrors.Errors_Success) {
                                    onStartShareView();
                                } else {
                                    shareImageView.setImageBitmap(null);
                                    shareViewGroup.setVisibility(View.GONE);
                                    boolean isLocked = ZoomVideoSDK.getInstance().getShareHelper().isShareLocked();
                                    Toast.makeText(this, "Share Fail isLocked=" + isLocked + " ret:" + ret, Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(this, "Other is sharing", Toast.LENGTH_LONG).show();
                            }

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
        }
    }

    protected void onStartShareView() {

    }

    public void onClickStopShare(View view) {
        ZoomVideoSDK.getInstance().getShareHelper().stopShare();
    }

    public void onSingleTap(ZoomVideoSDKUser user) {
        List<ZoomVideoSDKUser> all = UserHelper.getAllUsers();

        if (all.contains(user)) {
            all.remove(user);
        }

        adapter.RefreshMyList(all);

        subscribeVideoByUser(user);
    }

    protected void onUserActive(ZoomVideoSDKUser user) {
        CmdLowerThirdRequest cmdLowerThirdRequest = null;
        for (CmdLowerThirdRequest request : lowerThirdRequests) {
            if (request.user.equals(user)) {
                cmdLowerThirdRequest = request;
                break;
            }
        }
        showLowerThird(cmdLowerThirdRequest);
    }

    private void showLowerThird(@Nullable CmdLowerThirdRequest request) {
        if (request != null && SharePreferenceUtil.readBoolean(this, LowerThirdSettingBottomFragment.LOWER_THIRD_KEY, false)) {
            lowerThirdLayout.setVisibility(View.GONE); // set to GONE here to remove this ViewGroup from UI
            lowerThirdLayout.updateNameTv(request.name);
            lowerThirdLayout.updateCompanyTv(request.companyName);
            lowerThirdLayout.updateColor(request.rgb);
        } else {
            lowerThirdLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onKeyBoardChange(boolean isShow, int height, int inputHeight) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) chatListView.getLayoutParams();

        if (isShow) {
            params.gravity = Gravity.START | Gravity.BOTTOM;
            params.bottomMargin = getResources().getDimensionPixelSize(R.dimen.dp_13) + height + inputHeight;
        } else {
            params.gravity = Gravity.START | Gravity.BOTTOM;
            params.bottomMargin = getResources().getDimensionPixelSize(R.dimen.dp_160);
        }
        chatListView.setLayoutParams(params);
        if (chatMsgAdapter.getItemCount() > 0) {
            chatListView.scrollToPosition(chatMsgAdapter.getItemCount() - 1);
        }
    }

    protected void onStartShareScreen(Intent data) {
        if (null == shareToolbar) {
            shareToolbar = new ShareToolbar(this, this);
        }

        EventBus.getDefault().post(new ShareScreenEvent());
        if (Build.VERSION.SDK_INT >= 29) {
            //MediaProjection  need service with foregroundServiceType mediaProjection in android Q
            boolean hasForegroundNotification = NotificationMgr.hasNotification(getApplicationContext(), NotificationMgr.PT_NOTIFICATION_ID);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Bundle args = new Bundle();
                Intent intent = new Intent(getApplicationContext(), NotificationService.class);
                args.putInt(NotificationService.ARG_COMMAND_TYPE, NotificationService.COMMAND_MEDIA_PROJECTION_START);
                intent.putExtra(NotificationService.ARGS_EXTRA, args);
                intent.setClassName(getPackageName(), "co.subk.zoomsdk.meeting.notification.NotificationService");
                startForegroundService(intent);
            } else {
                if (!hasForegroundNotification) {
                    Intent intent = new Intent(this, NotificationService.class);
                    startForegroundService(intent);
                }
            }
        }

        int ret = ZoomVideoSDK.getInstance().getShareHelper().startShareScreen(data);
        if (ret == ZoomVideoSDKErrors.Errors_Success) {
            /**Added by arul to switch Camera Status*/
            switchVideoStatusOnScreenShare();
            shareToolbar.showToolbar();
            showDesktop();
        }
    }

    protected void showDesktop() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(home);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void onClickStopShare() {
        if (ZoomVideoSDK.getInstance().getShareHelper().isSharingOut()) {
            ZoomVideoSDK.getInstance().getShareHelper().stopShare();
            showMeetingActivity();
        }
    }

    private void showMeetingActivity() {
        Intent intent = new Intent(getApplicationContext(), IntegrationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.setAction(IntegrationActivity.ACTION_RETURN_TO_CONF);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
    }

    @SuppressLint("NewApi")
    protected void startShareScreen(Intent data) {
        if (data == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 24 && !Settings.canDrawOverlays(this)) {
            if (ZMAdapterOsBugHelper.getInstance().isNeedListenOverlayPermissionChanged())
                ZMAdapterOsBugHelper.getInstance().startListenOverlayPermissionChange(this);
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            mScreenInfoData = data;
            startActivityForResult(intent, REQUEST_SYSTEM_ALERT_WINDOW);
        } else {
            onStartShareScreen(data);
        }
    }

    protected void refreshRotation() {
        int displayRotation = display.getRotation();
        Log.d(TAG, "rotateVideo:" + displayRotation);
        ZoomVideoSDK.getInstance().getVideoHelper().rotateMyVideo(displayRotation);
    }

    public void updateFps(final ZoomVideoSDKVideoStatisticInfo statisticInfo) {
        if (null == statisticInfo) {
            return;
        }
        final int fps = statisticInfo.getFps();
        text_fps.post(new Runnable() {
            @Override
            public void run() {
                if (statisticInfo.getWidth() > 0 && statisticInfo.getHeight() > 0) {
                    text_fps.setVisibility(View.GONE); // Was Visible Here Change to GOne as per requirement on 08/07/23 @arul
                    String text = statisticInfo.getWidth() + "X" + statisticInfo.getHeight() + " " + fps + " FPS";
                    if (fps < 10) {
                        text = statisticInfo.getWidth() + "X" + statisticInfo.getHeight() + "  " + fps + " FPS";
                    }
                    text_fps.setText(text);
                } else {
                    text_fps.setVisibility(View.GONE);
                }
            }
        });
    }


    protected void initView() {
        loader = findViewById(R.id.loader);
        text_end_meeting = findViewById(R.id.text_end_meeting);
        showLoader();

        sessionNameText = findViewById(R.id.sessionName);

        sessionNameText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(BaseMeetingActivity.this, "Test CLick", Toast.LENGTH_SHORT).show();

                captureScreenshot(getWindow().getDecorView().getRootView());
            }
        });
        // mtvInput = findViewById(R.id.tv_input);
        userVideoList = findViewById(R.id.userVideoList);
        videoListContain = findViewById(R.id.video_list_contain);
        adapter = new UserVideoAdapter(this, this, renderType);
        userVideoList.setItemViewCacheSize(0);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        layoutManager.setItemPrefetchEnabled(false);
        userVideoList.setLayoutManager(layoutManager);
        userVideoList.setAdapter(adapter);

        text_fps = findViewById(R.id.text_fps);

        iconVideo = findViewById(R.id.icon_video);
        if (allowToHideVideo) {
            iconVideo.setVisibility(View.VISIBLE);
        } else {
            iconVideo.setVisibility(View.GONE);
        }
        iconAudio = findViewById(R.id.icon_audio);
        if (allowToMuteAudio) {
            iconAudio.setVisibility(View.VISIBLE);
        } else {
            iconAudio.setVisibility(View.GONE);
        }

        if (!allowToTakeScreenshot) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        }

        iconMore = findViewById(R.id.icon_more);
        practiceText = findViewById(R.id.text_meeting_user_size);

        keyBoardLayout = findViewById(R.id.chat_input_layout);

        chatListView = findViewById(R.id.chat_list);

        chatListView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        chatMsgAdapter = new ChatMsgAdapter(this);
        chatListView.setAdapter(chatMsgAdapter);

        keyBoardLayout.setKeyBoardListener(this);
        actionBar = findViewById(R.id.action_bar);

        iconLock = findViewById(R.id.meeting_lock_status);

        iconShare = findViewById(R.id.icon_share);
        if (allowToShareScreen) {
            iconShare.setVisibility(View.VISIBLE);
        } else {
            iconShare.setVisibility(View.GONE);
        }

        actionBarScroll = findViewById(R.id.action_bar_scroll);

        videoOffView = findViewById(R.id.video_off_tips);

        btnViewShare = findViewById(R.id.btn_view_share);

        shareViewGroup = findViewById(R.id.share_view_group);
        shareImageView = findViewById(R.id.share_image);

//        panelRecordBtn = findViewById(R.id.panelRecordBtn);

        lowerThirdLayout = findViewById(R.id.layout_lower_third);
        lowerThirdLayout.setVisibility(View.GONE);

        ceFormQuestionLayout = findViewById(R.id.ce_form_question_layout);
        ceFormQuestionText = findViewById(R.id.ce_form_question_text);
        ceAddLayoutAnswer = findViewById(R.id.ce_add_layout_answer);
        ceFormBtnNext = findViewById(R.id.ce_form_btn_next);
        ceFormBtnPrev = findViewById(R.id.ce_form_btn_prev);
        ceFormEnterAnswer = findViewById(R.id.ce_form_enter_answer);
        ceFormEdittextAnswer = findViewById(R.id.ce_form_edittext_answer);
        ceFormRadioGroup = findViewById(R.id.ce_form_radio_group);
        ceFormClose = findViewById(R.id.ce_form_close);

        manualConsentLayout = findViewById(R.id.manual_consent_layout);

        //just added commmne to publilsh new version of zoom
        /*if (!ceQuestionResponse.isEmpty()) {
            if (ceFormQuestions == null) {
                ceFormQuestions = new Gson().fromJson(ceQuestionResponse, TypeToken.getParameterized(List.class, CeFormQuestion.class).getType());
                showQuestion(currentQuestionIndex);

                // Restore saved answers when the activity is created

                if (currentQuestionIndex > 0) {
                    ceFormBtnPrev.setBackgroundResource(R.drawable.bg_button);
                    ceFormBtnPrev.setEnabled(true);
                } else {
                    ceFormBtnPrev.setBackgroundResource(R.drawable.bg_button_disable);
                    ceFormBtnPrev.setEnabled(false);
                }

                ceFormBtnPrev.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (currentQuestionIndex > 0) {
                            // Move to the previous question
                            currentQuestionIndex--;

                            // Show the previous question
                            showQuestion(currentQuestionIndex);

                            // Restore the previously entered answer
                            restorePreviousAnswer();
                            ceFormBtnPrev.setBackgroundResource(R.drawable.bg_button);
                            ceFormBtnPrev.setEnabled(true);
                            // If the current question index is 0, disable the previous button
                            if (currentQuestionIndex == 0) {
                                ceFormBtnPrev.setBackgroundResource(R.drawable.bg_button_disable);
                                ceFormBtnPrev.setEnabled(false);
                            }
                        }
                    }
                });

                ceFormClose.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ceFormQuestionLayout.setVisibility(View.GONE);
                    }
                });
                ceFormBtnNext.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String answer = "";

                        // Get the current question
                        CeFormQuestion ceFormQuestion = ceFormQuestions.get(currentQuestionIndex);
                        String questionId = ceFormQuestion.getId();

                        switch (ceFormQuestion.getAnswerType()) {
                            case PARAM_CE_FORM_TYPE_TEXT:
                            case PARAM_CE_FORM_TYPE_NUMBER:
                                answer = ceFormEdittextAnswer.getText().toString().trim();
                                break;
                            case PARAM_CE_FORM_TYPE_MCQ:
                                if (ceFormSelectedAnswer != null) {
                                    answer = ceFormSelectedAnswer.getText().toString().trim();
                                }
                                break;
                            default:
                                answer = "";
                        }

                        // Store the current answer
                        ceAnswersMap.put(questionId, answer);
                       *//* if (!answer.equalsIgnoreCase(""))
                        {
                            Toast.makeText(v.getContext(), "Khali Koni", Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            Toast.makeText(v.getContext(), "Khali Hai", Toast.LENGTH_LONG).show();
                        }*//*

                        if(!answer.isEmpty()) {
                            // Store the answer in the map
                            ceAnswersMap.put(questionId, answer);

                        // Move to the next question or check all answers if it's the last question
                            // Create QuestionAnswer object and add to list
                            List<CeFormAnswer> answers = new ArrayList<>();
                            answers.add(new CeFormAnswer(questionId, answer));

                            // Post event to pass the answer list
                            EventBus.getDefault().post(new CeFormAnswerDataEvent(taskId, token, answers));

                        }
                        else
                        {
                            // If the answer is empty, remove the entry from the map
                            ceAnswersMap.remove(questionId);
                        }

                        // Move to the next question or hide the form if there are no more questions
                        if (currentQuestionIndex < ceFormQuestions.size() - 1) {
                            // Move to the next question
                            currentQuestionIndex++;
                            showQuestion(currentQuestionIndex);
                            restorePreviousAnswer();
                            // Restore the previously entered answer
                            restorePreviousAnswer();
                            ceFormBtnPrev.setBackgroundResource(R.drawable.bg_button);
                            ceFormBtnPrev.setEnabled(true);
                        } else {
                            // Check if any answer is empty
                            boolean anyAnswerEmpty = false;
                            for (CeFormQuestion question : ceFormQuestions) {
                                String qId = question.getId();
                                String savedAnswer = ceAnswersMap.get(qId);
                                if (savedAnswer == null || savedAnswer.trim().isEmpty()) {
                                    anyAnswerEmpty = true;
                                    break;
                                }
                            }

                            if (anyAnswerEmpty) {
                                Toast.makeText(BaseMeetingActivity.this, "Please answer all questions", Toast.LENGTH_SHORT).show();
                            } else {
                                // Hide the form if all questions are answered
                                ceFormQuestionLayout.setVisibility(View.GONE);
                                // allowToCaptureData = false;
                            }
                        }
                    }
                });

            }

        }*/

        onKeyBoardChange(false, 0, 30);
        final int margin = (int) (5 * displayMetrics.scaledDensity);
        userVideoList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.set(margin, 0, margin, 0);
            }
        });

        userVideoList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    LinearLayoutManager linearLayoutManager = (LinearLayoutManager) userVideoList.getLayoutManager();
                    View view = linearLayoutManager.getChildAt(0);
                    if (null == view) {
                        return;
                    }
                    int index = linearLayoutManager.findFirstVisibleItemPosition();
                    int left = view.getLeft();
                    if (left < 0) {
                        if (-left > view.getWidth() / 2) {
                            index = index + 1;
                            if (index == adapter.getItemCount() - 1) {
                                recyclerView.scrollBy(view.getWidth(), 0);
                            } else {
                                recyclerView.scrollBy(view.getWidth() + left + 2 * margin, 0);
                            }
                        } else {
                            recyclerView.scrollBy(left - margin, 0);
                        }
                        if (index == 0) {
                            recyclerView.scrollTo(0, 0);
                        }
                    }
                    view = linearLayoutManager.getChildAt(0);
                    if (null == view) {
                        return;
                    }
                    scrollVideoViewForMargin(view);

                }
            }
        });

        if (showConsent) {
            onStartMeetingConsent();
        }


        text_end_meeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // Toast.makeText(BaseMeetingActivity.this, "Close button ", Toast.LENGTH_SHORT).show();


                ZoomVideoSDKUser userInfo = session.getMySelf();

                final Dialog builder = new Dialog(BaseMeetingActivity.this, R.style.MyDialog);
                builder.setCanceledOnTouchOutside(false);
                builder.setCancelable(false);
                builder.setContentView(R.layout.dialog_band_karo_meeting);

                TextView btn_cancel_button = builder.findViewById(R.id.btn_cancel_button_naya);
                CheckBox run_video_analytics_immeditly = builder.findViewById(R.id.run_video_analytics_immeditly);
                LinearLayout videoMeetingAnalytics = builder.findViewById(R.id.videoMeetingAnalytics);

                if (showEndMeetingDialog) {
                    videoMeetingAnalytics.setVisibility(View.VISIBLE);
                } else {
                    videoMeetingAnalytics.setVisibility(View.GONE);
                }

                if (view.getId() == R.id.text_end_meeting) {
//                    ((TextView) builder.findViewById(R.id.txt_leave_session_new)).setText(getString(R.string.leave_message));
                } else {
                    ((TextView) builder.findViewById(R.id.txt_leave_session_new)).setText(getString(R.string.consent_decline_message));
                }
                builder.findViewById(R.id.btn_leave_naya).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (showEndMeetingDialog) {
                            if (run_video_analytics_immeditly.isChecked()) {
                                builder.dismiss();
                                releaseResource();
                                int ret = ZoomVideoSDK.getInstance().leaveSession(false);
                                Log.d(TAG, "leaveSession ret = " + ret);
                                EventBus.getDefault().post(new IsAnalyticsBoxChecked(true));
                            } else {
                                Toast.makeText(BaseMeetingActivity.this, "Please check the box to run video analytics", Toast.LENGTH_SHORT).show();
                            }
                        }else {
                            builder.dismiss();
                            releaseResource();
                            int ret = ZoomVideoSDK.getInstance().leaveSession(false);
                            Log.d(TAG, "leaveSession ret = " + ret);
                        }

                    }
                });

                if (view.getId() == R.id.text_end_meeting) {
                    builder.findViewById(R.id.btn_end_naya).setVisibility(View.VISIBLE);
                } else {
                    builder.findViewById(R.id.btn_end_naya).setVisibility(View.GONE);
                }

                boolean end = false;
                if (null != userInfo && userInfo.isHost() && allowToEndMeeting) {
                    builder.findViewById(R.id.btn_end_naya).setVisibility(View.VISIBLE);
                    ((TextView) builder.findViewById(R.id.btn_end_naya)).setText(getString(R.string.leave_end_text));
                    end = true;
                }
                else
                {
                    builder.findViewById(R.id.btn_end_naya).setVisibility(View.GONE);
                }
                final boolean endSession = end;

                builder.findViewById(R.id.btn_end_naya).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (showEndMeetingDialog) {
                            if (run_video_analytics_immeditly.isChecked()) {
                                builder.dismiss();
                                if (endSession) {
                                    releaseResource();
                                    int ret = ZoomVideoSDK.getInstance().leaveSession(true);
                                    Log.d(TAG, "leaveSession ret = " + ret);
                                }
                                EventBus.getDefault().post(new IsAnalyticsBoxChecked(true));
                            } else {
                                Toast.makeText(BaseMeetingActivity.this, "Please check the box to run video analytics", Toast.LENGTH_SHORT).show();
                            }
                        }else {
                            builder.dismiss();
                            if (endSession) {
                                releaseResource();
                                int ret = ZoomVideoSDK.getInstance().leaveSession(true);
                                Log.d(TAG, "leaveSession ret = " + ret);
                            }
                        }
                    }
                });

                // builder.findViewById(R.id.btn_cancel).setVisibility(View.VISIBLE);
                /*builder.findViewById(R.id.btn_cancel)*/btn_cancel_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        builder.dismiss();
                    }
                });

                builder.show();


            }
        });

    }

    private void restorePreviousAnswer() {
        CeFormQuestion ceFormQuestion = ceFormQuestions.get(currentQuestionIndex);
        String questionId = ceFormQuestion.getId();

        // Check if the answer for the current question is stored in the map
        if (ceAnswersMap.containsKey(questionId)) {
            String previousAnswer = ceAnswersMap.get(questionId);

            switch (ceFormQuestion.getAnswerType()) {
                case PARAM_CE_FORM_TYPE_TEXT:
                case PARAM_CE_FORM_TYPE_NUMBER:
                    // Set the previous text answer to the EditText
                    ceFormEdittextAnswer.setText(previousAnswer);
                    break;
                case PARAM_CE_FORM_TYPE_MCQ:
                    // Set the previous selected answer to the appropriate TextView
                    /*if (previousAnswer != null) {
                        for (int i = 0; i < ceFormRadioGroup.getChildCount(); i++) {
                            View childView = ceFormRadioGroup.getChildAt(i);
                            if (childView instanceof TextView) {
                                TextView radioButton = (TextView) childView;
                                if (radioButton.getText().toString().equals(previousAnswer)) {
                                    radioButton.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.bg_radio_button));
                                    ceFormSelectedAnswer = radioButton;
                                    break;
                                }
                            }
                        }
                    }*/

                    if (previousAnswer != null) {
                        for (int i = 0; i < ceFormRadioGroup.getChildCount(); i++) {
                            View childView = ceFormRadioGroup.getChildAt(i);

                            if (childView instanceof LinearLayout) {
                                LinearLayout linearLayout = (LinearLayout) childView;

                                for (int j = 0; j < linearLayout.getChildCount(); j++) {
                                    View nestedChild = linearLayout.getChildAt(j);

                                    if (nestedChild instanceof TextView) {
                                        TextView optionTextView = (TextView) nestedChild;

                                        if (previousAnswer.equals(optionTextView.getText().toString())) {
                                            // Set the background or other selection indication
                                            optionTextView.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.bg_radio_button));
                                            ceFormSelectedAnswer = optionTextView;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }


                    break;
            }
        } else {
            // If there is no previous answer stored, clear the EditText
            ceFormEdittextAnswer.setText("");
        }
    }

    protected void initMeeting() {
    }

    public void showLoader() {
        loader.setVisibility(View.VISIBLE);
    }

    public void hideLoader() {
        loader.setVisibility(View.GONE);
    }

    @Override
    public void onItemClick() {

    }

    public void onClickSwitchShare(View view) {

    }

    protected int getLayout() {
        return 0;
    }

    public void onClickInfo(View view) {
        final Dialog builder = new Dialog(this, R.style.MyDialog);
        builder.setContentView(R.layout.dialog_session_info);

        final TextView sessionNameText = builder.findViewById(R.id.info_session_name);
        final TextView sessionPwdText = builder.findViewById(R.id.info_session_pwd);
        final TextView sessionUserSizeText = builder.findViewById(R.id.info_user_size);
        int size = UserHelper.getAllUsers().size();
        if (size <= 0) {
            size = 1;
        }
        sessionUserSizeText.setText(size + "");

        ZoomVideoSDKSession sessionInfo = ZoomVideoSDK.getInstance().getSession();
        meetingPwd = sessionInfo.getSessionPassword();
        sessionPwdText.setText(meetingPwd);

        if (TextUtils.isEmpty(meetingPwd)) {
            sessionPwdText.setText("Not set");
            sessionPwdText.setTextColor(getResources().getColor(R.color.color_not_set));
        }

        String name = sessionInfo.getSessionName();
        if (null == name) {
            name = "";
        }
        sessionNameText.setText(name);
        builder.setCanceledOnTouchOutside(true);
        builder.setCancelable(true);
        builder.show();
    }

    public void onClickYes(View view) {
        manualConsentLayout.setVisibility(View.GONE);
    }

    public void onClickEnd(View view) {
        ZoomVideoSDKUser userInfo = session.getMySelf();

        final Dialog builder = new Dialog(this, R.style.MyDialog);
        builder.setCanceledOnTouchOutside(false);
        builder.setCancelable(false);
        builder.setContentView(R.layout.dialog_leave_alert);

        TextView btn_cancel_button = findViewById(R.id.btn_cancel_button);


        if (view.getId() == R.id.text_end_meeting) {
            ((TextView) builder.findViewById(R.id.txt_leave_session)).setText(getString(R.string.leave_message));
        } else {
            ((TextView) builder.findViewById(R.id.txt_leave_session)).setText(getString(R.string.consent_decline_message));
        }
        builder.findViewById(R.id.btn_leave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                builder.dismiss();
                releaseResource();
                int ret = ZoomVideoSDK.getInstance().leaveSession(false);
                Log.d(TAG, "leaveSession ret = " + ret);
            }
        });

        if (view.getId() == R.id.text_end_meeting) {
            builder.findViewById(R.id.btn_end).setVisibility(View.VISIBLE);
        } else {
            builder.findViewById(R.id.btn_end).setVisibility(View.GONE);
        }

        boolean end = false;
        if (null != userInfo && userInfo.isHost() && allowToEndMeeting) {
            builder.findViewById(R.id.btn_end).setVisibility(View.VISIBLE);
            ((TextView) builder.findViewById(R.id.btn_end)).setText(getString(R.string.leave_end_text));
            end = true;
        }
        else
        {
            builder.findViewById(R.id.btn_end).setVisibility(View.GONE);
        }
        final boolean endSession = end;

        builder.findViewById(R.id.btn_end).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                builder.dismiss();
                if (endSession) {
                    releaseResource();
                    int ret = ZoomVideoSDK.getInstance().leaveSession(true);
                    Log.d(TAG, "leaveSession ret = " + ret);
                }
            }
        });


       /* if (btn_cancel_button == null) {
            Toast.makeText(this, "Button cancel is null ", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(this, "Not null ", Toast.LENGTH_SHORT).show();
        }

        btn_cancel_button.setVisibility(View.VISIBLE);
        btn_cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                builder.dismiss();
            }
        });*/
       // builder.findViewById(R.id.btn_cancel).setVisibility(View.VISIBLE);
        builder.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                builder.dismiss();
            }
        });

        builder.show();

    }

    public void onStartMeetingConsent() {
        final Dialog builder = new Dialog(this, R.style.MyDialog);
        builder.setCanceledOnTouchOutside(false);
        builder.setCancelable(false);
        /*DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMet9rics.heightPixels;nb,mn,m
        int width = displayMetrics.widthPixels;*/
        //builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.setContentView(R.layout.dialog_leave_alert);
        // builder.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, (int) (height*0.5));
        ((TextView) builder.findViewById(R.id.txt_leave_session)).setText(getString(R.string.call_is_being_recorded_please_take_consent/*did_the_customer_s_consent_to_recording_this_call*/));
        ((TextView) builder.findViewById(R.id.btn_leave)).setText(getString(R.string.no));
        ((TextView) builder.findViewById(R.id.btn_leave)).setVisibility(View.GONE);
        builder.findViewById(R.id.btn_cancel_button).setVisibility(View.GONE);
        builder.findViewById(R.id.btn_leave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                builder.dismiss();
                //onClickEnd(view);
            }
        });

        ((TextView) builder.findViewById(R.id.btn_end)).setText(getString(R.string.ok));
        builder.findViewById(R.id.btn_end).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                builder.dismiss();
                onClickYes(view);
            }
        });
        builder.show();
    }

    private void releaseResource() {
        unSubscribe();
        adapter.clear(true);
        actionBar.setVisibility(View.GONE);
        ceFormQuestionLayout.setVisibility(View.GONE);
        // mtvInput.setVisibility(View.GONE);
    }

    public void onClickVideo(View view) {
        ZoomVideoSDKUser zoomSDKUserInfo = session.getMySelf();
        if (null == zoomSDKUserInfo)
            return;
        if (zoomSDKUserInfo.getVideoStatus().isOn()) {
            ZoomVideoSDK.getInstance().getVideoHelper().stopVideo();
        } else {
            ZoomVideoSDK.getInstance().getVideoHelper().startVideo();
        }
    }

    public void switchVideoStatusOnScreenShare() {
        ZoomVideoSDKUser zoomSDKUserInfo = session.getMySelf();
        if (null == zoomSDKUserInfo)
            return;
        if (zoomSDKUserInfo.getVideoStatus().isOn()) {
            ZoomVideoSDK.getInstance().getVideoHelper().stopVideo();
        } else {
            /** Commented by arul because of this when user share screen from android then in web camera view is visible not screen - 28/nov/24*/
           // ZoomVideoSDK.getInstance().getVideoHelper().startVideo();
        }
    }

    public void onClickShare(View view) {
        ZoomVideoSDKShareHelper sdkShareHelper = ZoomVideoSDK.getInstance().getShareHelper();

        boolean isShareLocked = sdkShareHelper.isShareLocked();
        if (isShareLocked && !session.getMySelf().isHost()) {
            Toast.makeText(this, "Share is locked by host", Toast.LENGTH_SHORT).show();
            return;
        }

        if (null != currentShareUser && currentShareUser != session.getMySelf()) {
            Toast.makeText(this, "Other is shareing", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentShareUser == session.getMySelf()) {
            sdkShareHelper.stopShare();
            /**Added by arul to switch Camera Status*/
            switchVideoStatusOnScreenShare();
            return;
        }

        final Dialog builder = new Dialog(this, R.style.MyDialog);

        builder.setContentView(R.layout.dialog_share_view);
        builder.setCanceledOnTouchOutside(true);
        builder.setCancelable(true);
        builder.findViewById(R.id.group_screen_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.dismiss();
                if (ZoomVideoSDK.getInstance().getShareHelper().isSharingOut()) {
                    ZoomVideoSDK.getInstance().getShareHelper().stopShare();
                    if (null != shareToolbar) {
                        shareToolbar.destroy();
                    }
                } else {
                    askScreenSharePermission();
                }
            }
        });

        builder.findViewById(R.id.group_picture_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFromGallery();
                builder.dismiss();
            }
        });
        builder.show();
    }


    private void selectFromGallery() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_SELECT_ORIGINAL_PIC);
    }

    protected void toggleView(boolean show) {

    }

    public void onClickChat(View view) {
        keyBoardLayout.showChat();
        toggleView(true);
    }

    public void onClickAudio(View view) {
        ZoomVideoSDKUser zoomSDKUserInfo = session.getMySelf();
        if (null == zoomSDKUserInfo)
            return;
        if (zoomSDKUserInfo.getAudioStatus().getAudioType() == ZoomVideoSDKAudioStatus.ZoomVideoSDKAudioType.ZoomVideoSDKAudioType_None) {
            ZoomVideoSDK.getInstance().getAudioHelper().startAudio();
        } else {
            if (zoomSDKUserInfo.getAudioStatus().isMuted()) {
                ZoomVideoSDK.getInstance().getAudioHelper().unMuteAudio(zoomSDKUserInfo);
            } else {
                ZoomVideoSDK.getInstance().getAudioHelper().muteAudio(zoomSDKUserInfo);
            }
        }
    }

    public void onClickMoreSpeaker() {
        boolean speaker = ZoomVideoSDK.getInstance().getAudioHelper().getSpeakerStatus();
        ZoomVideoSDK.getInstance().getAudioHelper().setSpeaker(!speaker);
    }

    public void onClickMoreSwitchCamera() {
        ZoomVideoSDKUser zoomSDKUserInfo = session.getMySelf();
        if (null == zoomSDKUserInfo)
            return;
        if (zoomSDKUserInfo.getVideoStatus().isHasVideoDevice() && zoomSDKUserInfo.getVideoStatus().isOn()) {
            ZoomVideoSDK.getInstance().getVideoHelper().switchCamera();
            refreshRotation();
        }
    }

//    private void onClickStartCloudRecord() {
//        int error = ZoomVideoSDK.getInstance().getRecordingHelper().startCloudRecording();
//        if (error != ZoomVideoSDKErrors.Errors_Success) {
//            Toast.makeText(this, "start cloud record error: " + ErrorMsgUtil.getMsgByErrorCode(error) + ". Error code: " + error, Toast.LENGTH_LONG).show();
//        }
//    }

    private boolean isSpeakerOn() {
        return ZoomVideoSDK.getInstance().getAudioHelper().getSpeakerStatus();
    }

    public void onClickMore(View view) {
        ZoomVideoSDKUser zoomSDKUserInfo = session.getMySelf();
        if (null == zoomSDKUserInfo)
            return;
        final Dialog builder = new Dialog(this, R.style.MyDialog);
        builder.setContentView(R.layout.dialog_more_action);

        final View llSwitchCamera = builder.findViewById(R.id.llSwitchCamera);
        final View llSpeaker = builder.findViewById(R.id.llSpeaker);
        final View llStartRecord = builder.findViewById(R.id.llStartRecord);
        final View llRecording = builder.findViewById(R.id.llRecordStatus);
        final View llFeedback = builder.findViewById(R.id.llFeedback);
        View llInviteAttendee = builder.findViewById(R.id.llinviteattendee);
        final View llCaptureData = builder.findViewById(R.id.llCaptureData);
        final TextView tvFeedback = builder.findViewById(R.id.tvFeedback);
        final TextView tvSpeaker = builder.findViewById(R.id.tvSpeaker);
        final ImageView ivSpeaker = builder.findViewById(R.id.ivSpeaker);

        llInviteAttendee.setOnClickListener(view1 -> {
            builder.dismiss();
            EventBus.getDefault().post(new InviteAttendeeEvent(taskId));
        });

        if (allowToCaptureData) {
            llCaptureData.setVisibility(View.VISIBLE);
        } else {
            llCaptureData.setVisibility(View.GONE);
        }

        llCaptureData.setOnClickListener(view1 -> {
            builder.dismiss();
            ceFormQuestionLayout.setVisibility(View.VISIBLE);
        });

        if (allowToInviteAttendee) {
            llInviteAttendee.setVisibility(View.VISIBLE);
        } else {
            llInviteAttendee.setVisibility(View.GONE);
        }

        if (zoomSDKUserInfo.getVideoStatus().isOn()) {
            llSwitchCamera.setVisibility(View.VISIBLE);
            llSwitchCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    builder.dismiss();
                    onClickMoreSwitchCamera();
                }
            });
        } else {
            llSwitchCamera.setVisibility(View.GONE);
        }
        if (canSwitchAudioSource()) {
            llSpeaker.setVisibility(View.VISIBLE);
            llSpeaker.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    builder.dismiss();
                    onClickMoreSpeaker();
                }
            });
        } else {
            llSpeaker.setVisibility(View.GONE);
        }

        llRecording.setVisibility(View.GONE);
        llStartRecord.setVisibility(View.GONE);
//        if (
//                canStartRecord() && status != ZoomVideoSDKRecordingStatus.Recording_DiskFull) {
//            if (status == ZoomVideoSDKRecordingStatus.Recording_Stop) {
//                llStartRecord.setVisibility(View.VISIBLE);
//                llStartRecord.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        builder.dismiss();
//                        onClickStartCloudRecord();
//                    }
//                });
//            } else {
//                llRecording.setVisibility(View.VISIBLE);
//                final ImageView recordImg = llRecording.findViewById(R.id.imgRecording);
//                final ImageView pauseRecordImg = llRecording.findViewById(R.id.btn_pause_record);
//                final ImageView stopRecordImg = llRecording.findViewById(R.id.btn_stop_record);
////                final ProgressBar startRecordProgressBar = llRecording.findViewById(R.id.progressStartingRecord);
//                final TextView recordStatus = llRecording.findViewById(R.id.txtRecordStatus);
//
//                pauseRecordImg.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        if (status == ZoomVideoSDKRecordingStatus.Recording_Pause) {
//                            int error = ZoomVideoSDK.getInstance().getRecordingHelper().resumeCloudRecording();
//                            if (error == ZoomVideoSDKErrors.Errors_Success) {
//                                pauseRecordImg.setImageResource(R.drawable.zm_record_btn_pause);
//                                recordImg.setVisibility(View.VISIBLE);
//                                recordStatus.setText("Recording…");
//                            } else {
//                                Toast.makeText(BaseMeetingActivity.this, "resume cloud record error: " + ErrorMsgUtil.getMsgByErrorCode(error) + ". Error code: "+error, Toast.LENGTH_LONG).show();
//                            }
//                        } else {
//                            int error = ZoomVideoSDK.getInstance().getRecordingHelper().pauseCloudRecording();
//                            if (error == ZoomVideoSDKErrors.Errors_Success) {
//                                pauseRecordImg.setImageResource(R.drawable.zm_record_btn_resume);
//                                recordImg.setVisibility(View.GONE);
//                                recordStatus.setText("Recording Paused");
//                            } else {
//                                Toast.makeText(BaseMeetingActivity.this, "pause cloud record error: " + ErrorMsgUtil.getMsgByErrorCode(error) + ". Error code: "+error, Toast.LENGTH_LONG).show();
//                            }
//                        }
//                    }
//                });
//
//
//                stopRecordImg.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        int error = ZoomVideoSDK.getInstance().getRecordingHelper().stopCloudRecording();
//                        if (error == ZoomVideoSDKErrors.Errors_Success) {
//                            builder.dismiss();
//                        } else {
//                            Toast.makeText(BaseMeetingActivity.this, "stop cloud record error: " + ErrorMsgUtil.getMsgByErrorCode(error) + ". Error code: "+error, Toast.LENGTH_LONG).show();
//                        }
//                    }
//                });
//
//                if (status == ZoomVideoSDKRecordingStatus.Recording_Pause) {
//                    recordImg.setVisibility(View.GONE);
//                    recordStatus.setText("Recording Paused");
//                } else {
//                    recordImg.setVisibility(View.VISIBLE);
//                    recordStatus.setText("Recording…");
//                }
//
//                pauseRecordImg.setVisibility(View.VISIBLE);
//                stopRecordImg.setVisibility(View.VISIBLE);
//                pauseRecordImg.setImageResource(status == ZoomVideoSDKRecordingStatus.Recording_Pause ? R.drawable.zm_record_btn_resume : R.drawable.zm_record_btn_pause);
////                startRecordProgressBar.setVisibility(View.GONE);
//            }
//        }

        if (isSpeakerOn()) {
            tvSpeaker.setText("Turn off Speaker");
            ivSpeaker.setImageResource(R.drawable.icon_speaker_off);
        } else {
            tvSpeaker.setText("Turn on Speaker");
            ivSpeaker.setImageResource(R.drawable.icon_speaker_on);
        }

        showRaiseHand(builder);
        showEmojiPanel(builder);
        showLowerThirdBtn(builder);
        String feedbackText;
        if (ZoomVideoSDK.getInstance().getSession().getMySelf().isHost()) {
            int count = FeedbackDataManager.getInstance().getFeedbackCount();
            feedbackText = getResources().getString(R.string.more_feedbacks);
            if (count > 0) {
                feedbackText += "(" + count + ")";
            }
            tvFeedback.setText("");
        } else {
            feedbackText = getResources().getString(R.string.more_feedback_session);
        }
        tvFeedback.setText(feedbackText);
        llFeedback.setVisibility(View.GONE); // added this to hide feedback section
        llFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ZoomVideoSDK.getInstance().getSession().getMySelf().isHost()) {
                    FeedbackResultDialog.show(BaseMeetingActivity.this);
                } else {
                    FeedbackSubmitDialog.show(BaseMeetingActivity.this);
                }
                builder.dismiss();
            }
        });

        if (showCameraControl) {
            builder.findViewById(R.id.camera_control).setVisibility(View.VISIBLE);
           /* builder.findViewById(R.id.btn_request).setOnClickListener(cameraControlListener);
            builder.findViewById(R.id.btn_give_up).setOnClickListener(cameraControlListener);
            builder.findViewById(R.id.btn_left).setOnClickListener(cameraControlListener);
            builder.findViewById(R.id.btn_right).setOnClickListener(cameraControlListener);
            builder.findViewById(R.id.btn_up).setOnClickListener(cameraControlListener);
            builder.findViewById(R.id.btn_down).setOnClickListener(cameraControlListener);
            builder.findViewById(R.id.btn_zoom_in).setOnClickListener(cameraControlListener);
            builder.findViewById(R.id.btn_zoom_out).setOnClickListener(cameraControlListener);*/
        }

        builder.setCanceledOnTouchOutside(true);
        builder.setCancelable(true);
        builder.show();
    }

   /* private View.OnClickListener cameraControlListener = new View.OnClickListener() {
        @SuppressLint("NonConstantResourceId")
        @Override
        public void onClick(View v) {
            List<ZoomVideoSDKUser> users = session.getRemoteUsers();
            if (null == users || users.isEmpty()) {
                return;
            }
            ZoomVideoSDKUser user = feccUser;
            int ret=0;
            if (null == user && v.getId() != R.id.btn_request) {
                Toast.makeText(BaseMeetingActivity.this, "need request and approve ", Toast.LENGTH_SHORT).show();
                return;
            }
            int range = 100;
            switch (v.getId()) {
                case R.id.btn_request: {
                    if (null == user) {
                        user = users.get(0);
                    }
                    ret=user.getRemoteCameraControlHelper().requestControlRemoteCamera();
                    break;
                }
                case R.id.btn_give_up: {
                    ret=user.getRemoteCameraControlHelper().giveUpControlRemoteCamera();
                    if (ret == 0) {
                        feccUser = null;
                    }
                    break;
                }
                case R.id.btn_left: {
                    ret=user.getRemoteCameraControlHelper().turnLeft(range);
                    break;
                }
                case R.id.btn_right: {
                    ret=user.getRemoteCameraControlHelper().turnRight(range);
                    break;
                }
                case R.id.btn_up: {
                    ret=user.getRemoteCameraControlHelper().turnUp(range);
                    break;
                }
                case R.id.btn_down: {
                    ret= user.getRemoteCameraControlHelper().turnDown(range);
                    break;
                }
                case R.id.btn_zoom_in: {
                    ret=user.getRemoteCameraControlHelper().zoomIn(range);
                    break;
                }
                case R.id.btn_zoom_out: {
                    ret=user.getRemoteCameraControlHelper().zoomOut(range);
                    break;
                }
            }
            Toast.makeText(BaseMeetingActivity.this,"ret:"+ret,Toast.LENGTH_SHORT).show();
        }
    };
*/

    private void checkMoreAction() {
        ZoomVideoSDKUser zoomSDKUserInfo = session.getMySelf();
        if (null == zoomSDKUserInfo)
            return;
        // SET THIS TO VISIBLE FOR SHOWING MORE BUTTON IN SIDE RIBBON
        iconMore.setVisibility(View.VISIBLE);
    }


    private boolean canSwitchAudioSource() {
        return ZoomVideoSDK.getInstance().getAudioHelper().canSwitchSpeaker();

    }


//    private boolean canStartRecord() {
//        return ZoomVideoSDK.getInstance().getRecordingHelper().canStartRecording() == ZoomVideoSDKErrors.Errors_Success;
//    }

    private void showRaiseHand(final Dialog dialog) {
        final LinearLayout llRaiseHand = dialog.findViewById(R.id.llRaiseHand);
        final TextView tvRaiseHand = dialog.findViewById(R.id.tvRaiseHand);
        final ImageView ivRaiseHand = dialog.findViewById(R.id.ivRaiseHand);
        if (adapter.isHandRaised()) {
            tvRaiseHand.setText("Low Hand");
            ivRaiseHand.setImageResource(R.drawable.low_hand);
        } else {
            tvRaiseHand.setText("Raise Hand");
            ivRaiseHand.setImageResource(R.drawable.raise_hand);
        }
        llRaiseHand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CmdReactionRequest request = new CmdReactionRequest();
                request.user = null;
                request.reactionType = adapter.isHandRaised() ? EmojiReactionType.LowHand : EmojiReactionType.RaisedHand;
                CmdHelper.getInstance().sendCommand(request);
                dialog.dismiss();
            }
        });
    }

    private void showEmojiPanel(@NonNull final Dialog dialog) {
        /*View.OnClickListener listener = new View.OnClickListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public void onClick(View v) {
                EmojiReactionType type = EmojiReactionType.None;
                switch (v.getId()) {
                    case R.id.btnClap:
                        type = EmojiReactionType.Clap;
                        break;
                    case R.id.btnThumbup:
                        type = EmojiReactionType.Thumbsup;
                        break;
                    case R.id.btnHeart:
                        type = EmojiReactionType.Heart;
                        break;
                    case R.id.btnJoy:
                        type = EmojiReactionType.Joy;
                        break;
                    case R.id.btnOpenMouth:
                        type = EmojiReactionType.Openmouth;
                        break;
                    case R.id.btnTada:
                        type = EmojiReactionType.Tada;
                        break;
                }
                CmdReactionRequest cmdReactionRequest = new CmdReactionRequest();
                cmdReactionRequest.user = null;
                cmdReactionRequest.reactionType = type;
                CmdHelper.getInstance().sendCommand(cmdReactionRequest);
                dialog.dismiss();
            }
        };*/
        /*dialog.findViewById(R.id.btnClap).setOnClickListener(listener);
        dialog.findViewById(R.id.btnThumbup).setOnClickListener(listener);
        dialog.findViewById(R.id.btnHeart).setOnClickListener(listener);
        dialog.findViewById(R.id.btnJoy).setOnClickListener(listener);
        dialog.findViewById(R.id.btnOpenMouth).setOnClickListener(listener);
        dialog.findViewById(R.id.btnTada).setOnClickListener(listener);*/
        dialog.findViewById(R.id.llEmojis).setBackground(getDrawable(R.drawable.more_action_last_bg));
    }

    private void showLowerThirdBtn(@NonNull final Dialog dialog) {
        final View llLowerThird = dialog.findViewById(R.id.llLowerThird);
        llLowerThird.setVisibility(View.GONE); // setting this to GONE to hide it on stakeholders request
        llLowerThird.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LowerThirdSettingBottomFragment fragment = LowerThirdSettingBottomFragment.newInstance();
                fragment.lowerThirdDisableListener = new LowerThirdSettingBottomFragment.LowerThirdDisableListener() {
                    @Override
                    public void onLowerThirdDisabled() {
                        showLowerThird(null);
                    }

                    @Override
                    public void onLowerThirdEnabled() {
                        onUserActive(mActiveUser);
                    }
                };
                fragment.show(BaseMeetingActivity.this.getSupportFragmentManager(), "LowerThirdSettingBottomFragment");
                dialog.dismiss();
            }
        });
    }

    @SuppressLint("NewApi")
    protected void askScreenSharePermission() {
        if (ZoomVideoSDK.getInstance().getShareHelper().isSharingOut()) {
            return;
        }
        MediaProjectionManager mgr = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mgr != null) {
            Intent intent = mgr.createScreenCaptureIntent();
            try {
                startActivityForResult(intent, REQUEST_SHARE_SCREEN_PERMISSION);
            } catch (Exception e) {
                Log.e(TAG, "askScreenSharePermission failed");
            }
        }
    }

    protected void updateSessionInfo() {
        ZoomVideoSDKSession sessionInfo = ZoomVideoSDK.getInstance().getSession();
        if (ZoomVideoSDK.getInstance().isInSession()) {
            int size = UserHelper.getAllUsers().size();
            if (size <= 0) {
                size = 1;
            }
            practiceText.setText("Participants:" + size);
            if (sessionInfo != null) meetingPwd = sessionInfo.getSessionPassword();
            // mtvInput.setVisibility(View.VISIBLE);
            text_fps.setVisibility(View.GONE); // Was Visible Here Change to GOne as per requirement on 08/07/23 @arul
        } else {
            if (keyBoardLayout.isKeyBoardShow()) {
                keyBoardLayout.dismissChat(true);
                return;
            }
            actionBar.setVisibility(View.GONE);
            ceFormQuestionLayout.setVisibility(View.GONE);
            // mtvInput.setVisibility(View.GONE);
            text_fps.setVisibility(View.GONE);
            practiceText.setText("Connecting ...");
        }
        if (sessionInfo != null) sessionNameText.setText(sessionInfo.getSessionName());
        if (TextUtils.isEmpty(meetingPwd)) {
            iconLock.setImageResource(R.drawable.unlock);
        } else {
            iconLock.setImageResource(R.drawable.small_lock);
        }
    }


    protected void unSubscribe() {

    }

    @Override
    public void onSessionJoin() {
        Log.d(TAG, "onSessionJoin ");
        updateSessionInfo();
        updateFpsOrientation();
        actionBar.setVisibility(View.VISIBLE);

        if (ZoomVideoSDK.getInstance().getShareHelper().isSharingOut()) {
            ZoomVideoSDK.getInstance().getShareHelper().stopShare();
        }

        adapter.onUserJoin(UserHelper.getAllUsers());
        refreshUserListAdapter();
        publishSessionJoinedEvent();
        hideLoader();
        publishLocationEvent();
        // mtvInput.setVisibility(View.VISIBLE);
    }

    public void publishSessionJoinedEvent() {
        EventBus.getDefault().post(new SessionJoinedEvent(taskId, ZoomVideoSDK.getInstance().getSession().getSessionID()));
    }


    @SuppressLint("MissingPermission")
    private void publishLocationEvent() {
        if (!allowToCaptureLocation) {
            return;
        }

        // setting LocationRequest on FusedLocationClient
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            LocationEvent bestAccuracyLocationEvent = new LocationEvent(-1, -1, -1, meetingEntityId);
            EventBus.getDefault().post(bestAccuracyLocationEvent);
            return;
        }

        if (isLocationEnabled() && locationProviderClient != null) {
            locationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationTokenSource().getToken()).addOnSuccessListener(location -> {
                if (null != location) {
                    LocationEvent locationEvent = new LocationEvent(location.getLatitude(), location.getLongitude(), location.getAccuracy(), meetingEntityId);
                    EventBus.getDefault().post(locationEvent);
                } else {
                    new Handler(Looper.getMainLooper()).postDelayed(this::publishLocationEvent, 5000);
                }
            });
        }
    }

    public void publishSessionEndedEvent() {
        EventBus.getDefault().post(new SessionEndedEvent(taskId));
    }

    @Override
    public void onSessionLeave() {
        Log.d(TAG, "onSessionLeave");
        publishSessionEndedEvent();
        finish();
    }

    @Override
    public void onError(int errorcode) {
        Toast.makeText(this, ErrorMsgUtil.getMsgByErrorCode(errorcode) + ". Error code: " + errorcode, Toast.LENGTH_LONG).show();
        if (errorcode == ZoomVideoSDKErrors.Errors_Session_Disconnect) {
            unSubscribe();
            adapter.clear(true);
            updateSessionInfo();
            currentShareUser = null;
            mActiveUser = null;
            chatMsgAdapter.clear();
            chatListView.setVisibility(View.GONE);
            btnViewShare.setVisibility(View.GONE);
        } else if (errorcode == ZoomVideoSDKErrors.Errors_Session_Reconncting) {
            //start preview
//            subscribeVideoByUser(session.getMySelf());
        } else {
            ZoomVideoSDK.getInstance().leaveSession(false);
            finish();
        }

    }

    protected void subscribeVideoByUser(ZoomVideoSDKUser user) {

    }

    protected void subscribeShareByUser(ZoomVideoSDKUser user) {

    }

    @Override
    public void onUserJoin(ZoomVideoSDKUserHelper userHelper, List<ZoomVideoSDKUser> userList) {

        Log.d(TAG, "onUserJoin " + userList.size());
//        updateVideoListLayout();
        if (!isActivityPaused) {
            adapter.onUserJoin(userList);
        }
        refreshUserListAdapter();
        updateSessionInfo();
    }

    protected void selectAndScrollToUser(ZoomVideoSDKUser user) {
        if (null == user) {
            return;
        }
        adapter.updateSelectedVideoUser(user);
        int index = adapter.getIndexByUser(user);
        if (index >= 0) {
            LinearLayoutManager manager = (LinearLayoutManager) userVideoList.getLayoutManager();
            int first = manager.findFirstVisibleItemPosition();
            int last = manager.findLastVisibleItemPosition();
            if (index > last || index < first) {
                userVideoList.scrollToPosition(index);
                adapter.notifyDataSetChanged();
            }
        }
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) userVideoList.getLayoutManager();
        View view = linearLayoutManager.getChildAt(0);
        if (null != view) {
            scrollVideoViewForMargin(view);
        } else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    LinearLayoutManager linearLayoutManager = (LinearLayoutManager) userVideoList.getLayoutManager();
                    View view = linearLayoutManager.getChildAt(0);
                    scrollVideoViewForMargin(view);
                }
            }, 50);
        }
    }

    private void scrollVideoViewForMargin(View view) {
        if (null == view) {
            return;
        }
        int left = view.getLeft();
        int margin = 5;
        if (left > margin || left <= 0) {
            userVideoList.scrollBy(left - margin, 0);
        }
    }

    private void refreshUserListAdapter() {
        if (adapter.getItemCount() > 0) {
            videoListContain.setVisibility(View.VISIBLE);
            if (adapter.getSelectedVideoUser() == null) {
                ZoomVideoSDKUser zoomSDKUserInfo = session.getMySelf();
                if (null != zoomSDKUserInfo) {
                    selectAndScrollToUser(zoomSDKUserInfo);
                }
            }
        }
    }

    private void updateVideoListLayout() {
        int size = UserHelper.getAllUsers().size();
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) userVideoList.getLayoutParams();
        int preWidth = params.width;
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        if (size - 1 >= 3) {
            int maxWidth = (int) (325 * displayMetrics.scaledDensity);
            width = maxWidth;
        }
        if (width != preWidth) {
            params.width = width;
            userVideoList.setLayoutParams(params);
        }
    }

    @Override
    public void onUserLeave(ZoomVideoSDKUserHelper userHelper, List<ZoomVideoSDKUser> userList) {
        updateVideoListLayout();
        Log.d(TAG, "onUserLeave " + userList.size());
        adapter.onUserLeave(userList);
        if (adapter.getItemCount() == 0) {
            videoListContain.setVisibility(View.INVISIBLE);
        }
        updateSessionInfo();
    }

    @Override
    public void onUserVideoStatusChanged(ZoomVideoSDKVideoHelper videoHelper, List<ZoomVideoSDKUser> userList) {
        Log.d(TAG, "onUserVideoStatusChanged ");
        if (null == iconVideo) {
            return;
        }

        if (videoHelper.isMyVideoMirrored()) {
            videoHelper.mirrorMyVideo(false);
        }

        ZoomVideoSDKUser zoomSDKUserInfo = session.getMySelf();
        if (null != zoomSDKUserInfo) {
            iconVideo.setImageResource(zoomSDKUserInfo.getVideoStatus().isOn() ? R.drawable.icon_video_off : R.drawable.icon_video_on);
            if (userList.contains(zoomSDKUserInfo)) {
                checkMoreAction();
            }
        }
        adapter.onUserVideoStatusChanged(userList);
    }

    @Override
    public void onUserAudioStatusChanged(ZoomVideoSDKAudioHelper audioHelper, List<ZoomVideoSDKUser> userList) {
        // Toast.makeText(this, "" + userList.size(), Toast.LENGTH_SHORT).show();

        ZoomVideoSDKUser zoomSDKUserInfo = session.getMySelf();
        if (zoomSDKUserInfo != null && userList.contains(zoomSDKUserInfo)) {
            if (zoomSDKUserInfo.getAudioStatus().getAudioType() == ZoomVideoSDKAudioStatus.ZoomVideoSDKAudioType.ZoomVideoSDKAudioType_None) {
                iconAudio.setImageResource(R.drawable.icon_join_audio);
            } else {
                if (zoomSDKUserInfo.getAudioStatus().isMuted()) {
                    iconAudio.setImageResource(R.drawable.icon_unmute);
                } else {
                    iconAudio.setImageResource(R.drawable.icon_mute);
                }
            }
            checkMoreAction();
        }

        if (isVisitFirstTime) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    isVisitFirstTime = false;
                    adapter.onUserMuteUnmuteChanged(userList, userVideoList);
                }
            }, 3000);
        } else {
            adapter.onUserMuteUnmuteChanged(userList, userVideoList);
        }
    }

    @Override
    public void onUserShareStatusChanged(ZoomVideoSDKShareHelper shareHelper, ZoomVideoSDKUser userInfo, ZoomVideoSDKShareStatus status) {
        if (status == ZoomVideoSDKShareStatus.ZoomVideoSDKShareStatus_Start) {
            currentShareUser = userInfo;
            if (userInfo == session.getMySelf()) {
                iconShare.setImageResource(R.drawable.icon_stop_share);
            }
        } else if (status == ZoomVideoSDKShareStatus.ZoomVideoSDKShareStatus_Stop) {
            if (userInfo == session.getMySelf()) {
                /* only self share stop should update the ui */
                iconShare.setImageResource(R.drawable.icon_share);
                shareViewGroup.setVisibility(View.GONE);

                if (null != shareToolbar) {
                    shareToolbar.destroy();
                }
            }

            if (currentShareUser == userInfo) {
                currentShareUser = null;
            }
        }
    }


    @Override
    public void onLiveStreamStatusChanged(ZoomVideoSDKLiveStreamHelper liveStreamHelper, ZoomVideoSDKLiveStreamStatus status) {

    }

    @Override
    public void onChatNewMessageNotify(ZoomVideoSDKChatHelper chatHelper, ZoomVideoSDKChatMessage messageItem) {
        Log.d(TAG, "onChatNewMessageNotify msgId: " + messageItem.getMessageId());
        chatMsgAdapter.onReceive(messageItem);

        updateChatLayoutParams();
    }

    @Override
    public void onChatDeleteMessageNotify(ZoomVideoSDKChatHelper chatHelper, String msgID, ZoomVideoSDKChatMessageDeleteType deleteBy) {
        Log.d(TAG, "onChatDeleteMessageNotify msgID: " + msgID + ",deleteBy: " + deleteBy);
    }

    @Override
    public void onChatPrivilegeChanged(ZoomVideoSDKChatHelper chatHelper, ZoomVideoSDKChatPrivilegeType currentPrivilege) {
        Log.d(TAG, "onChatPrivilegeChanged currentPrivilege: " + currentPrivilege);
    }

    @Override
    public void onUserHostChanged(ZoomVideoSDKUserHelper userHelper, ZoomVideoSDKUser userInfo) {
        if (userInfo != null) {
            Log.d(TAG, "onUserHostChanged userInfo: " + userInfo.getUserName());
        }
    }


    @Override
    public void onSessionNeedPassword(ZoomVideoSDKPasswordHandler handler) {
        Log.d(TAG, "onSessionNeedPassword ");
        showInputPwdDialog(handler);
    }

    private void showInputPwdDialog(final ZoomVideoSDKPasswordHandler handler) {
        final Dialog builder = new Dialog(this, R.style.MyDialog);
        builder.setContentView(R.layout.dialog_session_input_pwd);
        builder.setCancelable(false);
        builder.setCanceledOnTouchOutside(false);
        final EditText editText = builder.findViewById(R.id.edit_pwd);
        builder.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pwd = editText.getText().toString();
                if (!TextUtils.isEmpty(pwd)) {
                    handler.inputSessionPassword(pwd);
                    builder.dismiss();
                }
            }
        });

        builder.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.leaveSessionIgnorePassword();
                builder.dismiss();
            }
        });

        builder.show();
    }


    @Override
    public void onSessionPasswordWrong(ZoomVideoSDKPasswordHandler handler) {
        Log.d(TAG, "onSessionPasswordWrong ");
        Toast.makeText(this, "Password wrong", Toast.LENGTH_LONG).show();
        showInputPwdDialog(handler);
    }

    @Override
    public void onUserActiveAudioChanged(ZoomVideoSDKAudioHelper audioHelper, List<ZoomVideoSDKUser> list) {
//        Log.d(TAG, "onUserActiveAudioChanged " + list);
        adapter.onUserActiveAudioChanged(list, userVideoList);
    }

    @Override
    public void onMixedAudioRawDataReceived(ZoomVideoSDKAudioRawData rawData) {

    }

    @Override
    public void onOneWayAudioRawDataReceived(ZoomVideoSDKAudioRawData rawData, ZoomVideoSDKUser user) {

    }

    @Override
    public void onUserManagerChanged(ZoomVideoSDKUser user) {
        Log.d(TAG, "onUserManagerChanged:" + user);
    }

    @Override
    public void onUserNameChanged(ZoomVideoSDKUser user) {
        Log.d(TAG, "onUserNameChanged:" + user);

    }

    @Override
    public void onShareAudioRawDataReceived(ZoomVideoSDKAudioRawData rawData) {

    }

    @Override
    public void onCommandReceived(ZoomVideoSDKUser sender, String strCmd) {
        Log.d(TAG, "onCommandReceived sender userName: " + sender.getUserName() + ", cmd: " + strCmd);
    }

    @Override
    public void onCommandChannelConnectResult(boolean isSuccess) {
        Log.d(TAG, "onCommandChannelConnectResult: " + isSuccess);
    }

    @Override
    public void onCloudRecordingStatus(ZoomVideoSDKRecordingStatus status, ZoomVideoSDKRecordingConsentHandler handler) {
        Log.d(TAG, "onCloudRecordingStatus status: " + status + ", handle: " + handler);
    }

    /*@Override
    public void onCloudRecordingStatus(ZoomVideoSDKRecordingStatus status) {

    }*/

    @Override
    public void onHostAskUnmute() {
        Log.d(TAG, "onHostAskUnmute ");
        Toast.makeText(this, "The host would like you to unmute", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onInviteByPhoneStatus(ZoomVideoSDKPhoneStatus status, ZoomVideoSDKPhoneFailedReason reason) {
        Log.d(TAG, "onInviteByPhoneStatus: " + "status: " + status + ", reason: " + reason);
    }

    @Override
    public void onMultiCameraStreamStatusChanged(ZoomVideoSDKMultiCameraStreamStatus status, ZoomVideoSDKUser user, ZoomVideoSDKRawDataPipe videoPipe) {
        Log.d(TAG, "onMultiCameraStreamStatusChanged: " + "status: " + status + ", user: " + user.getUserName() + " videoPipe:" + videoPipe);
        if (null != getMultiStreamDelegate() && renderType == RENDER_TYPE_OPENGLES && null != videoPipe) {
            Log.d(TAG, "onMultiCameraStreamStatusChanged: subscribe pipe");
            if (status == ZoomVideoSDKMultiCameraStreamStatus.Status_Joined) {
                videoPipe.subscribe(ZoomVideoSDKVideoResolution.VideoResolution_720P, getMultiStreamDelegate());
            } else {
                videoPipe.unSubscribe(getMultiStreamDelegate());
                //subscribe main user
                subscribeVideoByUser(user);
            }
        }
    }

    public void onMultiCameraStreamStatusChanged(ZoomVideoSDKMultiCameraStreamStatus status, ZoomVideoSDKUser user, ZoomVideoSDKVideoCanvas canvas) {
        Log.d(TAG, "onMultiCameraStreamStatusChanged: " + "status: " + status + ", user: " + user.getUserName() + " videoPipe:" + canvas);
        if (null != getMultiStreamVideoView() && renderType == RENDER_TYPE_ZOOMRENDERER && null != canvas) {
            Log.d(TAG, "onMultiCameraStreamStatusChanged: subscribe canvas");
            if (status == ZoomVideoSDKMultiCameraStreamStatus.Status_Joined) {
                canvas.subscribe(getMultiStreamVideoView(), ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_PanAndScan);
            } else {
                canvas.unSubscribe(getMultiStreamVideoView());
                //subscribe main user
                subscribeVideoByUser(user);
            }
        }
    }

    @Override
    public void onLiveTranscriptionStatus(ZoomVideoSDKLiveTranscriptionHelper.ZoomVideoSDKLiveTranscriptionStatus status) {

    }

    @Override
    public void onLiveTranscriptionMsgReceived(String ltMsg, ZoomVideoSDKUser pUser, ZoomVideoSDKLiveTranscriptionHelper.ZoomVideoSDKLiveTranscriptionOperationType type) {

    }

    @Override
    public void onOriginalLanguageMsgReceived(ZoomVideoSDKLiveTranscriptionHelper.ILiveTranscriptionMessageInfo messageInfo) {

    }

    @Override
    public void onLiveTranscriptionMsgInfoReceived(ZoomVideoSDKLiveTranscriptionHelper.ILiveTranscriptionMessageInfo messageInfo) {

    }

    @Override
    public void onLiveTranscriptionMsgError(ZoomVideoSDKLiveTranscriptionHelper.ILiveTranscriptionLanguage spokenLanguage, ZoomVideoSDKLiveTranscriptionHelper.ILiveTranscriptionLanguage transcriptLanguage) {

    }

    @Override
    public void onSSLCertVerifiedFailNotification(ZoomVideoSDKSSLCertificateInfo info) {

    }

    @Override
    public void onProxySettingNotification(ZoomVideoSDKProxySettingHandler handler) {
    }

    protected ZoomVideoSDKUser feccUser;

    public void onCameraControlRequestResult(ZoomVideoSDKUser user, boolean isApproved) {
        Log.d(TAG, "onCameraControlRequestResult:" + user + ":" + isApproved);
        if (isApproved) {
            feccUser = user;
        } else {
            feccUser = null;
        }
    }

    protected ZoomVideoSDKRawDataPipeDelegate getMultiStreamDelegate() {
        return null;
    }

    protected ZoomVideoSDKVideoView getMultiStreamVideoView() {
        return null;
    }

    @Override
    public void onUserVideoNetworkStatusChanged(ZoomVideoSDKNetworkStatus status, ZoomVideoSDKUser user) {
        Log.d(TAG, "onUserVideoNetworkStatusChanged:" + user.getUserName() + ":" + status);
    }

    @Override
    public void onUserRecordingConsent(ZoomVideoSDKUser user) {
        Log.d(TAG, "onUserRecordingConsent:" + user.getUserName());
    }

    @Override
    public void onCallCRCDeviceStatusChanged(ZoomVideoSDKCRCCallStatus status) {
        Log.d(TAG, "onCallOutCRCDeviceStateChanged:" + status);
    }

    @Override
    public void onVideoCanvasSubscribeFail(ZoomVideoSDKVideoSubscribeFailReason fail_reason, ZoomVideoSDKUser pUser, ZoomVideoSDKVideoView view) {
        Log.d(TAG, "onVideoCanvasSubscribeFail:" + fail_reason + ":" + view + ":" + pUser.getUserName());

    }

    @Override
    public void onShareCanvasSubscribeFail(ZoomVideoSDKVideoSubscribeFailReason fail_reason, ZoomVideoSDKUser pUser, ZoomVideoSDKVideoView view) {
        Log.d(TAG, "onShareCanvasSubscribeFail:" + fail_reason + ":" + view + ":" + pUser.getUserName());
    }

    @Override
    public void onAnnotationHelperCleanUp(ZoomVideoSDKAnnotationHelper helper) {

    }

    @Override
    public void onAnnotationPrivilegeChange(boolean enable, ZoomVideoSDKUser shareOwner) {

    }

    @Override
    public void onTestMicStatusChanged(ZoomVideoSDKTestMicStatus status) {

    }

    @Override
    public void onMicSpeakerVolumeChanged(int micVolume, int speakerVolume) {

    }

    public void showOfflineDialog() {
        internetAlertDialog = new Dialog(this, R.style.MyDialog);
        internetAlertDialog.setCanceledOnTouchOutside(false);
        internetAlertDialog.setCancelable(false);
        internetAlertDialog.setContentView(R.layout.dialog_internet_error);
        internetAlertDialog.findViewById(R.id.btn_retry).setOnClickListener(view -> {
            if (NetworkUtil.isOnline(BaseMeetingActivity.this)) {
                internetAlertDialog.hide();
            } else {
                Toast.makeText(BaseMeetingActivity.this, R.string.internet_toast_error_message, Toast.LENGTH_SHORT).show();
            }
        });

        internetAlertDialog.show();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onInternetChange(InternetEvent internetEvent) {
        Log.d(TAG, "InternetChangeEvent received, with isOnline as " + internetEvent.isOnline);
        if (internetEvent.isOnline) {
            if (internetAlertDialog != null) {
                internetAlertDialog.hide();
            }
        } else {
            if (internetAlertDialog != null) {
                if (!internetAlertDialog.isShowing()) {
                    showOfflineDialog();
                }
            } else {
                showOfflineDialog();
            }
        }
    }

    // method to check
    // if location is enabled
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // If everything is alright then
    @Override
    public void
    onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                publishLocationEvent();
            }
        }
    }


    public void captureScreenshot(View view) {
        // Get the root view of the current activity
        View rootView = getWindow().getDecorView().getRootView();

        // Create a Bitmap with the same dimensions as the root view
        Bitmap screenshot = Bitmap.createBitmap(rootView.getWidth(), rootView.getHeight(), Bitmap.Config.ARGB_8888);

        // Create a Canvas and draw the root view onto the Bitmap
        Canvas canvas = new Canvas(screenshot);
        rootView.draw(canvas);

        Toast.makeText(this, "Capture DOne", Toast.LENGTH_SHORT).show();
        // Save the Bitmap to a file
        saveScreenshotToFile(screenshot);
    }

    private void saveScreenshotToFile(Bitmap screenshot) {
        // Create a directory to store the screenshots
        File directory = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Screenshots");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Create a unique file name for the screenshot
        String fileName = "screenshot_" + System.currentTimeMillis() + ".png";

        // Save the screenshot to the file
        File file = new File(directory, fileName);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

}

