package hcmute.edu.vn.thanh0456.zaloclone.adaptor;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import hcmute.edu.vn.thanh0456.zaloclone.models.ChatMessage;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ItemContainerReceivedMessageBinding;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ItemContainerSendMessageBinding;

//L’adaptateur gère les modifications liées à Chat RecyclerView (historique des messages entre 2 utilisateurs)
//Mettre à jour et joindre des données au Viewholder, les afficher aux utilisateurs
public class ChatAdaptor extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    // Liste des messages entre 2 utilisateurs
    private final ArrayList<ChatMessage> chatMessages;
    // Photo de la personne qui reçoit le message
    private Bitmap receiverProfileImage;
    // ID de l’expéditeur du message
    private final String senderId;

    // Étant donné que Chat RecyclerView est vu de 2 côtés, il y aura 2 types de conceptions viewHolder pour afficher les messages.
    //  1 pour l’expéditeur, 1 pour le destinataire
    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;

    public void setReceiverProfileImage(Bitmap bitmap) {
        receiverProfileImage = bitmap;
    }

    // Le constructeur reçoit la liste des messages entre 2 utilisateurs, l’image du destinataire, l’ID de l’expéditeur
    public ChatAdaptor(ArrayList<ChatMessage> chatMessages, Bitmap receiverProfileImage, String senderId) {
        this.chatMessages = chatMessages;
        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
    }

    // Tạo viewHolder
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // S’il s’agit de l’expéditeur, renvoyez la conception viewHolder de l’expéditeur
        //        S’il s’agit d’un destinataire, renvoyez la conception viewHolder du destinataire
        if (viewType == VIEW_TYPE_SENT) {
            return new SentMessageViewHolder(
                    ItemContainerSendMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        } else {
            return new ReceivedMessageViewHolder(
                    ItemContainerReceivedMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        }
    }
    //Attacher des données à viewHolder, qui appellera la fonction pour joindre des données en fonction du cas
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).setData(chatMessages.get(position));
        } else {
            ((ReceivedMessageViewHolder) holder).setData(chatMessages.get(position), receiverProfileImage);
        }
    }

    // Renvoie le nombre/la taille des messages entre 2 utilisateurs
    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    // Définir et renvoyer le type de VIEW_TYPE correspondant, utilisé pour déterminer le viewHolder utilisé
    @Override
    public int getItemViewType(int position) {
        if (chatMessages.get(position).senderId.equals(senderId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    // Class ViewHolder de l’expéditeur
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerSendMessageBinding binding;
        // conception de liaison ViewHolder
        SentMessageViewHolder(ItemContainerSendMessageBinding itemContainerSendMessageBinding) {
            super(itemContainerSendMessageBinding.getRoot());
            binding = itemContainerSendMessageBinding;
        }

        // Joindre des données à viewHolder en fonction du cas du texte, de l’image ou du message audio
        void setData(ChatMessage chatMessage) {
            if (chatMessage.type.equals("text")) {
                binding.textMessage.setVisibility(View.VISIBLE);
                binding.audioMessage.setVisibility(View.GONE);
                binding.imageMessage.setVisibility(View.GONE);
                binding.textMessage.setText(chatMessage.message);
            } else if (chatMessage.type.equals("image")) {
                binding.textMessage.setVisibility(View.GONE);
                binding.audioMessage.setVisibility(View.GONE);
                binding.imageMessage.setVisibility(View.VISIBLE);
                Picasso.get().load(chatMessage.message).into(binding.imageMessage);
            } else if (chatMessage.type.equals("audio")) {
                binding.textMessage.setVisibility(View.GONE);
                binding.audioMessage.setVisibility(View.VISIBLE);
                binding.imageMessage.setVisibility(View.GONE);
                binding.audioMessage.setAudio(chatMessage.message);
            }
            binding.textDateTime.setText(chatMessage.dateTime);
        }
    }

    // Class ViewHolder Nombre de bénéficiaires
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerReceivedMessageBinding binding;
        // conception de liaison ViewHolder
        ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding) {
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
        }

        // Joindre des données à viewHolder en fonction du cas du texte, de l’image ou du message audio
        void setData(ChatMessage chatMessage, Bitmap receiverProfileImage) {
            if (chatMessage.type.equals("text")) {
                binding.textMessage.setVisibility(View.VISIBLE);
                binding.audioMessage.setVisibility(View.GONE);
                binding.imageMessage.setVisibility(View.GONE);
                binding.textMessage.setText(chatMessage.message);
            } else if (chatMessage.type.equals("image")) {
                binding.textMessage.setVisibility(View.GONE);
                binding.audioMessage.setVisibility(View.GONE);
                binding.imageMessage.setVisibility(View.VISIBLE);
                Picasso.get().load(chatMessage.message).into(binding.imageMessage);
            } else if (chatMessage.type.equals("audio")) {
                binding.textMessage.setVisibility(View.GONE);
                binding.audioMessage.setVisibility(View.VISIBLE);
                binding.imageMessage.setVisibility(View.GONE);
                binding.audioMessage.setAudio(chatMessage.message);
            }
            binding.textDateTime.setText(chatMessage.dateTime);
            if (receiverProfileImage != null) {
                binding.imageProfile.setImageBitmap(receiverProfileImage);
            }
        }
    }
}
