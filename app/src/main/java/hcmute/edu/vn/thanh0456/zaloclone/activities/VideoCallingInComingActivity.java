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

import com.google.firebase.firestore.FirebaseFirestore;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;

import hcmute.edu.vn.thanh0456.zaloclone.network.APIClient;
import hcmute.edu.vn.thanh0456.zaloclone.network.APIService;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ActivityVideoCallingInComingBinding;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Activité du côté de l’invité
public class VideoCallingInComingActivity extends AppCompatActivity {

    // Conception de la mise en page et de l’affichage de liaison (vous n’avez pas besoin de créer de variables pour obtenir les propriétés de la mise en page et les utiliser)
    ActivityVideoCallingInComingBinding binding;
    //instance de Firestore pour interagir avec les données selon les besoins
    FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoCallingInComingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        loadSenderDetails();
        setListeners();
    }

    // Fonction d’initialisation
    private void init() {
        String meetingType = getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE);
        database = FirebaseFirestore.getInstance();
    }

    // Fonction de configuration d’événements
    private void setListeners() {
        // Choisissez d’accepter ou de refuser l’appel, en envoyant une réponse à l’invité
        binding.fabAccept.setOnClickListener(v -> {
            sendInvitationResponse(
                    Constants.REMOTE_MSG_INVITATION_ACCEPTED,
                    getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)
            );
        });
        binding.fabDecline.setOnClickListener(v -> {
            sendInvitationResponse(
                    Constants.REMOTE_MSG_INVITATION_REJECTED,
                    getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)
            );
        });
    }

    // La fonction de traitement renvoie une réponse à l’invité
    private void sendInvitationResponse(String type, String receiverToken) {
        try {
            // Configurer les données
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, type);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

            // Renvoyer les données aux invités
            sendRemoteMessage(body.toString(), type);

        } catch (Exception e) {
            showToast(e.getMessage());
            finish();
        }
    }

    // La fonction de traitement renvoie les données à l’invité
    private void sendRemoteMessage(String remoteMessageBody, String type) {
        APIClient.getClient().create(APIService.class).sendMessage(
                Constants.getRemoteMsgHeaders(), remoteMessageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    // Si accepté, créez un appel vidéo entre 2 personnes
                    if (type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)) {
                        showToast("Invitation Accepted");

                        try {
                            //
                            URL serverURL = new URL("https://meet.jit.si");
                            JitsiMeetConferenceOptions conferenceOptions = new JitsiMeetConferenceOptions.Builder()
                                    .setServerURL(serverURL)
                                    .setWelcomePageEnabled(false)
                                    .setRoom(getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_ROOM))
                                    .build();
                            JitsiMeetActivity.launch(VideoCallingInComingActivity.this, conferenceOptions);
                            finish();
                        } catch (Exception e) {
                            // Si une erreur se produit lors de l’initialisation de l’appel, de l’affichage et de la fin de l’activité
                            //  Retour à la page précédente
                            showToast(e.getMessage());
                            finish();
                        }

                    } else {
                        // En cas de rejet, afficher et terminer l’activité
                        //                        Retour à la page précédente
                        showToast("Invitation Rejected");
                        finish();
                    }
                } else {
                    //      Si la réponse renvoie une erreur, affichez et terminez l’activité
                    //                    Retour à la page précédente
                    showToast(response.message());
                    finish();
                }
            }
            // Gestion de l’échec de la remise des invitations
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                // Afficher les erreurs, terminer et revenir à la page précédente
                showToast(t.getMessage());
                finish();
            }
        });
    }

    // Recevoir un signal pour répondre à l’invitation d’un expéditeur
    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            if (type.equals(Constants.REMOTE_MSG_INVITATION_CANCELLED)) {
                showToast("Invitation Cancelled");
                finish();
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        // Abonnez-vous à la fonction qui recevra les données comme signal pour répondre à une invitation de l’expéditeur
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                invitationResponseReceiver,
                new IntentFilter(Constants.REMOTE_MSG_INVITATION_RESPONSE)
        );
    }

    // Se désabonner de la fonction de réception lorsque l’activité est arrêtée
    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );
    }

    // Télécharger les données de l’expéditeur et les afficher dans l’interface
    private void loadSenderDetails() {
        binding.textName.setText(getIntent().getStringExtra(Constants.KEY_NAME));
        String senderId = getIntent().getStringExtra(Constants.KEY_USER_ID);
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(senderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        binding.imageProfile.setImageBitmap(getBitmapFromEncodedImage(documentSnapshot.getString(Constants.KEY_IMAGE)));
                    }
                });
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
        Toast.makeText(VideoCallingInComingActivity.this, message, Toast.LENGTH_SHORT).show();
    }

}