package hcmute.edu.vn.thanh0456.zaloclone.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


import hcmute.edu.vn.thanh0456.zaloclone.MainActivity;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;
import hcmute.edu.vn.thanh0456.zaloclone.R;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ActivitySignInBinding;

public class SignInActivity extends AppCompatActivity  {


//Conception de la mise en page et de l’affichage de liaison (vous
// n’avez pas besoin de créer de variables pour obtenir les propriétés de la mise en page et les utiliser)
    private ActivitySignInBinding binding;
    //Enregistrer ou récupérer des données dans SharePref)
    private PreferenceManager preferenceManager;
    //Créez un compte et authentifiez votre adresse e-mail à l’aide de FirebaseAuth
    private FirebaseAuth mAuth;
    private TextView forgetPassword;
    private EditText password ;
    private boolean passwordVisible ;
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        password=findViewById(R.id.inputPassword);











        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        setListeners();



       // password.setOnTouchListener((v, event) -> {
           // final int Right =2 ;
           // if(event.getAction()==MotionEvent.ACTION_UP){
              //  if(event.getRawX()>=password.getRight()-password.getCompoundDrawables()[Right].getBounds().width()){
                   // int selection =password.getSelectionEnd();
                   // if(passwordVisible){
                        // set drawble image here
                       // password.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0, R.drawable.ic_visibility_off,0);
                       // password.setTransformationMethod(PasswordTransformationMethod.getInstance());
                       // passwordVisible=false;

                   // }else {
                       // password.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,R.drawable.ic_visibility,0);
                       // password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                       // passwordVisible=true;


                   // }
                    //password.setSelection(selection);
                 //   return true ;
                //}
           // }
           // return false;
       // });
















    }

    //Fonction d’initialisation
    private void init() {
        //  Enregistrer ou récupérer des données à partir de SharePreferences
        preferenceManager = new PreferenceManager(getApplicationContext());
        //Se connecter avec FirebaseAuth
        mAuth = FirebaseAuth.getInstance();
    }
     //Fonction de configuration d’événements
    private void setListeners() {
        //  Aller à la page du mot de passe oublié

        binding.forget.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ForgetPassword.class)));
        //  Aller à la page d’enregistrement du compte

        binding.textCreateNewAccount.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));
       // Validez les données d’entrée, si elles sont valides, puis appelez la fonction de gestionnaire de connexion signIn
        binding.buttonSignIn.setOnClickListener(v -> {
            if (isValidSignUpDetails()) {
                signIn();
            }
        });

       // Pas besoin de se reconnecter
        //Accéder à la page principale si SharePref stocke des informations utilisateur
        if(preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }

    }

// La fonction reçoit la chaîne transmise et affiche
    private void showToast(String message) {

        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
//  Fonction de traitement de connexion
    private void signIn() {
        loading(true);
//Connectez-vous avec une entrée utilisateur, à l’aide de FirebaseAuth
        mAuth.signInWithEmailAndPassword(binding.inputEmail.getText().toString(), binding.inputPassword.getText().toString())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showToast("Successfully Logged In");
                        storeCurrentUserInPrefs();
                    }
                    else {
                        loading(false);
                        showToast(task.getException().getMessage());
                    }
                });
    }
// Obtenez les données utilisateur de Firestore et enregistrez-les dans SharePrefs
    private void storeCurrentUserInPrefs() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, binding.inputEmail.getText().toString())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null
                            && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                        preferenceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME));

                        preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE));
                        // Aller à la page principale
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        loading(false);
                        showToast(task.getException().getMessage());
                    }
                });
    }
//Effet de chargement en attendant le chargement des données
    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.buttonSignIn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.buttonSignIn.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }
// Valider les informations d’inscription saisies par l’utilisateur
    private Boolean isValidSignUpDetails() {
        if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Enter valid email");
            return false;
        } else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        }
        return true;
    }
//    private void addDataToFirestore() {
//        FirebaseFirestore database = FirebaseFirestore.getInstance();
//        HashMap<String, Object> data = new HashMap<>();
//        data.put("first_name", "Thanh");
//        data.put("last_name", "Ninh");
//        database.collection("users")
//                .add(data)
//                .addOnSuccessListener(documentReference -> {
//                    Toast.makeText(getApplicationContext(), "Data Inserted", Toast.LENGTH_SHORT).show();
//                })
//                .addOnFailureListener(exception -> {
//                    Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_SHORT).show();
//                });
//    }
}