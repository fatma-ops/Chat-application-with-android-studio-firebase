package hcmute.edu.vn.thanh0456.zaloclone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import hcmute.edu.vn.thanh0456.zaloclone.activities.GroupActivity;
import hcmute.edu.vn.thanh0456.zaloclone.adaptor.RecentGroupAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.models.Group;
import hcmute.edu.vn.thanh0456.zaloclone.models.GroupLastMessageModel;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.FragmentGroupBinding;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link GroupFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GroupFragment extends Fragment{

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private FragmentGroupBinding binding;
    private PreferenceManager preferenceManager;
    private RecentGroupAdaptor recentGroupAdaptor;
    private ArrayList<Group> groups;
    private FirebaseFirestore database;
    private CollectionReference groupRef;
    ArrayList<Task> tasks = new ArrayList<>();

    public GroupFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CloudFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GroupFragment newInstance(String param1, String param2) {
        GroupFragment fragment = new GroupFragment();
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
        if (groups != null) {
            groups.clear();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentGroupBinding.inflate(inflater, container, false);
        init();
        setListeners();
        listenConversations();
        return binding.getRoot();
    }

    // Fonction d’initialisation
    private void init() {
        // Enregistrer ou récupérer des données à partir de SharePrefs
        preferenceManager = new PreferenceManager(getActivity().getApplicationContext());
        // Liste des groupes dont l’utilisateur est membre
        groups = new ArrayList<>();
        // Configuration de l’adaptateur
        recentGroupAdaptor = new RecentGroupAdaptor(groups);
        // Connectez l’adaptateur au RecyclerView
        binding.conversationsRecyclerView.setAdapter(recentGroupAdaptor);
        // Interagir avec les données stockées sur Firestore
        database = FirebaseFirestore.getInstance();
        // Mapper le groupe de collecte sur Firestore
        groupRef = database.collection(Constants.KEY_COLLECTION_GROUP);
    }

    // Fonction de configuration d’événements
    private void setListeners() {
        //Accéder à la page Créer un nouveau groupe
        binding.fabNewGroup.setOnClickListener(v ->
                startActivity(new Intent(getActivity().getApplicationContext(), GroupActivity.class)));
    }
   // La fonction suit l’état du groupe, met à jour les derniers messages en temps réel
    private void listenConversations() {
        groupRef.addSnapshotListener(eventListener);
    }

    // Le gestionnaire d’événements met à jour le dernier message en temps réel
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        //Obtenez des données de tous les groupes sur Firestore et poussez RecyclerView

        if (error != null) {
            return;
        }
        if (value != null) {
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    String groupId = documentChange.getDocument().getId();
                    groupRef.document(groupId)
                            .collection(Constants.KEY_COLLECTIONS_MEMBERS)
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful() && task.getResult() != null
                                        && task.getResult().getDocuments().size() > 0) {
                                    Group group = new Group();
                                    group.members = new ArrayList<>();
                                    for (DocumentSnapshot documentSnapshot : task.getResult().getDocuments()) {
                                        String memberId = documentSnapshot.getId();
                                        User user = new User();
                                        user.id = memberId;
                                        user.role = documentSnapshot.getString(Constants.KEY_ROLE);

                                        DocumentReference documentReference1 = database.collection(Constants.KEY_COLLECTION_USERS)
                                                .document(memberId);
                                        documentReference1.addSnapshotListener((value12, error12) -> {
                                            if (error12 != null) {
                                                return;
                                            }
                                            if (value12 != null && value12.exists()) {
                                                user.name = value12.getString(Constants.KEY_NAME);
                                                user.image = value12.getString(Constants.KEY_IMAGE);
                                                if (user.role.equals("owner")) {
                                                    group.ownerId = user.id;
                                                    group.ownerName = user.name;
                                                }
                                            }
                                        });
                                        group.members.add(user);
                                        if (memberId.equals(preferenceManager.getString(Constants.KEY_USER_ID))) {
                                            DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_GROUP)
                                                    .document(groupId);
                                            documentReference.addSnapshotListener((value1, error1) -> {
                                                if (error1 != null) {
                                                    return;
                                                }
                                                if (value1 != null && value1.exists()) {
                                                    group.image = value1.getString(Constants.KEY_GROUP_IMAGE);
                                                    group.name = value1.getString(Constants.KEY_GROUP_NAME);
                                                    group.id = value1.getId();
                                                    group.dateObject = value1.getDate(Constants.KEY_TIMESTAMP);

//                                                    Task task1 = value1.getReference().collection(Constants.KEY_COLLECTION_LAST_MESSAGE)
//                                                            .document(groupId)
//                                                            .get();

//                                                    tasks.add(task1);
//                                                    groups.add(group);

                                                    value1.getReference().collection(Constants.KEY_COLLECTION_LAST_MESSAGE)
                                                            .document(groupId)
                                                            .get()
                                                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                                @Override
                                                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                                    if (documentSnapshot.exists()) {
                                                                        group.lastMessageModel = documentSnapshot.toObject(GroupLastMessageModel.class);
                                                                        group.lastMessageModel.dateObject = documentSnapshot.getDate(Constants.KEY_TIMESTAMP);
                                                                        group.lastMessageModel.dateTime = getReadableDateTime(group.lastMessageModel.dateObject);
                                                                    }
                                                                    else {
                                                                        HashMap<String, Object> message = new HashMap<>();
                                                                        message.put(Constants.KEY_TIMESTAMP, new Date());
                                                                        group.lastMessageModel = new GroupLastMessageModel();
                                                                        group.lastMessageModel.dateObject = (Date) message.get(Constants.KEY_TIMESTAMP);
                                                                        database.collection(Constants.KEY_COLLECTION_GROUP)
                                                                                .document(group.id)
                                                                                .collection(Constants.KEY_COLLECTION_LAST_MESSAGE)
                                                                                .document(group.id)
                                                                                .set(message);
                                                                    }
                                                                    groups.add(group);
                                                                    Collections.sort(groups, (obj1, obj2) -> obj2.lastMessageModel.dateObject.compareTo(obj1.lastMessageModel.dateObject));
                                                                    recentGroupAdaptor.notifyDataSetChanged();
                                                                }
                                                            });
                                                }
                                                binding.conversationsRecyclerView.smoothScrollToPosition(0);
                                                binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
                                                binding.progressBar.setVisibility(View.GONE);
                                            });
                                        }
                                    }
                                }
                            });
                } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    for (Group group : groups) {
                        if (group.id.equals(documentChange.getDocument().getId())) {
                            groups.remove(group);
                            break;
                        }
                    }
                }
            }
            Collections.sort(groups, (obj1, obj2) -> obj2.lastMessageModel.dateObject.compareTo(obj1.lastMessageModel.dateObject));
            recentGroupAdaptor.notifyDataSetChanged();
