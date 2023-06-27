package hcmute.edu.vn.thanh0456.zaloclone.adaptor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Objects;

import hcmute.edu.vn.thanh0456.zaloclone.listeners.ConversationListener;
import hcmute.edu.vn.thanh0456.zaloclone.models.ChatMessage;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ItemContainerRecentConversationBinding;

// L’adaptateur gère les modifications liées à RecentConversation RecyclerView (conversations des utilisateurs avec d’autres utilisateurs)
//Mettre à jour et joindre des données au Viewholder, les afficher aux utilisateurs
public class RecentConversationAdaptor extends RecyclerView.Adapter<RecentConversationAdaptor.ConversationViewHolder>{

    // Données relatives au dernier message de la conversation entre 2 utilisateurs
    private final ArrayList<ChatMessage> chatMessages;
    // ID de l’expéditeur
    private final String senderId;
    // Gestion des événements lorsque l’utilisateur clique sur une conversation
    private final ConversationListener conversationListener;
    // Enregistrer le contexte
    private final Context mcontext;
    // Interaction avec les données sur Firestore
    private final FirebaseFirestore database;
    // Vérifier l’état marche/arrêt des autres utilisateurs
    private Boolean isReceiverAvailaible = false;

    // Le constructeur reçoit la dernière liste de données de message entre les utilisateurs, ID d’expéditeur de message
    //    un écouteur d’instance pour gérer l’événement lorsque l’utilisateur clique,
    public RecentConversationAdaptor(ArrayList<ChatMessage> chatMessages, String senderId, ConversationListener conversationListener, Context context) {
        this.chatMessages = chatMessages;
        this.senderId = senderId;
        this.conversationListener = conversationListener;
        this.mcontext = context;
        this.database = FirebaseFirestore.getInstance();
    }

    // Créer un viewHolder
    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversationViewHolder(
                ItemContainerRecentConversationBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    // Aligner des données sur viewHolder
    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
    }

    // Renvoie le nombre/la taille de la liste chatMessages
    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {
        ItemContainerRecentConversationBinding binding;

        // conception de liaison ViewHolder
        ConversationViewHolder(ItemContainerRecentConversationBinding itemContainerRecentConversationBinding) {
            super(itemContainerRecentConversationBinding.getRoot());
            binding = itemContainerRecentConversationBinding;
        }

        // Joindre des données à viewHolder en fonction du cas du texte, de l’image ou du message audio
        //        Mettre également à jour l’état actif de l’utilisateur recevant le message
        void setData(ChatMessage chatMessage) {
            binding.imageProfile.setImageBitmap(getConversationImage(chatMessage.conversationImage));
            binding.textName.setText(chatMessage.conversationName);
            if (chatMessage.senderId.equals(senderId)) {
                if (chatMessage.type.equals("text")) {
                    binding.textRecentMessage.setText(String.format("You: %s", chatMessage.message));
                } else if (chatMessage.type.equals("image")) {
                    binding.textRecentMessage.setText(String.format("You %s", "just sent an image"));
                } else if (chatMessage.type.equals("audio")){
                    binding.textRecentMessage.setText(String.format("You %s", "just send an audio"));
                }
                database.collection(Constants.KEY_COLLECTION_USERS)
                        .document(chatMessage.receiverId)
                        .addSnapshotListener((Activity) mcontext, ((value, error) -> {
                            if (error != null) {
                                return;
                            }
                            if (value != null) {
                                if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                                    int availability = Objects.requireNonNull(
                                            value.getLong(Constants.KEY_AVAILABILITY)
                                    ).intValue();
                                    isReceiverAvailaible = availability == 1;
                                }
                            }
                            if (isReceiverAvailaible) {
                                binding.userAvailability.setVisibility(View.VISIBLE);
                            } else {
                                binding.userAvailability.setVisibility(View.GONE);
                            }
                        }));
            }
            else {
                if (chatMessage.type.equals("text")) {
                    binding.textRecentMessage.setText(chatMessage.message);
                } else if (chatMessage.type.equals("image")) {
                    binding.textRecentMessage.setText(String.format("%s %s", binding.textName.getText().toString(), "sent you an image"));
                } else if (chatMessage.type.equals("audio")){
                    binding.textRecentMessage.setText(String.format("%s %s", binding.textName.getText().toString(), "sent you an audio"));
                }
                database.collection(Constants.KEY_COLLECTION_USERS)
                        .document(chatMessage.senderId)
                        .addSnapshotListener((Activity) mcontext, ((value, error) -> {
                            if (error != null) {
                                return;
                            }
                            if (value != null) {
                                if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                                    int availability = Objects.requireNonNull(
                                            value.getLong(Constants.KEY_AVAILABILITY)
                                    ).intValue();
                                    isReceiverAvailaible = availability == 1;
                                }
                            }
                            if (isReceiverAvailaible) {
                                binding.userAvailability.setVisibility(View.VISIBLE);
                            } else {
                                binding.userAvailability.setVisibility(View.GONE);
                            }
                        }));
            }
            binding.getRoot().setOnClickListener(v -> {
                User user = new User();
                user.id = chatMessage.conversationId;
                user.name = chatMessage.conversationName;
                user.image = chatMessage.conversationImage;
                //appelle une fonction de gestionnaire d’événements lorsque
                // l’utilisateur clique sur un Viewholder dans RecyclerView
                conversationListener.onConversationClicked(user);
            });
        }
    }

    // Convertir des images de chaîne en bitmap pour accrocher la conception du titulaire de la vue
    private Bitmap getConversationImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

}
