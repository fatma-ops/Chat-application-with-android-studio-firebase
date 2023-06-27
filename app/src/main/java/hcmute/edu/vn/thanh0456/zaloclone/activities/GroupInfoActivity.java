package hcmute.edu.vn.thanh0456.zaloclone.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

import hcmute.edu.vn.thanh0456.zaloclone.MainActivity;
import hcmute.edu.vn.thanh0456.zaloclone.adaptor.UsersAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.listeners.UserListener;
import hcmute.edu.vn.thanh0456.zaloclone.models.Group;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;
import hcmute.edu.vn.thanh0456.zaloclone.R;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ActivityGroupInfoBinding;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.DialogEditGroupnameBinding;

public class GroupInfoActivity extends AppCompatActivity implements UserListener {

    // Conception de la mise en page et de l’affichage de liaison (vous n’avez pas besoin de créer de variables pour obtenir les propriétés de la mise en page et les utiliser)
    ActivityGroupInfoBinding binding;
    // L’instance enregistre les données relatives au groupe
    private Group group;
    // Image encodée dans le style String
    private String encodeImage;
    // instance de Firestore pour interagir avec les données selon les besoins
    FirebaseFirestore database;

    // Vérifier s’il y a un changement lié au groupe
    //    Pour envoyer les données modifiées à la page précédente, mettez à jour en temps réel
    private Boolean isGroupChange;

