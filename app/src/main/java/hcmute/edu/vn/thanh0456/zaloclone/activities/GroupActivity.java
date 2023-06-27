package hcmute.edu.vn.thanh0456.zaloclone.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import hcmute.edu.vn.thanh0456.zaloclone.adaptor.UsersAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.listeners.UserListener;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ActivityGroupBinding;

public class GroupActivity extends AppCompatActivity implements UserListener{

    // Conception de la mise en page et de l’affichage de liaison
    // (vous n’avez pas besoin de créer de variables pour obtenir les propriétés de la mise en page et les utiliser)
    private static ActivityGroupBinding binding;
    //Image encodée dans le style String
    private String encodeImage;
    // Enregistrer ou récupérer des données dans SharePref
    private PreferenceManager preferenceManager;
    // ID de la discussion de groupe
    private String groupId = null;
    // Liste des utilisateurs sélectionnés pour ajouter au groupe
    private static ArrayList<User> selectedUser;
    // Interaction avec les données sur Firestore
    private FirebaseFirestore database;
    // Référence au groupe de collecte
    private CollectionReference memberRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        setContentView(binding.getRoot());
        setListeners();
        getUsers();
    }

    // Fonction d’initialisation
    private void init() {
        binding = ActivityGroupBinding.inflate(getLayoutInflater());
        // Enregistrer ou récupérer des données dans SharePref
        preferenceManager = new PreferenceManager(getApplicationContext());
        // Contient les utilisateurs à ajouter au groupe
        selectedUser = new ArrayList<>();
        // instance de Firestore pour interagir avec les données selon les besoins
        database = FirebaseFirestore.getInstance();
        // Référence au groupe de collecte
        memberRef = database.collection(Constants.KEY_COLLECTION_GROUP);
    }

    //  Générateur de conversation de groupe
    private void createGroup() {
        // Configurer les données
        HashMap<String, Object> group = new HashMap<>();
        group.put(Constants.KEY_OWNER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        group.put(Constants.KEY_OWNER_NAME, preferenceManager.getString(Constants.KEY_NAME));
        group.put(Constants.KEY_GROUP_IMAGE, encodeImage);
        group.put(Constants.KEY_GROUP_NAME, binding.inputGroupName.getText().toString());
        group.put(Constants.KEY_TIMESTAMP, new Date());
        DocumentReference ref = database.collection(Constants.KEY_COLLECTION_GROUP).document();
        groupId = ref.getId();
        // Créer un document dans un groupe de collections
        addGroup(group);
        // Créer un document dans un groupe de collections
        addMemberToGroup();
        // Retourner à GroupFragment
        onBackPressed();
    }

    // La fonction de traitement crée un nouveau document dans un groupe de collection
    private void addGroup(HashMap<String, Object> group) {
        database.collection(Constants.KEY_COLLECTION_GROUP)
                .document(groupId)
                .set(group);
    }

    // La fonction de traitement ajoute l’utilisateur au document nouvellement créé
    private void addMemberToGroup() {
        // Ajouter l’utilisateur en tant que propriétaire du groupe
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        user.put(Constants.KEY_ROLE, "owner");
        memberRef.document(groupId).collection(Constants.KEY_COLLECTIONS_MEMBERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .set(user);

        // Ajouter un utilisateur membre du groupe
        for(User user1 : selectedUser) {
            HashMap<String, Object> user2 = new HashMap<>();
            user2.put(Constants.KEY_USER_ID, user1.id);
            user2.put(Constants.KEY_ROLE, "member");
            memberRef.document(groupId).collection(Constants.KEY_COLLECTIONS_MEMBERS)
                    .document(user1.id)
                    .set(user2);
        }
        showToast("Group Created");
    }

    // Fonction de configuration d’événements
    private void setListeners() {
        // Sélectionnez une photo sur votre appareil
        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
        // Retour à la page précédente
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        // Créer un groupe
        binding.buttonCreate.setOnClickListener(v -> {
            if (!selectedUser.isEmpty()) {
                createGroup();
            }
        });
    }

    // La fonction extrait les données utilisateur de Firestore, les pousse dans RecyclerView
    private void getUsers() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if (task.isSuccessful() && task.getResult() != null) {
                        ArrayList<User> users = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            if (currentUserId.equals(queryDocumentSnapshot.getId())) {
                                continue;
                            }
                            User user = new User();
                            user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                            user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                            user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                            user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            user.id = queryDocumentSnapshot.getId();
                            users.add(user);
                        }
                        if (users.size() > 0) {
                            UsersAdaptor usersAdaptor = new UsersAdaptor(users, null);
                            binding.usersRecyclerView.setAdapter(usersAdaptor);
                            binding.usersRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessage();
                        }
                    } else {
                        // Si la tâche échoue, une erreur s’affiche
                        showErrorMessage();
                    }
                });
    }

    // Fonction d’affichage des erreurs (pour le débogage)
    private void showErrorMessage() {
        binding.textErrorMessage.setText(String.format("%s", "No user available"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }

    // Effet de chargement en attendant le chargement des données
    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    // Convertir des images bitmap en chaîne
    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    //  Après avoir sélectionné une photo de profil pour le groupe, affichez-la sur l’interface
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        binding.imageProfile.setImageBitmap(bitmap);
                        binding.textAddImage.setVisibility(View.GONE);
                        // affecter l’avatar codé à String à la variable
                        //                        attendez la poignée push up enregistrée dans Firestore
                        encodeImage = encodeImage(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
    );

    // Modifier la fonction de traitement dans la liste sélectionnéeUtilisateur
    public static void onCheckedChangeListener(User user, Boolean isSelect) {
        if (isSelect) {
            selectedUser.add(user);
        } else {
            selectedUser.remove(user);
        }
        if (selectedUser.isEmpty()) {
            loadingButton(false);
        } else {
            loadingButton(true);
        }
    }

    // Effet de chargement en attendant le chargement des données
    public static void loadingButton(Boolean isLoading) {
        if (isLoading) {
            binding.buttonCreate.setVisibility(View.VISIBLE);
        } else {
            binding.buttonCreate.setVisibility(View.INVISIBLE);
        }
    }

    // La fonction reçoit la chaîne String transmise et affichée
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // La fonction est vide en raison de la réutilisation de UsersAdaptor mais il n’est pas nécessaire de cliquer sur l’utilisateur pour accéder à la page de chat
    @Override
    public void onUserClicked(View v, User user) {
        // do nothing
    }

}