package hcmute.edu.vn.thanh0456.zaloclone.listeners;

import android.view.View;

import hcmute.edu.vn.thanh0456.zaloclone.models.User;

// Gestion des événements lorsqu’un utilisateur clique sur un utilisateur
public interface UserListener {
    void onUserClicked(View v, User user);
}
