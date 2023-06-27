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
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

import hcmute.edu.vn.thanh0456.zaloclone.MainActivity;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ActivitySignUpBinding;

// Créer un compte
public class SignUpActivity extends AppCompatActivity {

    // Conception de la mise en page et de l’affichage de liaison
    // (vous n’avez pas besoin de créer de variables pour obtenir les propriétés de la mise en page et les utiliser)
    private ActivitySignUpBinding binding;
    // Enregistrer ou récupérer des données dans SharePref
    private PreferenceManager preferenceManager;
    // Image codée en chaîne
    private String encodeImage;
    // Créez un compte et authentifiez votre adresse e-mail à l’aide de FirebaseAuth
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        setListeners();
    }
    // Fonction d’initialisation
    private void init() {
        // Enregistrer ou récupérer des données à partir de SharePreferences
        preferenceManager = new PreferenceManager(getApplicationContext());
        // Créer un compte FirebaseAuth
        mAuth = FirebaseAuth.getInstance();
    }
    // Fonction de configuration d’événements
    private void setListeners() {
        binding.textSignIn.setOnClickListener(v -> onBackPressed());

        // Valider les informations d’entrée de l’utilisateur, s’il est satisfait,
        // appellera la fonction d’exécution de l’enregistrement
        binding.buttonSignUp.setOnClickListener(v -> {
            if (isValidSignUpDetails()) {
                signUp();
            }
        });
        // Sélectionnez une photo sur votre appareil
        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    // La fonction reçoit la chaîne transmise et affiche
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Fonction d’enregistrement de compte
    private void signUp() {
        loading(true);
        createAccount();
    }
    // Créer un compte
    private void createAccount() {
        // Créer un compte à l’aide de l’adresse e-mail et du mot de passe à l’aide de FirebaseAuth
        mAuth.createUserWithEmailAndPassword(binding.inputEmail.getText().toString(), binding.inputPassword.getText().toString())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showToast("Successfully Registered");
                        insertUser();
                    } else {
                        // Afficher les notifications en cas d’échec
                        loading(false);
                        showToast(task.getException().getMessage()
                        );
                    }
                });
    }
    // Ajouter un utilisateur à Firestore
    private void insertUser() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_NAME, binding.inputName.getText().toString());
        user.put(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());
        user.put(Constants.KEY_IMAGE, encodeImage);
        // Ajouter un utilisateur à la collection dans Firestore
        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    loading(false);
                    // Enregistrer les données utilisateur dans SharePreferences
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(Constants.KEY_NAME, binding.inputName.getText().toString());
                    preferenceManager.putString(Constants.KEY_IMAGE, encodeImage);
                    // Accéder à l’écran d’accueil
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(exception -> {
                    // Afficher les notifications en cas d’échec
                    loading(false);
                    showToast(exception.getMessage());
                });
    }

    // Valider les informations d’inscription saisies par l’utilisateur
    private Boolean isValidSignUpDetails() {
        if (encodeImage == null) {
            showToast("Select profile image");
            return false;
        } else if (binding.inputName.getText().toString().trim().isEmpty()) {
            showToast("Enter name");
            return false;
        } else if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Enter valid email");
            return false;
        } else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        } else if (binding.inputConfirmPassword.getText().toString().trim().isEmpty()) {
            showToast("Confirm your password");
            return false;

        } else if (binding.inputPassword.getText().toString().length() < 6) {
            showToast("Password should be at least 6 characters");
            return false;
        }
        else if (!binding.inputPassword.getText().toString().equals(binding.inputConfirmPassword.getText().toString())) {
            showToast("Password & confirm password must be same");
            return false;
        }
        return true;
    }

    // Encode ảnh từ Bitmap sang String
    // để lưu lên Firestore
    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    // Sélectionnez une photo sur votre appareil
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
                        encodeImage = encodeImage(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
    );


    // Effet de chargement en attendant le chargement des données
    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.buttonSignUp.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }
}