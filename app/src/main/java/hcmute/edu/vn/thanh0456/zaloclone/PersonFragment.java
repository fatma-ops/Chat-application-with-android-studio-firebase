package hcmute.edu.vn.thanh0456.zaloclone;

import static android.app.Activity.RESULT_OK;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

import hcmute.edu.vn.thanh0456.zaloclone.activities.SignInActivity;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.DialogEditUsernameBinding;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.FragmentPersonBinding;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PersonFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PersonFragment extends Fragment{

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private @NonNull
    FragmentPersonBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    // Image encodée dans le style String
    private String encodeImage;

    public PersonFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PersonFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PersonFragment newInstance(String param1, String param2) {
        PersonFragment fragment = new PersonFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentPersonBinding.inflate(inflater, container, false);
        preferenceManager = new PreferenceManager(getActivity().getApplicationContext());
        init();
        setListeners();
        return binding.getRoot();
    }

    // Fonction d’initialisation
    private void init() {
        binding.imageProfile.setImageBitmap(getConversationImage(preferenceManager.getString(Constants.KEY_IMAGE)));
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        //instance de Firestore pour interagir avec les données sur Firestore
        database = FirebaseFirestore.getInstance();
    }

    // Fonction de configuration d’événements
    private void setListeners() {
        binding.signout.setOnClickListener(v -> signOut());
        // Affiche un menu contextuel avec des options pour modifier le nom du groupe, l’image du groupe, supprimer le groupe, quitter le groupe
        binding.imageMore.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(getActivity(), v);
            popupMenu.getMenuInflater().inflate(R.menu.popup_user, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getItemId() == R.id.changePhoto) {
                    changeUserPhoto();
                } else if(menuItem.getItemId() == R.id.changeName) {
                    changeUserName();
                }
                return true;
            });
            popupMenu.show();
        });
    }

    // La fonction gère le remplacement de l’image utilisateur
    private void changeUserPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pickImage.launch(intent);
    }

    // Après avoir sélectionné l’avatar de l’utilisateur, mettez à jour l’interface, poussez les données d’image vers Firestore
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream inputStream = MainActivity.getContextOfApplication().getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        binding.imageProfile.setImageBitmap(bitmap);
                        encodeImage = encodeImage(bitmap);
                        updateUserPhoto();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
    );

    // Mettre à jour les nouvelles photos dans SharePrefs et Firestore
    private void updateUserPhoto() {
        preferenceManager.putString(Constants.KEY_IMAGE, encodeImage);
        binding.imageProfile.setImageBitmap(getBitmapFromEncodedImage(encodeImage));
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .update(Constants.KEY_IMAGE, encodeImage);
        database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, (preferenceManager.getString(Constants.KEY_USER_ID)))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                        if (documentSnapshot.exists()) {
                            documentSnapshot
                                    .getReference()
                                    .update(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
                        }
                    }
                });
        database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, (preferenceManager.getString(Constants.KEY_USER_ID)))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                        if (documentSnapshot.exists()) {
                            documentSnapshot
                                    .getReference()
                                    .update(Constants.KEY_RECEIVER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
                        }
                    }
                });
        showToast("Updated Photo");
    }

    // La fonction gère le changement de nom d’utilisateur
    private void changeUserName() {
        // Afficher une boîte de dialogue pour modifier le nouveau nom de l’utilisateur
        //        Il y a 3 options: supprimer, annuler, enregistrer
        DialogEditUsernameBinding dialogEditUsernameBinding = DialogEditUsernameBinding.inflate(getLayoutInflater());
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogEditUsernameBinding.getRoot());

        Window window = dialog.getWindow();

        if (window == null) {
            return;
        }

        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        WindowManager.LayoutParams windowAtrributes = window.getAttributes();
        windowAtrributes.gravity = Gravity.CENTER;
        window.setAttributes(windowAtrributes);

        dialogEditUsernameBinding.edtUserName.setText(binding.textName.getText().toString());
        dialogEditUsernameBinding.edtUserName.requestFocus();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(getActivity().getApplicationContext().INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        setDialogListeners(dialogEditUsernameBinding, dialog);
        dialog.show();

    }

    // Fonction de réglage d’événement pour la boîte de dialogue
    private void setDialogListeners(DialogEditUsernameBinding dialogEditUsernameBinding, Dialog dialog) {
        // Vide où entrer le nom d’utilisateur
        dialogEditUsernameBinding.removeButton.setOnClickListener(v -> {
            dialogEditUsernameBinding.edtUserName.setText("");
        });
        // Fermer la boîte de dialogue
        dialogEditUsernameBinding.cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
        });
        // Mettre à jour le nouveau nom de l’utilisateur
        dialogEditUsernameBinding.saveButton.setOnClickListener(v -> {
            if (!binding.textName.equals(dialogEditUsernameBinding.edtUserName)) {
                updateUserName(dialogEditUsernameBinding.edtUserName.getText().toString());
            }
            dialog.dismiss();
        });
    }

    // Mettre à jour les nouveaux noms dans SharePrefs et Firestore
    private void updateUserName(String newName) {
        preferenceManager.putString(Constants.KEY_NAME, newName);
        binding.textName.setText(newName);
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .update(Constants.KEY_NAME, newName);
        database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, (preferenceManager.getString(Constants.KEY_USER_ID)))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                        if (documentSnapshot.exists()) {
                            documentSnapshot
                                    .getReference()
                                    .update(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
                        }
                    }
                });
        database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, (preferenceManager.getString(Constants.KEY_USER_ID)))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                        if (documentSnapshot.exists()) {
                            documentSnapshot
                                    .getReference()
                                    .update(Constants.KEY_RECEIVER_NAME, preferenceManager.getString(Constants.KEY_NAME));
                        }
                    }
                });
        showToast("Updated Name");
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

    // Fonction de conversion d’image bitmap en chaîne
    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    // Décoder des images de String vers Bitmap
    private Bitmap getConversationImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    // La fonction reçoit la chaîne String transmise et affichée
    private void showToast(String message) {
        Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // La fonction de déconnexion, lorsqu’elle est appelée, supprime FCM_TOKEN dans le document de l’utilisateur sur Firestore, supprimant les données utilisateur dans SharePrefs
    //    et allez à la page de connexion
    private void signOut() {
        showToast("Signing out...");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    FirebaseAuth.getInstance().signOut();
                    preferenceManager.clear();
                    startActivity(new Intent(getActivity().getApplicationContext(), SignInActivity.class));
                    getActivity().finish();
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }

}