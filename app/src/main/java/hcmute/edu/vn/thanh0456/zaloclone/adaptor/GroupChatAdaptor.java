package hcmute.edu.vn.thanh0456.zaloclone.adaptor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import hcmute.edu.vn.thanh0456.zaloclone.models.GroupMessage;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ItemContainerGroupReceivedMessageBinding;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ItemContainerGroupSendMessageBinding;

// L’adaptateur gère les modifications liées à Group Chat RecyclerView (historique des messages de groupe)
//Mettre à jour et joindre des données au Viewholder, les afficher aux utilisateurs
public class GroupChatAdaptor extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    // Liste des messages de groupe
    private final ArrayList<GroupMessage> groupMessages;
    // Photo du destinataire du message
    private Bitmap receiverProfileImage;
    // ID de l’expéditeur du message
    private final String senderId;

    // Étant donné que Group Chat RecyclerView est affiché de 2 côtés, il y aura 2 types de conceptions viewHolder pour afficher les messages
    //    1 pour l’expéditeur, 1 pour le destinataire
    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;

    private Bitmap getBitmapFromEncodedImage(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }
    }
//    Le constructeur reçoit dans la liste des messages du groupe, l’ID de l’expéditeur du message
    public GroupChatAdaptor(ArrayList<GroupMessage> groupMessages, String senderId) {
        this.groupMessages = groupMessages;
        this.senderId = senderId;
    }

    // Créer un viewHolder
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // S’il s’agit de l’expéditeur, renvoyez la conception viewHolder de l’expéditeur
        //        S’il s’agit d’un destinataire, renvoyez la conception viewHolder du destinataire
        if (viewType == VIEW_TYPE_SENT) {
            return new SentMessageViewHolder(
                    ItemContainerGroupSendMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        } else {
            return new ReceivedMessageViewHolder(
                    ItemContainerGroupReceivedMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        }
    }
   // Attacher des données à viewHolder, qui appellera la fonction pour joindre des données en fonction du cas
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).setData(groupMessages.get(position));
        } else {
            ((ReceivedMessageViewHolder) holder).setData(groupMessages.get(position), receiverProfileImage);
        }
    }

    // Renvoie le nombre/la taille des messages de groupe.
    @Override
    public int getItemCount() {
        return groupMessages.size();
    }

    // Définir et renvoyer le type de VIEW_TYPE correspondant, utilisé pour déterminer le viewHolder utilisé
    @Override
    public int getItemViewType(int position) {
        if (groupMessages.get(position).senderId.equals(senderId)) {
            return VIEW_TYPE_SENT;
        } else {
            receiverProfileImage = groupMessages.get(position).imageBitmap;
            return VIEW_TYPE_RECEIVED;
        }
    }

    // Classe ViewHolder de l’expéditeur
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerGroupSendMessageBinding binding;
        // conception de liaison ViewHolder
        SentMessageViewHolder(ItemContainerGroupSendMessageBinding itemContainerGroupSendMessageBinding) {
            super(itemContainerGroupSendMessageBinding.getRoot());
            binding = itemContainerGroupSendMessageBinding;
        }

        // Joindre des données à viewHolder en fonction du cas du texte, de l’image ou du message audio
        void setData(GroupMessage groupMessage) {
            if (groupMessage.type.equals("text")) {
                binding.textMessage.setVisibility(View.VISIBLE);
                binding.audioMessage.setVisibility(View.GONE);
                binding.imageMessage.setVisibility(View.GONE);
                binding.textMessage.setText(groupMessage.message);
            } else if (groupMessage.type.equals("image")) {
                binding.textMessage.setVisibility(View.GONE);
                binding.audioMessage.setVisibility(View.GONE);
                binding.imageMessage.setVisibility(View.VISIBLE);
                Picasso.get().load(groupMessage.message).into(binding.imageMessage);
            } else if (groupMessage.type.equals("audio")) {
                binding.textMessage.setVisibility(View.GONE);
                binding.audioMessage.setVisibility(View.VISIBLE);
                binding.imageMessage.setVisibility(View.GONE);
                binding.audioMessage.setAudio(groupMessage.message);
            }
            binding.textDateTime.setText(groupMessage.dateTime);
        }
    }

    // Classe ViewHolder du destinataire
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerGroupReceivedMessageBinding binding;

        // conception de liaison ViewHolder
        ReceivedMessageViewHolder(ItemContainerGroupReceivedMessageBinding itemContainerGroupReceivedMessageBinding) {
            super(itemContainerGroupReceivedMessageBinding.getRoot());
            binding = itemContainerGroupReceivedMessageBinding;
        }

        // Joindre des données à viewHolder en fonction du cas du texte, de l’image ou du message audio
        void setData(GroupMessage groupMessage, Bitmap receiverProfileImage) {
            if (groupMessage.type.equals("text")) {
                binding.textMessage.setVisibility(View.VISIBLE);
                binding.audioMessage.setVisibility(View.GONE);
                binding.imageMessage.setVisibility(View.GONE);
                binding.textMessage.setText(groupMessage.message);
            } else if (groupMessage.type.equals("image")) {
                binding.textMessage.setVisibility(View.GONE);
                binding.audioMessage.setVisibility(View.GONE);
                binding.imageMessage.setVisibility(View.VISIBLE);
                Picasso.get().load(groupMessage.message).into(binding.imageMessage);
            } else if (groupMessage.type.equals("audio")) {
                binding.textMessage.setVisibility(View.GONE);
                binding.audioMessage.setVisibility(View.VISIBLE);
                binding.imageMessage.setVisibility(View.GONE);
                binding.audioMessage.setAudio(groupMessage.message);
            }
            binding.textDateTime.setText(groupMessage.dateTime);
            if (receiverProfileImage != null) {
                binding.imageProfile.setImageBitmap(receiverProfileImage);
            }
        }
    }

}
