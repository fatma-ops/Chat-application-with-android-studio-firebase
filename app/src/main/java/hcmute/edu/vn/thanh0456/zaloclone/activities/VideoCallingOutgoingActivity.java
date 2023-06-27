package hcmute.edu.vn.thanh0456.zaloclone.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.UUID;

import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.network.APIClient;
import hcmute.edu.vn.thanh0456.zaloclone.network.APIService;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ActivityVideoCallingOutgoingBinding;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Activité du côté de l’invitant
public class VideoCallingOutgoingActivity extends AppCompatActivity {

    // Conception de la mise en page et de l’affichage de liaison (vous n’avez pas besoin de créer de variables pour
    // obtenir les propriétés de la mise en page et les utiliser)
    ActivityVideoCallingOutgoingBinding binding;
    // Enregistrer les données relatives aux invités
    private User receivedUser;
    // Enregistrer ou récupérer des données dans SharePref
    private PreferenceManager preferenceManager;
    // meetingRoom ID
    String meetingRoom = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoCallingOutgoingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        loadReceiverDetails();
        setListeners();
        String meetingType = getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE);
        if (meetingType != null && receivedUser != null) {
            initiateMeeting(meetingType, receivedUser.token);
        }
    }

    // Fonction d’initialisation
    private void init() {
        // Enregistrer ou récupérer des données à partir de SharePrefs
        preferenceManager = new PreferenceManager(getApplicationContext());
    }

    // Fonction de configuration d’événements
    private void setListeners() {
        binding.fabDecline.setOnClickListener(v -> {
            if (receivedUser != null) {
                cancelInvitation(receivedUser.token);
            }
        });
    }

    // La fonction de traitement génère et envoie des invitations d’appel vidéo aux destinataires
    private void initiateMeeting(String meetingType, String receiverToken) {
        try {
            // Configurer les données nécessaires
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION);
            data.put(Constants.REMOTE_MSG_MEETING_TYPE, meetingType);
            data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
            data.put(Constants.REMOTE_MSG_INVITER_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));

            meetingRoom = preferenceManager.getString(Constants.KEY_USER_ID)
                    + '_'
                    + UUID.randomUUID().toString().substring(0, 5);
            data.put(Constants.REMOTE_MSG_MEETING_ROOM, meetingRoom);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

            //Envoyer une invitation à un appel vidéo
            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION);

        } catch (Exception e) {
            showToast(e.getMessage());
            finish();
        }
    }

    // La fonction de traitement envoie des invitations aux appels vidéo
    private void sendRemoteMessage(String remoteMessageBody, String type) {
        // Créer une API, envoyer des notifications à un autre appareil
        APIClient.getClient().create(APIService.class).sendMessage(
                Constants.getRemoteMsgHeaders(), remoteMessageBody
        ).enqueue(new Callback<String>() {
            // Gérer la réponse renvoyée
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    if (type.equals(Constants.REMOTE_MSG_INVITATION)) {
                        // Si l’invitation a été envoyée avec succès, affichez un message
                        showToast("Invitation send successfully");
                    } else if (type.equals(Constants.REMOTE_MSG_INVITATION_RESPONSE)) {
                        // Si le destinataire annule, ne participe pas
                        //                        Afficher les notifications, terminer et revenir à la page de discussion
                        showToast("Invitation cancelled");
                        finish();
                    }
                } else {
                    // Si la réponse contient une erreur, affichez et terminez, revenez à la page de discussion
                    showToast(response.message());
                    finish();
                }
            }

            // Gestion de l’échec de la remise des invitations
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                // Afficher les erreurs, terminer et revenir à la page de discussion
                showToast(t.getMessage());
                finish();
            }
        });
    }

    // La fonction gère l’annulation des invitations
    private void cancelInvitation(String receiverToken) {
        try {
            // Configurer les données
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, Constants.REMOTE_MSG_INVITATION_CANCELLED);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);
            // Envoyer une invitation à un appel vidéo
            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION_RESPONSE);

        } catch (Exception e) {
            showToast(e.getMessage());
            finish();
        }
    }

    // Recevoir un signal en réponse à l’invitation d’un destinataire
    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            // Si le destinataire de l’invitation accepte de participer, créez un appel vidéo
            //            Au contraire, terminez et revenez à la page de discussion
            if (type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)) {
                showToast("Invitation Accepted");
                try {
                    // Configurer des données pour créer un appel vidéo
                    //                    Fermez l’activité en cours, accédez à l’interface d’appel
                    URL serverURL = new URL("https://meet.jit.si");
                    JitsiMeetConferenceOptions conferenceOptions = new JitsiMeetConferenceOptions.Builder()
                            .setServerURL(serverURL)
                            .setWelcomePageEnabled(false)
                            .setRoom(meetingRoom)
                            .build();
                    JitsiMeetActivity.launch(VideoCallingOutgoingActivity.this, conferenceOptions);
                    finish();
                } catch (Exception e) {
                    // Si une erreur se produit lors de l’initialisation de l’appel, de l’affichage et de la fin de l’activité
                    //                    Retour à la page de discussion
                    showToast(e.getMessage());
                    finish();
                }
            } else if (type.equals(Constants.REMOTE_MSG_INVITATION_REJECTED)) {
                // Si l’invité refuse, affiche et met fin à l’activité.
                //                Retour à la page de discussion
                showToast("Invitation Rejected");
                finish();
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        // Enregistrer la fonction qui recevra les données comme signal pour répondre à une invitation du destinataire
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                invitationResponseReceiver,
                new IntentFilter(Constants.REMOTE_MSG_INVITATION_RESPONSE)
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Supprimer la fonction d’abonnement de Broadcast
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );
    }

    // Télécharger les données du destinataire et les afficher sur l’interface
    private void loadReceiverDetails() {
        receivedUser = getIntent().getParcelableExtra(Constants.KEY_USER);
        binding.textName.setText(receivedUser.name);
        binding.imageProfile.setImageBitmap(getBitmapFromEncodedImage(receivedUser.image));
    }

    // Fonction de conversion d’image de chaîne en bitmap
    private Bitmap getBitmapFromEncodedImage(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }
    }

    // La fonction reçoit la chaîne String et affiche
    private void showToast(String message) {
        Toast.makeText(VideoCallingOutgoingActivity.this, message, Toast.LENGTH_SHORT).show();
    }

}