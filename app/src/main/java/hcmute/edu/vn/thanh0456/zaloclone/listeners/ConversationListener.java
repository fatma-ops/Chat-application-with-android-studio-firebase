package hcmute.edu.vn.thanh0456.zaloclone.listeners;

import hcmute.edu.vn.thanh0456.zaloclone.models.User;

// Gestion des événements lorsque l’utilisateur clique sur une conversation
public interface ConversationListener {
    void onConversationClicked(User user);
}
