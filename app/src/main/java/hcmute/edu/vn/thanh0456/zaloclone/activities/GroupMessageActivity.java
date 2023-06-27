package hcmute.edu.vn.thanh0456.zaloclone.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import hcmute.edu.vn.thanh0456.zaloclone.adaptor.GroupChatAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.adaptor.RecentGroupAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.models.Group;
import hcmute.edu.vn.thanh0456.zaloclone.models.GroupLastMessageModel;
import hcmute.edu.vn.thanh0456.zaloclone.models.GroupMessage;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Permissions;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ActivityGroupMessageBinding;

public class GroupMessageActivity extends AppCompatActivity {
    // Conception de la mise en page et de l’affichage de liaison (vous n’avez pas besoin de créer de variables pour obtenir les propriétés de la mise en page et les utiliser)
    private ActivityGroupMessageBinding binding;
    // instance de Firestore pour interagir avec les données selon les besoins
    private FirebaseFirestore database;
    // Enregistrer ou récupérer des données dans SharePref
    private PreferenceManager preferenceManager;
    // L’instance enregistre les données relatives au groupe
    private Group group;
    // L’instance enregistre les données relatives au dernier message du groupe
    GroupLastMessageModel lastMessageModel;
    // Enregistrer l’historique des messages dans les groupes
    private ArrayList<GroupMessage> groupMessages;
    // Adaptor
    private GroupChatAdaptor groupChatAdaptor;
    // ID du message envoyé dans le groupe
    private String groupMessageId = null;
    // Chemin d’accès aux photos, audio stockés sur Firestore’s Storage
    private String imageURL = null, audioURL = null;
    // Pousser des photos, de l’audio vers le stockage
    private StorageTask uploadTask;
    // URI du lien vers la photo, audio stocké sur l’appareil
    private Uri imageUri, audioUri;
    // Créer un enregistrement
    private MediaRecorder mediaRecorder;
    // Chemin d’accès au fichier d’enregistrement
    private String audioPath;
    // Mettez en pause l’affichage du chat inférieur de l’interface utilisateur, attendez la fin de l’enregistrement d’annulation de l’animation
    Handler handlerUI = new Handler();
    // Enregistrer la liste des membres et des avatars du groupe
    private ArrayList<HashMap<String, Object>> memberAndImageHashMapList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupMessageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loadGroupDetails();
        setListeners();
        init();
        listenMessages();
    }

    // Fonction de démarrage à
    private void init() {
        //Enregistrer ou récupérer des données à partir de SharePrefs
        preferenceManager = new PreferenceManager(getApplicationContext());
        // Enregistrer les messages de groupe
        groupMessages = new ArrayList<>();
        // Configuration de l’adaptateur
        groupChatAdaptor = new GroupChatAdaptor(
                groupMessages,
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        // Connectez l’adaptateur au RecyclerView
        binding.chatRecyclerView.setAdapter(groupChatAdaptor);
        // Interaction avec les données sur Firestore
        database = FirebaseFirestore.getInstance();
        // Définir le bouton recordView pour l’enregistrement
        binding.recordButton.setRecordView(binding.recordView);
        binding.recordButton.setListenForRecord(false);
        // Enregistrer les données sur les derniers messages de groupe
        lastMessageModel = new GroupLastMessageModel();
    }

    // Fonction de configuration d’événements
    private void setListeners() {
        // Masquer/afficher l’interface de chat en bas en fonction des messages saisis par l’utilisateur
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
        //Retour à la page précédente
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        // Appeler la fonction de traitement des messages texte
        binding.imageSend.setOnClickListener(v -> sendMessage());
        // Appelez la fonction de traitement des messages d’image
        binding.imagePhoto.setOnClickListener(v -> sendImage());
        // Appelez la fonction de traitement pour accéder à la page d’affichage des informations du groupe
        binding.imageInfo.setOnClickListener(v -> showInfoGroup());
        // Accorder des autorisations d’enregistrement à l’application
        binding.recordButton.setOnClickListener(v -> {
            if (Permissions.isRecordingok(GroupMessageActivity.this)) {
                binding.recordButton.setListenForRecord(true);
            } else {
                Permissions.requestRecording(GroupMessageActivity.this);
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
                //On Swipe To Cancel
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

            // La fonction de traitement supprime le bouton d’enregistrement pour terminer l’enregistrement
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
                //When the record time is less than One Second
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
        // Définir l’animation du bouton d’enregistrement
        binding.recordView.setOnBasketAnimationEndListener(() -> {
            Log.d("RecordView", "Basket Animation Finished");
            binding.layoutBottomLeft.setVisibility(View.VISIBLE);
            binding.inputMessage.setVisibility(View.VISIBLE);
            binding.layoutLikeAndSend.setVisibility(View.VISIBLE);
        });
    }

    // La fonction de traitement accède à la page d’informations du groupe
    private void showInfoGroup() {
        Intent intent = new Intent(getApplicationContext(), GroupInfoActivity.class);
        intent.putExtra(Constants.KEY_COLLECTION_GROUP, group);
        startActivity(intent);
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

        DocumentReference ref = database.collection(Constants.KEY_COLLECTION_GROUP_MESSAGE).document();
        groupMessageId = ref.getId();

        StorageReference audioPathOnFireBase = storageReference.child(groupMessageId + "." + "3gp");
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
                // Configurer les données requises pour un document dans un groupe de collections
                Uri downloadURL = (Uri) task.getResult();
                audioURL = downloadURL.toString();

                lastMessageModel.dateObject = new Date();
                lastMessageModel.message = audioURL;
                lastMessageModel.senderId = preferenceManager.getString(Constants.KEY_USER_ID);
                lastMessageModel.type = "audio";

                HashMap<String, Object> message = new HashMap<>();
                message.put(Constants.KEY_SENDER_ID, lastMessageModel.senderId);
                message.put(Constants.KEY_MESSAGE, lastMessageModel.message);
                message.put(Constants.KEY_TYPE, lastMessageModel.type);
                message.put(Constants.KEY_TIMESTAMP, lastMessageModel.dateObject);

                // Mettre à jour le dernier message de groupe vers Firestore dans la sous-collection lastMessage du groupe de collections
                database.collection(Constants.KEY_COLLECTION_GROUP)
                        .document(group.id)
                        .collection(Constants.KEY_COLLECTION_LAST_MESSAGE)
                        .document(group.id)
                        .update(message);

                message.put(Constants.KEY_GROUP_ID, group.id);
                // Créer un document dans la collection groupMessage avec group.id id, transférer les données dans le document
                database.collection(Constants.KEY_COLLECTION_GROUP_MESSAGE)
                        .document(groupMessageId)
                        .set(message)
                        .addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                showToast("Audio sent successfully");
                            } else {
                                showToast(task1.getException().getMessage());
                            }
                        });

                binding.inputMessage.setText(null);
            }
        });
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
            // Obtenir un uri à partir d’une photo, push vers Firebase Storage, enregistrer dans le dossier « Image Files »
            imageUri = data.getData();
            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files");

            DocumentReference ref = database.collection(Constants.KEY_COLLECTION_GROUP_MESSAGE).document();
            groupMessageId = ref.getId();

            StorageReference imagePath = storageReference.child(groupMessageId + "." + "jpg");
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

                    lastMessageModel.dateObject = new Date();
                    lastMessageModel.message = imageURL;
                    lastMessageModel.senderId = preferenceManager.getString(Constants.KEY_USER_ID);
                    lastMessageModel.type = "image";

                    // Définir les données de 1 document dans la sous-collection lastMessage dans le groupe de collections
                    HashMap<String, Object> message = new HashMap<>();
                    message.put(Constants.KEY_SENDER_ID, lastMessageModel.senderId);
                    message.put(Constants.KEY_MESSAGE, lastMessageModel.message);
                    message.put(Constants.KEY_TYPE, lastMessageModel.type);
                    message.put(Constants.KEY_TIMESTAMP, lastMessageModel.dateObject);

                    // Mettre à jour les données définies sur Firestore dans la sous-collection lastMessage dans le groupe de collections
                    //                    Données relatives aux derniers messages de groupe
                    database.collection(Constants.KEY_COLLECTION_GROUP)
                            .document(group.id)
                            .collection(Constants.KEY_COLLECTION_LAST_MESSAGE)
                            .document(group.id)
                            .update(message);

                    message.put(Constants.KEY_GROUP_ID, group.id);
                    // Créer un document dans la collection groupMessage avec group.id id, transférer les données dans le document
                    database.collection(Constants.KEY_COLLECTION_GROUP_MESSAGE)
                            .document(groupMessageId)
                            .set(message)
                            .addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    showToast("Image sent successfully");
                                } else {
                                    showToast(task1.getException().getMessage());
                                }
                            });