//            Task combineTask = Tasks.whenAllSuccess(tasks).addOnSuccessListener(new OnSuccessListener<List<Object>>() {
//                @Override
//                public void onSuccess(List<Object> objects) {
//                    for (Object object : objects) {
//                        for (DocumentSnapshot documentSnapshot : (QuerySnapshot) object) {
//                            if (documentSnapshot.exists()) {
//                                for (Group group : groups) {
//                                    if (group.id.equals(documentSnapshot.getId())) {
//                                        group.lastMessageModel = documentSnapshot.toObject(GroupLastMessageModel.class);
//                                        group.lastMessageModel.dateObject = documentSnapshot.getDate(Constants.KEY_TIMESTAMP);
//                                        group.lastMessageModel.dateTime = getReadableDateTime(group.lastMessageModel.dateObject);
//                                    }
//                                }
//                            }
//                            else {
//                                for (Group group : groups) {
//                                    if (group.lastMessageModel == null) {
//                                        HashMap<String, Object> message = new HashMap<>();
//                                        message.put(Constants.KEY_TIMESTAMP, new Date());
//                                        group.lastMessageModel = new GroupLastMessageModel();
//                                        group.lastMessageModel.dateObject = (Date) message.get(Constants.KEY_TIMESTAMP);
//                                        database.collection(Constants.KEY_COLLECTION_GROUP)
//                                                .document(group.id)
//                                                .collection(Constants.KEY_COLLECTION_LAST_MESSAGE)
//                                                .document(group.id)
//                                                .set(message);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                    Collections.sort(groups, (obj1, obj2) -> obj2.lastMessageModel.dateObject.compareTo(obj1.lastMessageModel.dateObject));
//                    recentGroupAdaptor.notifyDataSetChanged();
//                    binding.conversationsRecyclerView.smoothScrollToPosition(0);
//                    binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
//                    binding.progressBar.setVisibility(View.GONE);
//                }
//            });
        }
    };

    // Fonction de conversion de date en chaîne
    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM, dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    // Si l’utilisateur revient à la page, mettez à jour le RecyclerView
    @Override
    public void onResume() {
        super.onResume();
        recentGroupAdaptor.notifyDataSetChanged();
    }

    //

    //La fonction reçoit des données si le groupe a un changement tel que le nom, la photo, le nombre de membresMettre à jour l’état du groupe vers la dernière version
    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Group changedGroup = intent.getParcelableExtra(Constants.KEY_COLLECTION_GROUP);
            if (changedGroup != null) {
                int i = 0;
                while (groups.size() > 0 && i < groups.size()) {
                    if (groups.get(i).id.equals(changedGroup.id)) {
                        groups.remove(i);
                    } else {
                        i += 1;
                    }
                }
                groups.add(0, changedGroup);
                recentGroupAdaptor.notifyDataSetChanged();
                Toast.makeText(context, "Changed Group", Toast.LENGTH_SHORT).show();
            }
        }
    };

    // Au début de l’activité, l’enregistrement de la fonction recevra les données transmises par une autre activité
    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                invitationResponseReceiver,
                new IntentFilter(Constants.KEY_COLLECTION_GROUP)
        );
    }

    // Se désabonner de la fonction de réception lorsque l’activité est arrêtée
    @Override
    public void onDestroy() {
        // Unregister when activity finished
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );
        super.onDestroy();
    }
}