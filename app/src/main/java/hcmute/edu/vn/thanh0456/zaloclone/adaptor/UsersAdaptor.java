package hcmute.edu.vn.thanh0456.zaloclone.adaptor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;

import hcmute.edu.vn.thanh0456.zaloclone.activities.GroupActivity;
import hcmute.edu.vn.thanh0456.zaloclone.listeners.UserListener;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ItemContainerUserBinding;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ItemContainerUserGroupBinding;

// L’adaptateur gère les modifications liées à Utilisateurs RecyclerView (liste des utilisateurs)
//Les utilisateurs peuvent cliquer sur n’importe quel utilisateur pour commencer à discuter les uns avec les autres
public class UsersAdaptor extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    // Liste des utilisateurs
    private final ArrayList<User> users;
    // Gérer l’événement lorsque l’utilisateur clique sur un autre utilisateur,
    // créera une conversation et accède à la page de discussion
    private final UserListener userListener;

   // En raison de la réutilisation de UsersAdaptor dans la sélection des utilisateurs à ajouter au groupe
   // il divisera donc 2 cas correspondant à 2 conceptions ViewHolder
    //un pour afficher la liste des utilisateurs pour commencer à discuter si vous cliquez dessus
    //un pour afficher une liste d’utilisateurs à choisir et à ajouter au groupe
    public static final int VIEW_TYPE_FRAGMENT_USER = 1;
    public static final int VIEW_TYPE_FRAGMENT_GROUP = 2;
    private final int VIEW_TYPE;

    //  Le constructeur prend en compte la liste des utilisateurs, spécifie VIEW_TYPE afficher
    //  le type de conception ViewHolder correspondant
    public UsersAdaptor(ArrayList<User> users, UserListener userListener) {
        this.users = users;
        this.userListener = userListener;
        VIEW_TYPE = getViewType(userListener);
    }

    // Créez un ViewHolder basé sur le VIEW_TYPE défini
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (this.VIEW_TYPE == VIEW_TYPE_FRAGMENT_USER) {
            ItemContainerUserBinding itemContainerUserBinding = ItemContainerUserBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new UserViewHolder(itemContainerUserBinding);
        } else{
            ItemContainerUserGroupBinding itemContainerUserGroupBinding = ItemContainerUserGroupBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new UserGroupViewHolder(itemContainerUserGroupBinding);
        }
    }

    // Accrocher des données à ViewHolder en fonction de VIEW_TYPE définies
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (this.VIEW_TYPE == VIEW_TYPE_FRAGMENT_USER) {
            ((UserViewHolder)holder).setUserData(users.get(position));
        } else {
            ((UserGroupViewHolder)holder).setUserData(users.get(position));
        }
    }

    // Renvoie le nombre d’utilisateurs
    @Override
    public int getItemCount() {
        return users.size();
    }

    // Déterminer VIEW_TYPE en fonction de l’utilisation ou non de userListener
    public int getViewType(UserListener userListener) {
        if (userListener != null) {
            return VIEW_TYPE_FRAGMENT_USER;
        }
        return VIEW_TYPE_FRAGMENT_GROUP;
    }

    // La classe ViewHolder utilisée pour UsersActivity
    class UserViewHolder extends RecyclerView.ViewHolder {
        ItemContainerUserBinding binding;
        // conception de liaison ViewHolder
        UserViewHolder(ItemContainerUserBinding itemContainerUserBinding) {
            super(itemContainerUserBinding.getRoot());
            binding = itemContainerUserBinding;
        }
        // Accrochage des données à ViewHolder
        void setUserData(User user) {
            binding.textName.setText(user.name);
            binding.textEmail.setText(user.email);
            binding.imageProfile.setImageBitmap(getUserImage(user.image));
            binding.getRoot().setOnClickListener(v -> userListener.onUserClicked(v, user));
        }
    }

    // Classe ViewHolder utilisée pour GroupActivity
    class UserGroupViewHolder extends RecyclerView.ViewHolder {
        ItemContainerUserGroupBinding binding;
        // conception de liaison ViewHolder
        UserGroupViewHolder(ItemContainerUserGroupBinding itemContainerUserGroupBinding) {
            super(itemContainerUserGroupBinding.getRoot());
            binding = itemContainerUserGroupBinding;
        }
        // Accrochage des données à ViewHolder
        void setUserData(User user) {
            binding.textName.setText(user.name);
            binding.textEmail.setText(user.email);
            binding.imageProfile.setImageBitmap(getUserImage(user.image));
            binding.cbSelectUser.setOnCheckedChangeListener((compoundButton, b) -> {
                GroupActivity.onCheckedChangeListener(user, compoundButton.isChecked());
            });
        }
    }

    // Convertir des images de chaîne en bitmap
    private Bitmap getUserImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

}