//                    updateLastMessage(message);
                    binding.inputMessage.setText(null);
                }
            });
        }
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

    // Télécharger les informations de base du groupe telles que la photo, le nom, la liste des membres
    //    et afficher sur l’interface
    private void loadGroupDetails() {
        group = getIntent().getParcelableExtra(Constants.KEY_COLLECTION_GROUP);
        binding.textName.setText(group.name);
        binding.imageProfile.setImageBitmap(getBitmapFromEncodedImage(group.image));
        memberAndImageHashMapList = new ArrayList<>();
        for (User user : group.members) {
            HashMap<String, Object> memberAndImageHashMap = new HashMap<>();
            memberAndImageHashMap.put(Constants.KEY_USER_ID, user.id);
            memberAndImageHashMap.put(Constants.KEY_IMAGE, getBitmapFromEncodedImage(user.image));
            memberAndImageHashMapList.add(memberAndImageHashMap);
        }
    }

    // Fonction de traitement des messages texte
    private void sendMessage() {
        lastMessageModel.dateObject = new Date();
        lastMessageModel.message = binding.inputMessage.getText().toString();
        lastMessageModel.senderId = preferenceManager.getString(Constants.KEY_USER_ID);
        lastMessageModel.type = "text";

        // Définir les données d’un document dans la sous-collection lastMessage du groupe de collections
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, lastMessageModel.senderId);
        message.put(Constants.KEY_MESSAGE, lastMessageModel.message);
        message.put(Constants.KEY_TYPE, lastMessageModel.type);
        message.put(Constants.KEY_TIMESTAMP, lastMessageModel.dateObject);

        // Mettre à jour les données définies sur Firestore dans la sous-collection lastMessage dans le groupe de collections
        //        Données relatives aux derniers messages de groupe
        database.collection(Constants.KEY_COLLECTION_GROUP)
                .document(group.id)
                .collection(Constants.KEY_COLLECTION_LAST_MESSAGE)
                .document(group.id)
                .update(message);

        message.put(Constants.KEY_GROUP_ID, group.id);
        // Créer un document dans la collection groupMessage avec group.id id, transférer les données dans le document
        database.collection(Constants.KEY_COLLECTION_GROUP_MESSAGE)
                .add(message);


        group.lastMessageModel = lastMessageModel;
        // Mettre à jour RecyclerView
        RecentGroupAdaptor.updateLastMessage(group);
        binding.inputMessage.setText(null);
    }

    // La fonction de traitement suit l’évolution des messages dans le groupe
    private void listenMessages() {
        database.collection(Constants.KEY_COLLECTION_GROUP_MESSAGE)
                .whereEqualTo(Constants.KEY_GROUP_ID, group.id)
                .addSnapshotListener(eventListener);
    }

    // Mettre à jour et afficher les messages en temps réel
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            int count = groupMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    GroupMessage groupMessage = new GroupMessage();
                    groupMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);

                    // Convertir l’avatar d’un membre du groupe String en Bitmap
                    //                    Stocké dans la propriété imageBitmap
                    //                    Utilisé pour afficher avec le message envoyé par le membre
                    for (HashMap<String, Object> hashMap : memberAndImageHashMapList) {
                        if (hashMap.get(Constants.KEY_USER_ID).equals(groupMessage.senderId)) {
                            groupMessage.imageBitmap = (Bitmap) hashMap.get(Constants.KEY_IMAGE);
                            break;
                        }
                    }
                    // Définir des valeurs pour les propriétés de l’instance groupMessage
                    groupMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    groupMessage.type = documentChange.getDocument().getString(Constants.KEY_TYPE);
                    groupMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    groupMessage.dateTime = getReadableDateTime(groupMessage.dateObject);
                    // Ajouter des instances à la liste des messages
                    groupMessages.add(groupMessage);
                }
            }
            // Trier les messages par ordre chronologique
            Collections.sort(groupMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0) {
                // Mettre à jour RecyclerView
                groupChatAdaptor.notifyDataSetChanged();
            } else {
                // Mettre à jour RecyclerView
                groupChatAdaptor.notifyItemRangeChanged(groupMessages.size(), groupMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(groupMessages.size() - 1);
                groupChatAdaptor.notifyDataSetChanged();
            }

            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
//        if (conversationId == null) {
//            checkForConversation();
//        }
    };

    // Convertir l’heure de la date en chaîne
    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM, dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    // La fonction de traitement affiche les extraits de message
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // La fonction reçoit des données si le groupe a un changement tel que le nom, la photo, le nombre de membres
    //    Mettre à jour l’état du groupe vers la dernière version
    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Group changedGroup = intent.getParcelableExtra(Constants.KEY_COLLECTION_GROUP);
            if (changedGroup != null) {
                if (changedGroup.image != null && !changedGroup.image.equals(group.image)) {
                    binding.imageProfile.setImageBitmap(getBitmapFromEncodedImage(changedGroup.image));
                    showToast("Changed Image");
                }
                if (changedGroup.name != null && !changedGroup.name.equals(group.name)) {
                    binding.textName.setText(changedGroup.name);
                    showToast("Changed Name");
                }
                group = changedGroup;
                showToast("Changed Group");
            }
        }
    };

    // Au début de l’activité, l’enregistrement de la fonction recevra les données transmises par une autre activité
    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                invitationResponseReceiver,
                new IntentFilter(Constants.KEY_COLLECTION_GROUP)
        );
    }


    // Se désabonner de la fonction de réception lorsque l’activité est arrêtée
    @Override
    protected void onDestroy() {
        // Se désinscrire lorsque l’activité est terminée
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );
        super.onDestroy();
    }

}