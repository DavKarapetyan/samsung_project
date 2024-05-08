package com.example.project_.repository;

import androidx.core.app.NotificationCompat;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Person;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.project_.R;
import com.example.project_.activities.CallActivity;
import com.example.project_.activities.MainActivity;
import com.example.project_.firebase.MessagingService;
import com.example.project_.network.ApiClient;
import com.example.project_.network.ApiService;
import com.example.project_.remote.FirebaseClient;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.DataModel;
import com.example.project_.utilities.DataModelType;
import com.example.project_.utilities.ErrorCallBack;
import com.example.project_.utilities.NewEventCallBack;
import com.example.project_.utilities.PreferenceManager;
import com.example.project_.utilities.SuccessCallBack;
import com.example.project_.webrtc.MyPeerConnectionObserver;
import com.example.project_.webrtc.WebRTCClient;
import com.google.firebase.Firebase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

import java.util.Date;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainRepository implements WebRTCClient.Listener {

    public Listener listener;
    private final Gson gson = new Gson();
    private final FirebaseClient firebaseClient;

    private WebRTCClient webRTCClient;

    private String currentUsername;

    private SurfaceViewRenderer remoteView;
    private AudioManager audioManager;
    private String target;
    private PreferenceManager preferenceManager;
    private void updateCurrentUsername(String username){
        this.currentUsername = username;
    }

    private MainRepository(){
        this.firebaseClient = new FirebaseClient();
    }

    private static MainRepository instance;
    public static MainRepository getInstance(){
        if (instance == null){
            instance = new MainRepository();
        }
        return instance;
    }

    public void login(String username, Context context, SuccessCallBack callBack){
        firebaseClient.login(username,()->{
            updateCurrentUsername(username);
            this.webRTCClient = new WebRTCClient(context,new MyPeerConnectionObserver(){
                @Override
                public void onAddStream(MediaStream mediaStream) {
                    super.onAddStream(mediaStream);
                    try{
                        mediaStream.videoTracks.get(0).addSink(remoteView);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
                    Log.d("TAG", "onConnectionChange: "+newState);
                    super.onConnectionChange(newState);
                    if (newState == PeerConnection.PeerConnectionState.CONNECTED && listener!=null){
                        listener.webrtcConnected();
                    }

                    if (newState == PeerConnection.PeerConnectionState.CLOSED ||
                            newState == PeerConnection.PeerConnectionState.DISCONNECTED ){
                        if (listener!=null){
                            listener.webrtcClosed();
                        }
                    }
                }

                @Override
                public void onIceCandidate(IceCandidate iceCandidate) {
                    super.onIceCandidate(iceCandidate);
                    webRTCClient.sendIceCandidate(iceCandidate,target);
                }
            },username);
            webRTCClient.listener = this;
            callBack.onSuccess();
        });
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public void initLocalView(SurfaceViewRenderer view){
        webRTCClient.initLocalSurfaceView(view);
    }

    public void initRemoteView(SurfaceViewRenderer view){
        webRTCClient.initRemoteSurfaceView(view);
        this.remoteView = view;
    }
    public void startCall(String target){
        webRTCClient.call(target);
    }
    public void switchCamera() {
        webRTCClient.switchCamera();
    }

    public void toggleAudio(Boolean shouldBeMuted){
        webRTCClient.toggleAudio(shouldBeMuted);
    }
    public void toggleVideo(Boolean shouldBeMuted){
        webRTCClient.toggleVideo(shouldBeMuted);
    }
    public void sendCallRequest(String target,String targetToken , ErrorCallBack errorCallBack) throws JSONException {
        preferenceManager = new PreferenceManager(remoteView.getContext());
        firebaseClient.sendMessageToOtherUser(
                new DataModel(target, currentUsername, null, DataModelType.StartCall),
                new ErrorCallBack() {
                    @Override
                    public void onError() {
                        // Handle error
                    }
                }
        );
        JSONArray tokens = new JSONArray();
        tokens.put(targetToken);

        JSONObject data = new JSONObject();
        data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
        data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
        data.put(Constants.KEY_MESSAGE, target);
        data.put("notificationType", "call");

        JSONObject body = new JSONObject();
        body.put(Constants.REMOTE_MSG_DATA, data);
        body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

        try {
            JSONObject dataObject = body.getJSONObject("data");

            String name = dataObject.getString("name");

            HashMap<String, Object> notification = new HashMap<>();
            notification.put("name", "New message");
            notification.put("receiverId", target);
            notification.put("senderImage", preferenceManager.getString(Constants.KEY_IMAGE));
            notification.put("timestamp", new Date());
            notification.put("message", name + " sent you a message");

            FirebaseFirestore.getInstance().collection("notifications").add(notification);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ApiClient.getClient().create(ApiService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),
                body.toString()
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    try {
                        if (response.body() != null) {
                            JSONObject responseJson = new JSONObject(response.body());
                            JSONArray results = responseJson.getJSONArray("results");
                            if (responseJson.getInt("failure") == 1) {
                                JSONObject error = (JSONObject) results.get(0);
                                //showToast(error.getString("error"));
                                Toast.makeText(remoteView.getContext(), "error", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(remoteView.getContext(), "Notification was sent", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(remoteView.getContext(), "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(remoteView.getContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void turnOnSpeakers() {
        int result = audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.setSpeakerphoneOn(true);
        } else {
            Log.e("MainRepository", "Failed to request audio focus");
        }
    }
    public void releaseAudioFocus() {
        audioManager.abandonAudioFocus(null); // Release audio focus
        audioManager.setMode(AudioManager.MODE_NORMAL); // Reset audio mode
        audioManager.setSpeakerphoneOn(false); // Turn off speakerphone
    }
    public void endCall(){
        webRTCClient.closeConnection();
    }

    public void subscribeForLatestEvent(NewEventCallBack callBack){
        firebaseClient.observeIncomingLatestEvent(model -> {
            switch (model.getType()){

                case Offer:
                    this.target = model.getSender();
                    webRTCClient.onRemoteSessionReceived(new SessionDescription(
                            SessionDescription.Type.OFFER,model.getData()
                    ));
                    webRTCClient.answer(model.getSender());
                    break;
                case Answer:
                    this.target = model.getSender();
                    webRTCClient.onRemoteSessionReceived(new SessionDescription(
                            SessionDescription.Type.ANSWER,model.getData()
                    ));
                    break;
                case IceCandidate:
                        try{
                            IceCandidate candidate = gson.fromJson(model.getData(),IceCandidate.class);
                            webRTCClient.addIceCandidate(candidate);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    break;
                case StartCall:
                    this.target = model.getSender();
                    callBack.onNewEventReceived(model);
                    break;
            }

        });
    }

    @Override
    public void onTransferDataToOtherPeer(DataModel model) {
        firebaseClient.sendMessageToOtherUser(model,()->{});
    }

    public interface Listener{
        void webrtcConnected();
        void webrtcClosed();
    }
}
