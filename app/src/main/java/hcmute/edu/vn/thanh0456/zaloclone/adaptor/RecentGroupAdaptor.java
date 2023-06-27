package hcmute.edu.vn.thanh0456.zaloclone.adaptor;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;

import hcmute.edu.vn.thanh0456.zaloclone.activities.GroupMessageActivity;
import hcmute.edu.vn.thanh0456.zaloclone.models.Group;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ItemContainerRecentGroupBinding;

// Les adaptateurs gèrent les modifications liées à RecentGroup RecyclerView (groupes dont les utilisateurs de périphériques sont membres)
//Mettre à jour et joindre des données au Viewholder, les afficher aux utilisateurs
public class RecentGroupAdaptor extends RecyclerView.Adapter<RecentGroupAdaptor.GroupViewHolder>{

    // Liste des groupes dont les membres contiennent des utilisateurs
    private static ArrayList<Group> groups = new ArrayList<>();
    // Le constructeur reçoit une liste de groupes dont l’utilisateur de périphérique est membre
    public RecentGroupAdaptor(ArrayList<Group> groups) {
        RecentGroupAdaptor.groups = groups;
    }

    // Créer un ViewHolder
    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GroupViewHolder(
                ItemContainerRecentGroupBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        // Accrochage des données à ViewHolder
        holder.setData(groups.get(position));
        // Accédez à la page de messagerie de groupe lorsque l’utilisateur clique
        // sur l’un des ViewHolder (groupes) correspondant(s)
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), GroupMessageActivity.class);
            intent.putExtra(Constants.KEY_COLLECTION_GROUP, groups.get(position));
            v.getContext().startActivity(intent);
        });
    }

    // Renvoie le nombre de groupes.
    @Override
    public int getItemCount() {
        return groups.size();
    }

    // Mettre à jour le dernier statut du groupe
    public static void updateLastMessage(Group updatedGroup) {
        for (Group group : groups) {
            if (group.id.equals(updatedGroup.id)) {
                groups.remove(group);
                groups.add(updatedGroup);
                break;
            }
        }
        Collections.sort(groups, (obj1, obj2) -> obj2.lastMessageModel.dateObject.compareTo(obj1.lastMessageModel.dateObject));
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        ItemContainerRecentGroupBinding binding;
        // conception de liaison Titulaire de vue
        GroupViewHolder(ItemContainerRecentGroupBinding itemContainerRecentGroupBinding) {
            super(itemContainerRecentGroupBinding.getRoot());
            binding = itemContainerRecentGroupBinding;
        }

        // Accrocher les données au Viewholder
        void setData(Group group) {
            binding.imageProfile.setImageBitmap(getConversationImage(group.image));
            binding.textName.setText(group.name);
            if (group.lastMessageModel != null) {
                binding.textRecentMessage.setText(group.lastMessageModel.message);
            } else {
                binding.textRecentMessage.setText(String.format("Last Message"));
            }
//            if (group.lastMessageModel.senderId.equals(senderId)) {
//                if (chatMessage.type.equals("text")) {
//                    binding.textRecentMessage.setText(String.format("You: %s", gr.message));
//                }
//                else if (chatMessage.type.equals("image")) {
//                    binding.textRecentMessage.setText(String.format("You %s", "just sent an image"));
//                } else if (chatMessage.type.equals("audio")){
//                    binding.textRecentMessage.setText(String.format("You %s", "just send an audio"));
//                }
////                database.collection(Constants.KEY_COLLECTION_USERS)
////                        .document(chatMessage.receiverId)
////                        .addSnapshotListener((Activity) mcontext, ((value, error) -> {
////                            if (error != null) {
////                                return;
////                            }
////                            if (value != null) {
////                                if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
////                                    int availability = Objects.requireNonNull(
////                                            value.getLong(Constants.KEY_AVAILABILITY)
////                                    ).intValue();
////                                    isReceiverAvailaible = availability == 1;
////                                }
////                            }
////                            if (isReceiverAvailaible) {
////                                binding.userAvailability.setVisibility(View.VISIBLE);
////                            } else {
////                                binding.userAvailability.setVisibility(View.GONE);
////                            }
////                        }));
//            }
//            else {
//                if (chatMessage.type.equals("text")) {
//                    binding.textRecentMessage.setText(chatMessage.message);
//                } else if (chatMessage.type.equals("image")) {
//                    binding.textRecentMessage.setText(String.format("%s %s", binding.textName.getText().toString(), "sent you an image"));
//                } else if (chatMessage.type.equals("audio")){
//                    binding.textRecentMessage.setText(String.format("%s %s", binding.textName.getText().toString(), "sent you an audio"));
//                }
////                database.collection(Constants.KEY_COLLECTION_USERS)
////                        .document(chatMessage.senderId)
////                        .addSnapshotListener((Activity) mcontext, ((value, error) -> {
////                            if (error != null) {
////                                return;
////                            }
////                            if (value != null) {
////                                if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
////                                    int availability = Objects.requireNonNull(
////                                            value.getLong(Constants.KEY_AVAILABILITY)
////                                    ).intValue();
////                                    isReceiverAvailaible = availability == 1;
////                                }
////                            }
////                            if (isReceiverAvailaible) {
////                                binding.userAvailability.setVisibility(View.VISIBLE);
////                            } else {
////                                binding.userAvailability.setVisibility(View.GONE);
////                            }
////                        }));
//            }
//            binding.getRoot().setOnClickListener(v -> {
//                User user = new User();
//                user.id = chatMessage.conversationId;
//                user.name = chatMessage.conversationName;
//                user.image = chatMessage.conversationImage;
//                conversationListener.onConversationClicked(user);
//            });
        }
    }

    // Convertir des images de chaîne en bitmap pour accrocher la conception du Viewholder
    private Bitmap getConversationImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

}
