package hcmute.edu.vn.thanh0456.zaloclone.adaptor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import hcmute.edu.vn.thanh0456.zaloclone.MainActivity;
import hcmute.edu.vn.thanh0456.zaloclone.models.Story;
import hcmute.edu.vn.thanh0456.zaloclone.models.UserStory;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ItemContainerStoryBinding;
import omari.hamza.storyview.StoryView;
import omari.hamza.storyview.callback.StoryClickListeners;
import omari.hamza.storyview.model.MyStory;

// L’adaptateur gère les modifications liées à Story RecyclerView (liste des récits utilisateur)
//mettre à jour et joindre des données au viewHolder, afficher
public class TopStoryAdaptor extends RecyclerView.Adapter<TopStoryAdaptor.TopStoryViewHolder>{

    // Enregistrer le contexte d’entrée
    Context context;
    // Données sur les histoires de chaque utilisateur
    ArrayList<UserStory> userStories;

    // Le constructeur prend le contexte, la liste d’articles de chaque utilisateur est transmise dans
    public TopStoryAdaptor(Context context, ArrayList<UserStory> userStories) {
        this.context = context;
        this.userStories = userStories;
    }

    // Créer un ViewHolder
    @NonNull
    @Override
    public TopStoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TopStoryViewHolder(
                ItemContainerStoryBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull TopStoryViewHolder holder, int position) {
        // Accrochage des données à ViewHolder
        UserStory userStory = userStories.get(position);
        holder.setData(userStory);
        if (userStory.stories != null) {
            holder.binding.circularStatusView.setPortionsCount(userStory.stories.size());
        }
        // Accédez à la page de visualisation de l’histoire de l’utilisateur pour cliquer sur
        holder.binding.circularStatusView.setOnClickListener(v -> {
            ArrayList<MyStory> myStories = new ArrayList<>();
            for (Story story : userStory.stories) {
                myStories.add(new MyStory(story.imageURL));
            }
            new StoryView.Builder(((MainActivity)context).getSupportFragmentManager())
                    .setStoriesList(myStories)
                    .setStoryDuration(3000)
                    .setTitleText(userStory.name)
                    .setSubtitleText("")
                    .setTitleLogoUrl(userStory.image)
                    .setStoryClickListeners(new StoryClickListeners() {
                        @Override
                        public void onDescriptionClickListener(int position) {
                        }

                        @Override
                        public void onTitleIconClickListener(int position) {
                        }
                    })
                    .build()
                    .show();
        });
    }

    // Renvoie le nombre d’utilisateurs qui ont publié des articles
    @Override
    public int getItemCount() {
        return userStories.size();
    }

    public class TopStoryViewHolder extends RecyclerView.ViewHolder {
        ItemContainerStoryBinding binding;
        // conception de liaison ViewHolder
        public TopStoryViewHolder(ItemContainerStoryBinding itemContainerStoryBinding) {
            super(itemContainerStoryBinding.getRoot());
            binding = itemContainerStoryBinding;
        }
        // Accrochage des données à ViewHolder
        void setData(UserStory userStory) {
            binding.imageProfile.setImageBitmap(getConversationImage(userStory.image));
        }
    }

    // Convertir des images de chaîne en bitmap pour ancrer la conception ViewHolder
    private Bitmap getConversationImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
