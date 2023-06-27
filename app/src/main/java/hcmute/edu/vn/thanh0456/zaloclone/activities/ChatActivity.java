package hcmute.edu.vn.thanh0456.zaloclone.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.devlomi.record_view.OnRecordListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

import hcmute.edu.vn.thanh0456.zaloclone.adaptor.ChatAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.models.ChatMessage;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.network.APIClient;
import hcmute.edu.vn.thanh0456.zaloclone.network.APIService;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Permissions;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ActivityChatBinding;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


// Gérer l’interface de chat 1-1
//étend BaseActivity pour surveiller et mettre à jour l’état marche/arrêt de l’utilisateur
public class ChatActivity extends BaseActivity {

    // Conception de la mise en page et de l’affichage de liaison (vous n’avez pas besoin de créer de variables pour obtenir les propriétés de la mise en page et les utiliser)
    private ActivityChatBinding binding;
    // Enregistrer les données du destinataire
    private User receivedUser;
    // Enregistrer ou récupérer des données dans SharePrefs
    private PreferenceManager preferenceManager;
    // Historique des messages
    private ArrayList<ChatMessage> chatMessages;
    // Adaptateurs
    private ChatAdaptor chatAdaptor;
    // Interaction avec les données sur Firestore
    private FirebaseFirestore database;
    // ID de conversation entre 2 utilisateurs
    private String conversationId = null;
    // État marche/arrêt de l’utilisateur
    private Boolean isReceiverAvailaible = false;
    // ID du message
    private String chatId = null;
    //   Chemin d’accès aux photos, audio stockés sur Firestore’s Storage
    private String imageURL = null, audioURL = null;
    // Đẩy ảnh, audio lên Storage
    private StorageTask uploadTask;
    // Uri từ đường dẫn đến ảnh, audio lưu trên thiết bị
    private Uri imageUri, audioUri;
    // Tạo recording
    private MediaRecorder mediaRecorder;
    // Đường dẫn đến file recording
    private String audioPath;
    // Tạm dừng hiển thị UI bottom chat, đợi animation cancel recording hoàn thành
    Handler handlerUI = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loadReceiverDetails();
        setListeners();
        init();
        listenMessages();
    }

    // Fonction de configuration d’événements
    private void setListeners() {
        //         Masquer/afficher l’interface de chat en bas en fonction des messages saisis par l’utilisateur
        binding.inputMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (binding.inputMessage.getText().length() != 0) {
                    binding.imageCamera.setVisibility(View.GONE);
                    binding.imagePhoto.setVisibility(View.GONE);
                    binding.imageLike.setVisibility(View.GONE);
                    binding.imageShrink.setVisibility(View.VISIBLE);
                    binding.imageSend.setVisibility(View.VISIBLE);
                } else {
                    binding.imageCamera.setVisibility(View.VISIBLE);
                    binding.imagePhoto.setVisibility(View.VISIBLE);
                    binding.imageLike.setVisibility(View.VISIBLE);
                    binding.imageShrink.setVisibility(View.GONE);
                    binding.imageSend.setVisibility(View.GONE);
                }
            }
        });
        binding.imageShrink.setOnClickListener(view -> {
            binding.imageCamera.setVisibility(View.VISIBLE);
            binding.imagePhoto.setVisibility(View.VISIBLE);
            binding.imageShrink.setVisibility(View.GONE);
        });
        // Retour à la page précédente
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        // Appeler la fonction de traitement des messages texte
        binding.imageSend.setOnClickListener(v -> sendMessage());
        // Appelez la fonction de traitement des messages d’image
        binding.imagePhoto.setOnClickListener(v -> sendImage());
        // Envoyer des invitations d’appels vocaux aux utilisateurs, afficher l’interface en attente d’une réponse des invités
        binding.imageVideo.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, VideoCallingOutgoingActivity.class);
            intent.putExtra(Constants.KEY_USER, receivedUser);
            intent.putExtra(Constants.REMOTE_MSG_MEETING_TYPE, "video");
            startActivity(intent);
        });
        // Accorder des autorisations d’enregistrement à l’application
        binding.recordButton.setOnClickListener(v -> {
            if (Permissions.isRecordingok(ChatActivity.this)) {
                binding.recordButton.setListenForRecord(true);
            } else {
                Permissions.requestRecording(ChatActivity.this);
            }
        });
        // Maintenez le bouton d’enregistrement enfoncé
        binding.recordView.setOnRecordListener(new OnRecordListener() {
            @Override
            public void onStart() {
                // Début de l’enregistrement
                Log.d("RecordView", "onStart");
                setUpRecording();
                binding.layoutBottomLeft.setVisibility(View.GONE);
                binding.inputMessage.setVisibility(View.GONE);
                binding.layoutLikeAndSend.setVisibility(View.GONE);
                try {
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Le gestionnaire tire de droite à gauche pour annuler l’enregistrement
            @Override
            public void onCancel() {
                Log.d("RecordView", "onCancel");

                mediaRecorder.reset();
                mediaRecorder.release();
                File file = new File(audioPath);
                if (file.exists()) {
                    file.delete();
                }
                handlerUI.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        binding.layoutBottomLeft.setVisibility(View.VISIBLE);
                        binding.inputMessage.setVisibility(View.VISIBLE);
                        binding.layoutLikeAndSend.setVisibility(View.VISIBLE);                    }
                }, 1150);
            }

            //  La fonction de traitement supprime le bouton d’enregistrement pour terminer l’enregistrement
            @Override
            public void onFinish(long recordTime) {
                Log.d("RecordView", "onFinish");
                mediaRecorder.stop();
                mediaRecorder.release();
                binding.layoutBottomLeft.setVisibility(View.VISIBLE);
                binding.inputMessage.setVisibility(View.VISIBLE);
                binding.layoutLikeAndSend.setVisibility(View.VISIBLE);
                sendRecordingMessage();
            }

            // La fonction gère si la durée de l’enregistrement est inférieure à 1 seconde, puis annule le fichier, n’envoie pas
            @Override
            public void onLessThanSecond() {
                // Lorsque le temps d’enregistrement est inférieur à une seconde
                Log.d("RecordView", "onLessThanSecond");

                mediaRecorder.reset();
                mediaRecorder.release();

                File file = new File(audioPath);
                if (file.exists()) {
                    file.delete();
                }

                binding.layoutBottomLeft.setVisibility(View.VISIBLE);
                binding.inputMessage.setVisibility(View.VISIBLE);
                binding.layoutLikeAndSend.setVisibility(View.VISIBLE);
            }
        });
        // Thiết lập animation cho nút record
        binding.recordView.setOnBasketAnimationEndListener(() -> {
            Log.d("RecordView", "Basket Animation Finished");
            binding.layoutBottomLeft.setVisibility(View.VISIBLE);
            binding.inputMessage.setVisibility(View.VISIBLE);
            binding.layoutLikeAndSend.setVisibility(View.VISIBLE);
        });
    }

    // Fonction d’initialisation
    private void init() {
        binding.imageProfile.setImageBitmap(getBitmapFromEncodedImage(receivedUser.image));
        preferenceManager = new PreferenceManager(getApplicationContext());
        // Enregistrer des messages entre 2 utilisateurs
        chatMessages = new ArrayList<>();
        // Configuration de l’adaptateur
        chatAdaptor = new ChatAdaptor(
                chatMessages,
                getBitmapFromEncodedImage(receivedUser.image),
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        // Connectez l’adaptateur au RecyclerView
        binding.chatRecyclerView.setAdapter(chatAdaptor);
        // instance de Firestore pour interagir avec les données selon les besoins
        database = FirebaseFirestore.getInstance();
        // Configurer RecordView pour le bouton d’enregistrement
        binding.recordButton.setRecordView(binding.recordView);
        binding.recordButton.setListenForRecord(false);
    }

    // Définissez la source, le format, l’encodage du fichier d’enregistrement, audioPath est le chemin d’accès au fichier d’enregistrement
    private void setUpRecording() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//        File file = new File(Environment.getExternalStorageDirectory(). getAbsolutePath(), "Recordings");
//        if (!file.exists()) {
//            file.mkdirs();
//        }
        audioPath = getFilePath();

        mediaRecorder.setOutputFile(audioPath);
    }

    // Fonction pour définir filepath pour enregistrer le fichier
    private String getFilePath() {
        ContextWrapper contextwrapper = new ContextWrapper(getApplicationContext());
        File recordPath = null;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            recordPath = contextwrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        }
        File file  = new File(recordPath, System.currentTimeMillis() + ".3gp");
        return file.getPath();
    }

    // Fonction de traitement des messages d’enregistrement
    private void sendRecordingMessage() {
        // Après l’enregistrement, obtenez l’URI à partir d’audioPath, poussez le fichier d’enregistrement vers le stockage de Firebase, dans le dossier Fichiers audio.
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Audio Files");
        audioUri = Uri.fromFile(new File(audioPath));

        DocumentReference ref = database.collection(Constants.KEY_COLLECTION_CHAT).document();
        chatId = ref.getId();

        StorageReference audioPathOnFireBase = storageReference.child(chatId + "." + "3gp");
        uploadTask = audioPathOnFireBase.putFile(audioUri);
        uploadTask.continueWithTask(task -> {
            // Si tout le reste échoue, affichez une erreur
            if (!task.isSuccessful()) {
                showToast(task.getException().getMessage());
                throw task.getException();
            }
            // En cas de succès, renvoyez l’URL au fichier d’enregistrement sur le stockage
            return audioPathOnFireBase.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                // Configurer les données nécessaires pour un document dans le chat de collecte
                Uri downloadURL = (Uri) task.getResult();
                audioURL = downloadURL.toString();

                HashMap<String, Object> message = new HashMap<>();
                message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                message.put(Constants.KEY_RECEIVER_ID, receivedUser.id);
                message.put(Constants.KEY_MESSAGE, audioURL);
                message.put(Constants.KEY_TYPE, "audio");
                message.put(Constants.KEY_TIMESTAMP, new Date());

                // Créer un document dans la collection de chats avec l’ID de chatId, pousser les données dans le document
                database.collection(Constants.KEY_COLLECTION_CHAT)
                        .document(chatId)
                        .set(message)
                        .addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                showToast("Audio sent successfully");
                            } else {
                                showToast(task1.getException().getMessage());
                            }
                        });
                // Fonction de mise à jour LastMessage
                updateLastMessage(message);
                binding.inputMessage.setText(null);
            }
        });
    }

    // Fonction de livraison de messages texte
    private void sendMessage() {

        // Configurer des données pour un document dans le chat de collection
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receivedUser.id);
        message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
        message.put(Constants.KEY_TYPE, "text");
        message.put(Constants.KEY_TIMESTAMP, new Date());

        // Ajouter des données au chat de collecte
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .add(message);
        // Fonction de mise à jour LastMessage
        updateLastMessage(message);
        binding.inputMessage.setText(null);
    }

    // Fonction de traitement des messages d’image
    private void sendImage() {
        // Sélectionner une photo
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        // Fonction de traitement après la sélection d’une image
        startActivityForResult(intent.createChooser(intent, "Select Image"), 438);
    }

    // Fonction de traitement après la sélection d’une image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 438 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            // Obtenir un uri à partir d’une photo, push vers Firebase Storage, enregistrer dans le dossier «Image Files»
            imageUri = data.getData();
            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files");

            DocumentReference ref = database.collection(Constants.KEY_COLLECTION_CHAT).document();
            chatId = ref.getId();

            StorageReference imagePath = storageReference.child(chatId + "." + "jpg");
            uploadTask = imagePath.putFile(imageUri);
            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    showToast(task.getException().getMessage());
                    throw task.getException();
                }
                // Si la diffusion de l’image réussit, renvoyez l’URL à l’image sur le stockage
                return imagePath.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Obtenir l’URL de retour, enregistrer en tant que chaîne
                    Uri downloadURL = (Uri) task.getResult();
                    imageURL = downloadURL.toString();

                    // Configurer des données pour 1 document dans le chat de collecte
                    HashMap<String, Object> message = new HashMap<>();
                    message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                    message.put(Constants.KEY_RECEIVER_ID, receivedUser.id);
                    message.put(Constants.KEY_MESSAGE, imageURL);
                    message.put(Constants.KEY_TYPE, "image");
                    message.put(Constants.KEY_TIMESTAMP, new Date());
                    // Enregistrer les données dans la collection de chats, dans le document dont l’ID est chatId
                    database.collection(Constants.KEY_COLLECTION_CHAT)
                            .document(chatId)
                            .set(message)
                            .addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    showToast("Image sent successfully");
                                } else {
                                    showToast(task1.getException().getMessage());
                                }
                            });
                    // Mettre à jour lastMessage
                    updateLastMessage(message);
                    binding.inputMessage.setText(null);

                }
            });
        }
    }

    // Fonction de poignée de mise à jour LastMessage pour la conversation entre 2 utilisateurs
    private void updateLastMessage(HashMap<String, Object> message) {
        // Si 2 utilisateurs ont déjà une conversation entre eux, ne mettez à jour que lastMessage et heure
        //        Au contraire, créez et ajoutez une nouvelle conversation dans la conversation de collection
        if (conversationId != null) {
            updateConversation(message);
        } else {
            // Configurer des données pour un document dans une conversation de collection
            HashMap<String, Object> conversation = new HashMap<>();
            conversation.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversation.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversation.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversation.put(Constants.KEY_RECEIVER_ID, receivedUser.id);
            conversation.put(Constants.KEY_RECEIVER_NAME, receivedUser.name);
            conversation.put(Constants.KEY_RECEIVER_IMAGE, receivedUser.image);
            conversation.put(Constants.KEY_LAST_MESSAGE, message.get(Constants.KEY_MESSAGE));
            conversation.put(Constants.KEY_TYPE, message.get(Constants.KEY_TYPE));
            conversation.put(Constants.KEY_TIMESTAMP, new Date());
            // Ajouter un nouveau document dans la collection
            addConversation(conversation);
        }
        // Si l’utilisateur est hors ligne, envoyez une notification à l’appareil
        if (!isReceiverAvailaible) {
            try {
                // Configurer les données pour les notifications
                JSONArray tokens = new JSONArray();
                tokens.put(receivedUser.token);

                JSONObject data = new JSONObject();
                data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                if (message.get(Constants.KEY_TYPE).equals( "text")) {
                    data.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
                } else if (message.get(Constants.KEY_TYPE).equals("image")) {
                    data.put(Constants.KEY_MESSAGE, String.format("%s %s", preferenceManager.getString(Constants.KEY_NAME), "sent you an image"));
                } else if (message.get(Constants.KEY_TYPE).equals("audio")) {
                    data.put(Constants.KEY_MESSAGE, String.format("%s %s", preferenceManager.getString(Constants.KEY_NAME), "sent you an audio"));
                }
                JSONObject body = new JSONObject();
                body.put(Constants.REMOTE_MSG_DATA, data);
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

                // Fonction de traitement des notifications
                sendNotification(body.toString());
            } catch (Exception e) {
                showToast(e.getMessage());
            }
        }
    }

    // La fonction prend la chaîne de chaîne et affiche
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Fonction de traitement des notifications
    private void sendNotification(String messageBody) {
        // Créer une API, envoyer des notifications à un autre appareil
        APIClient.getClient().create(APIService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            // Gérer la réponse renvoyée
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    try {
                        if (response.body() != null) {
                            JSONObject responseJSON = new JSONObject(response.body());
                            JSONArray results = responseJSON.getJSONArray("results");
                            if (responseJSON.getInt("failure") == 1) {
                                JSONObject error = (JSONObject) results.get(0);
                                showToast(error.getString("error"));
                                return;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    // Si le message est envoyé avec succès, affichez-le
                    showToast("Notification sent successfully");
                } else {
                    // Si tout le reste échoue, affichez une erreur
                    showToast("Error: " + response.code());
                }
            }
            // Si la remise des notifications sortantes échoue, une erreur s’affiche
            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                showToast(t.getMessage());
            }
        });
    }

    // La fonction vérifie l’état de fonctionnement de l’utilisateur (hors ligne / en ligne) en temps réel
    private void listenAvailabilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(receivedUser.id)
                .addSnapshotListener(ChatActivity.this, ((value, error) -> {
                    if (error != null) {
                        return;
                    }
                    if (value != null) {
                        if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                            int availability = Objects.requireNonNull(
                                    value.getLong(Constants.KEY_AVAILABILITY)
                            ).intValue();
                            isReceiverAvailaible = availability == 1;
                        }
                        receivedUser.token = value.getString(Constants.KEY_FCM_TOKEN);
                        if (receivedUser.image == null) {
                            receivedUser.image = value.getString(Constants.KEY_IMAGE);
                            chatAdaptor.setReceiverProfileImage(getBitmapFromEncodedImage(receivedUser.image));
                            chatAdaptor.notifyItemRangeChanged(0, chatMessages.size());
                        }
                    }
                    // Si l’internaute affiche 1 cercle bleu à côté de l’avatar pour identifier
                    //                    Au contraire, cachez-vous
                    if (isReceiverAvailaible) {
                        binding.userAvailability.setVisibility(View.VISIBLE);
                    } else {
                        binding.userAvailability.setVisibility(View.GONE);
                    }
                }));
    }

    // La fonction traite les messages entre 2 utilisateurs en temps réel
    private void listenMessages() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receivedUser.id)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receivedUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }
    // Un écouteur extrait les données des messages entre 2 utilisateurs qui les poussent dans RecyclerView en temps réel
    //    S’il y a un nouveau message, il apparaîtra immédiatement
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.type = documentChange.getDocument().getString(Constants.KEY_TYPE);
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessage.dateTime = getReadableDateTime(chatMessage.dateObject);
                    chatMessages.add(chatMessage);
                }
            }
            // Trier les messages par ordre chronologique
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0) {
                chatAdaptor.notifyDataSetChanged();
            } else {
                chatAdaptor.notifyItemRangeChanged(chatMessages.size(), chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if (conversationId == null) {
            // La fonction vérifie la conversation entre 2 utilisateurs
            checkForConversation();
        }
    };

    // Fonction de conversion d’image de chaîne en bitmap
    private Bitmap getBitmapFromEncodedImage(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }
    }

    // La fonction de traitement prend les données du destinataire et les affecte à l’interface
    private void loadReceiverDetails() {
        receivedUser = getIntent().getParcelableExtra(Constants.KEY_USER);
        binding.textName.setText(receivedUser.name);
    }

    // Convertir les données Date heure en chaîne
    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM, dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    // Ajouter un nouveau document à une conversation de collection
    private void addConversation(HashMap<String, Object> conversation) {
        database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS)
                .add(conversation)
                .addOnSuccessListener(documentReference -> conversationId = documentReference.getId());
    }

    // Mettre à jour des documents dans la conversation sur la collection
    private void updateConversation(HashMap<String, Object> message) {
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS).document(conversationId);
        documentReference.update(
                Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID),
                Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME),
                Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE),
                Constants.KEY_RECEIVER_ID, receivedUser.id,
                Constants.KEY_RECEIVER_NAME, receivedUser.name,
                Constants.KEY_RECEIVER_IMAGE, receivedUser.image,
                Constants.KEY_LAST_MESSAGE, message.get(Constants.KEY_MESSAGE),
                Constants.KEY_TYPE, message.get(Constants.KEY_TYPE),
                Constants.KEY_TIMESTAMP, new Date()
        );
    }

    // Fonction de vérification entre 2 utilisateurs ayant déjà une conversation
    private void checkForConversation() {
        checkForConversationRemoteLy(
                preferenceManager.getString(Constants.KEY_USER_ID),
                receivedUser.id
        );
        checkForConversationRemoteLy(
                receivedUser.id,
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
    }
    // Créer une tâche pour récupérer un document dans une conversation de collection
    //    a KEY_SENDER_ID = senderId
    //    a KEY_RECEIVER_ID = receiverId
    private void checkForConversationRemoteLy(String senderId, String receiverId) {
        database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversationOnCompleteListener);
    }

    // Si la tâche est exécutée avec succès, obtenez et affectez l’ID du document à la variable « conversationId ».
    private final OnCompleteListener<QuerySnapshot> conversationOnCompleteListener = task -> {
        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversationId = documentSnapshot.getId();
        }
    };

    // Si l’utilisateur rouvre l’application, il appellera la fonction pour mettre à jour le statut de l’utilisateur
    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }
}