    //  Enregistrer ou récupérer des données dans SharePref
    private PreferenceManager preferenceManager;
    //  Enregistrer le rôle de l’utilisateur dans le groupe
    private String role;
    // Adaptor
    private UsersAdaptor usersAdaptor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        setListeners();
        loadGroupInfo();
        getAllUsersOfGroup();
        getRole();
    }

    //  Fonction d’initialisation
    private void init() {
        // Enregistrer ou récupérer des données dans SharePrefs
        preferenceManager = new PreferenceManager(getApplicationContext());
        // Interagir avec les données stockées sur Firestore
        database = FirebaseFirestore.getInstance();
        // Variables d’initialisation, les valeurs changent en fonction du changement du groupe (photo, nom, nombre de membres)
        isGroupChange = false;
    }

    // Fonction de configuration d’événements
    private void setListeners() {
        // Retour à la page précédente
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        // Affiche un menu contextuel avec des options pour modifier le nom du groupe, l’image du groupe, supprimer le groupe, quitter le groupe
        binding.imageMore.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(GroupInfoActivity.this, v);
            popupMenu.getMenuInflater().inflate(R.menu.popup_group, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getItemId() == R.id.changeGroupPhoto) {
                    changeGroupPhoto();

                } else if(menuItem.getItemId() == R.id.changeName) {
                    changeGroupName();

                } else if (menuItem.getItemId() == R.id.deleteGroup) {
                    deleteGroup();

                } else if (menuItem.getItemId() == R.id.leaveGroup) {
//                    leaveGroup();
                }
                return true;
            });
            popupMenu.show();
        });
        // TabLayout View Membre ou administrateur dans le groupe
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    getAllUsersOfGroup();
                }
                else {
                    getAdminOfGroup();
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    // Enregistrer le rôle de l’utilisateur actuel dans le groupe actuel dans la variable
    private void getRole() {
        if (preferenceManager.getString(Constants.KEY_USER_ID).equals(group.ownerId)) {
            role = "owner";
            return;
        }
        for (User member : group.members) {
            if (preferenceManager.getString(Constants.KEY_USER_ID).equals(member.id)) {
                role = member.role;
                return;
            }
        }
    }

    // Fonction de traitement de remplacement d’image de groupe
    private void changeGroupPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pickImage.launch(intent);

    }

    // Fonction de traitement de mise à jour d’image vers Firestore
    private void updateGroupPhoto() {
        group.image = encodeImage;
        binding.imageProfile.setImageBitmap(getBitmapFromEncodedImage(encodeImage));
        database.collection(Constants.KEY_COLLECTION_GROUP)
                .document(group.id)
                .update(Constants.KEY_GROUP_IMAGE, encodeImage);
        isGroupChange = true;
    }

    // Fonction de substitution de nom de groupe
    private void changeGroupName() {
        // Afficher une boîte de dialogue pour modifier le nouveau nom du groupe
        //        Il y a 3 options: supprimer, annuler, enregistrer
        DialogEditGroupnameBinding dialogEditGroupnameBinding = DialogEditGroupnameBinding.inflate(getLayoutInflater());
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogEditGroupnameBinding.getRoot());

        Window window = dialog.getWindow();

        if (window == null) {
            return;
        }

        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        WindowManager.LayoutParams windowAtrributes = window.getAttributes();
        windowAtrributes.gravity = Gravity.CENTER;
        window.setAttributes(windowAtrributes);

        dialogEditGroupnameBinding.edtGroupName.setText(binding.textName.getText().toString());
        dialogEditGroupnameBinding.edtGroupName.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        setDialogListeners(dialogEditGroupnameBinding, dialog);
        dialog.show();

    }

    // Fonction de réglage d’événement pour la boîte de dialogue
    private void setDialogListeners(DialogEditGroupnameBinding dialogEditGroupnameBinding, Dialog dialog) {
        // Vide où entrer le nom du groupe
        dialogEditGroupnameBinding.removeButton.setOnClickListener(v -> {
            dialogEditGroupnameBinding.edtGroupName.setText("");
        });
        // Fermer la boîte de dialogue
        dialogEditGroupnameBinding.cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
        });
        // Mettre à jour le nouveau nom du groupe
        dialogEditGroupnameBinding.saveButton.setOnClickListener(v -> {
            if (!binding.textName.equals(dialogEditGroupnameBinding.edtGroupName)) {
                updateGroupName(dialogEditGroupnameBinding.edtGroupName.getText().toString());
            }
            dialog.dismiss();
        });
    }


    // Mettre à jour le nouveau nom du groupe en Firestore
    private void updateGroupName(String newGroupName) {
        group.name = newGroupName;
        binding.textName.setText(newGroupName);
        database.collection(Constants.KEY_COLLECTION_GROUP)
                .document(group.id)
                .update(Constants.KEY_GROUP_NAME, newGroupName);
        showToast("Updated Group Name");
        isGroupChange = true;
    }

    // Fonction de traitement de suppression de groupe
    private void deleteGroup() {
        // Affiche une boîte de dialogue avec 2 options : oui, non
        //        Sélectionnez oui -> supprimer le groupe, revenir à la page principale
        //        Sélectionnez no -> fermer la boîte de dialogue
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Group")
                .setMessage("Are you sure to delete this group?")
                .setCancelable(true)
                .setPositiveButton("Yes", (dialogInterface, i) -> {
                    database.collection(Constants.KEY_COLLECTION_GROUP)
                            .document(group.id)
                            .delete();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("deleteGroup", "deleteGroup");
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", (dialogInterface, i) -> dialogInterface.cancel())
                .show();
    }

    // Après avoir sélectionné l’avatar du groupe, mettez à jour l’interface, poussez les données d’image vers Firestore
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        binding.imageProfile.setImageBitmap(bitmap);
                        encodeImage = encodeImage(bitmap);
                        updateGroupPhoto();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
    );

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

    // La fonction de traitement tire tous les utilisateurs du groupe, poussant dans le RecyclerView
    private void getAllUsersOfGroup() {
        loading(true);
        if (group.members.size() > 0) {
            usersAdaptor = new UsersAdaptor(group.members, this);
            binding.usersRecyclerView.setAdapter(usersAdaptor);
            binding.usersRecyclerView.setVisibility(View.VISIBLE);
        } else {
            showErrorMessage();
        }
        loading(false);
    }

    // La poignée tire l’utilisateur avec le rôle propriétaire ou administrateur, en poussant dans RecyclerView
    private void getAdminOfGroup() {
        loading(true);
        if (group.members.size() > 0) {
            ArrayList<User> adminList = new ArrayList<>();
            for (User user : group.members) {
                if (user.role.equals("owner") || user.role.equals("admin")) {
                    adminList.add(user);
                }
            }
            usersAdaptor = new UsersAdaptor(adminList, this);
            binding.usersRecyclerView.setAdapter(usersAdaptor);
            binding.usersRecyclerView.setVisibility(View.VISIBLE);
        } else {
            showErrorMessage();
        }
        loading(false);
    }

    // La fonction attache les informations de base du groupe à l’interface
    private void loadGroupInfo() {
        group = getIntent().getParcelableExtra(Constants.KEY_COLLECTION_GROUP);
        binding.textName.setText(group.name);
        binding.imageProfile.setImageBitmap(getBitmapFromEncodedImage(group.image));
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

    // Fonction d’affichage des erreurs
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

        if (user.id.equals(group.ownerId)) {
            if (preferenceManager.getString(Constants.KEY_USER_ID).equals(user.id)) {
                return;
            }
            messageWithMember(user);
            return;
        }

        if (role.equals("owner") || role.equals("admin")) {
            if (preferenceManager.getString(Constants.KEY_USER_ID).equals(user.id)) {
                return;
            }
            PopupMenu popupMenu = new PopupMenu(GroupInfoActivity.this, v);
            popupMenu.getMenuInflater().inflate(R.menu.popup_member_group_for_admin, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getItemId() == R.id.message) {
                    messageWithMember(user);

                } else if (menuItem.getItemId() == R.id.setAdmin) {
                    setAdminToMember(user);

                } else if (menuItem.getItemId() == R.id.removeMember) {
                    removeMember(user);
                }
                return true;
            });
            popupMenu.show();
        } else if (role.equals("member")) {
            if (preferenceManager.getString(Constants.KEY_USER_ID).equals(user.id)) {
                return;
            }
            messageWithMember(user);
        }

    }

    // Fonction de processus pour supprimer des membres d’un groupe
    private void removeMember(User user) {
        CollectionReference groupRef = database.collection(Constants.KEY_COLLECTION_GROUP);
        groupRef.document(group.id)
                .collection(Constants.KEY_COLLECTIONS_MEMBERS)
                .document(user.id)
                .delete();
        showToast("Deleted Member");
        for (User member : group.members) {
            if (member.id.equals(user.id)) {
                group.members.remove(member);
                break;
            }
        }
        usersAdaptor.notifyDataSetChanged();
        isGroupChange = true;

    }

    // La fonction de traitement va à la page de chat avec l’utilisateur sélectionné
    private void messageWithMember(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
        finish();
    }

    // La fonction gère les autorisations d’administration définies pour les membres du groupe
    private void setAdminToMember(User user) {
        CollectionReference groupRef = database.collection(Constants.KEY_COLLECTION_GROUP);
        groupRef.document(group.id)
                .collection(Constants.KEY_COLLECTIONS_MEMBERS)
                .document(user.id)
                .update(Constants.KEY_ROLE, "admin");

        for (User member : group.members) {
            if (member.id.equals(user.id)) {
                member.role = "admin";
                break;
            }
        }
        showToast("Set Admin Successfully");
    }

    // La fonction reçoit la chaîne String transmise et affichée
    private void showToast(String message) {
        Toast.makeText(GroupInfoActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    // La fonction de traitement revient à la page précédente
    //    transmis selon les données modifiées du groupe (photo, nom, nombre de membres)
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(Constants.KEY_COLLECTION_GROUP);
        if (isGroupChange) {
            intent.putExtra(
                    Constants.KEY_COLLECTION_GROUP,
                    group
            );
        }
        if (isGroupChange) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }
}