package hcmute.edu.vn.thanh0456.zaloclone.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

import hcmute.edu.vn.thanh0456.zaloclone.adaptor.UsersAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.listeners.UserListener;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ActivitySearchBinding;

public class SearchActivity extends AppCompatActivity implements UserListener {

    // Conception de la mise en page et de l’affichage de liaison (vous n’avez pas besoin de créer de variables pour
    // obtenir les propriétés de la mise en page et les utiliser)
    ActivitySearchBinding binding;
    // Enregistrer ou récupérer des données dans SharePref
    PreferenceManager preferenceManager;
    // instance de Firestore pour interagir avec les données selon les besoins
    FirebaseFirestore database;
    // Liste des utilisateurs
    ArrayList<User> users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        setListeners();
        getUsers();
    }

    // Fonction d’initialisation
    private void init() {
        // Enregistrer ou récupérer des données à partir de SharePrefs
        preferenceManager = new PreferenceManager(getApplicationContext());
        // Interaction avec les données sur Firestore
        database = FirebaseFirestore.getInstance();
        // Enregistrer les données utilisateur
        users = new ArrayList<>();
        // Affichez le clavier pour entrer la saisie dès que vous accédez à cette page
        binding.inputSearch.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    // Fonction de configuration d’événements
    private void setListeners() {
        // Retour à la page précédente
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        // Entrée videRecherche
        binding.imageCancel.setOnClickListener(v -> binding.inputSearch.setText(""));
        // Modifier l’interface, masquer/afficher en fonction de l’état de inputSearch
        binding.inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (binding.inputSearch.getText().length() != 0) {
                    binding.imageCancel.setVisibility(View.VISIBLE);
                } else {
                    binding.imageCancel.setVisibility(View.GONE);
                    binding.textErrorMessage.setVisibility(View.GONE);
                    binding.suggested.setVisibility(View.VISIBLE);
                    getUsers();
                }
            }
        });

        // Effectuer une recherche d’utilisateur par nom en fonction de l’entrée entrée de l’utilisateur dans inputSearch
        binding.inputSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEARCH) {
                    users.clear();
                    String text = binding.inputSearch.getText().toString().toLowerCase().trim();
                    database.collection(Constants.KEY_COLLECTION_USERS)
                            .get()
                            .addOnCompleteListener(task -> {
                                loading(false);
                                String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                                if (task.isSuccessful() && task.getResult() != null) {
                                    for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                                        if (currentUserId.equals(queryDocumentSnapshot.getId())) {
                                            continue;
                                        }
                                        if (queryDocumentSnapshot.getString(Constants.KEY_NAME).toLowerCase().contains(text)) {
                                            User user = new User();
                                            user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                                            user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                                            user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                                            user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                                            user.id = queryDocumentSnapshot.getId();
                                            users.add(user);
                                        }
                                    }
                                    if (users.size() > 0) {

                                        UsersAdaptor usersAdaptor = new UsersAdaptor(users, SearchActivity.this::onUserClicked);
                                        binding.usersRecyclerView.setAdapter(usersAdaptor);
                                        binding.textErrorMessage.setVisibility(View.GONE);
                                        binding.usersRecyclerView.setVisibility(View.VISIBLE);
                                        usersAdaptor.notifyDataSetChanged();
                                    } else {
                                        binding.usersRecyclerView.setVisibility(View.GONE);
                                        showErrorMessage();
                                    }
                                    binding.suggested.setVisibility(View.GONE);
                                } else {
                                    showErrorMessage();
                                    binding.suggested.setVisibility(View.GONE);
                                }
                            });
                    return true;
                }
                return false;
            }
        });
    }

    // La fonction de poignée prend tous les utilisateurs de Firestore et pousse dans RecyclerView
    private void getUsers() {
        users.clear();
        loading(true);
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if (task.isSuccessful() && task.getResult() != null) {
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
                            UsersAdaptor usersAdaptor = new UsersAdaptor(users, this);
                            binding.usersRecyclerView.setAdapter(usersAdaptor);
                            binding.usersRecyclerView.setVisibility(View.VISIBLE);
                            usersAdaptor.notifyDataSetChanged();
                        } else {
                            showErrorMessage();
                        }
                    } else {
                        showErrorMessage();
                    }
                });
    }

    // Fonction qui affiche un message d’erreur
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

    // Lorsque vous cliquez sur un membre, accédez à la page de discussion avec ce membre
    @Override
    public void onUserClicked(View v, User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
        finish();
    }
}