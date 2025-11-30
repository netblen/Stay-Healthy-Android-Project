package com.example.stayhealthyap;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupDetailActivity extends AppCompatActivity {

    private Button btnJoinGroup;
    private RecyclerView rvGroupPosts;
    private TextView tvHiddenText, tvGroupName, tvMemberCount;
    private ImageButton btnBack, btnSendPost;
    private LinearLayout layoutInputArea, layoutGroupHeader;
    private EditText etPostInput;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentGroupName;
    private String currentUserId;
    private boolean isReplying = false;
    private String replyToPostId = null;
    private boolean isAdmin = false;

    private CommunityAdapter adapter;
    private List<PostModel> postList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        } else {
            finish(); return;
        }

        // Setup UI
        btnJoinGroup = findViewById(R.id.btnJoinGroup);
        rvGroupPosts = findViewById(R.id.rvGroupPosts);
        tvHiddenText = findViewById(R.id.tvHiddenText);
        tvGroupName = findViewById(R.id.tvGroupName);
        btnBack = findViewById(R.id.btnBack);
        layoutInputArea = findViewById(R.id.layoutInputArea);
        etPostInput = findViewById(R.id.etPostInput);
        btnSendPost = findViewById(R.id.btnSendPost);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        layoutGroupHeader = findViewById(R.id.layoutGroupHeader);

        currentGroupName = getIntent().getStringExtra("GROUP_NAME");
        if (currentGroupName != null) tvGroupName.setText(currentGroupName);

        // Initialize Group in Database (Check Admin if exists)
        initializeGroupInFirebase();

        // Setup List
        rvGroupPosts.setLayoutManager(new LinearLayoutManager(this));
        postList = new ArrayList<>();

        // --- ADAPTER SETUP ---
        adapter = new CommunityAdapter(postList,
                // 1. Reply Listener
                (username, postId) -> {
                    isReplying = true;
                    replyToPostId = postId;
                    etPostInput.setHint("Replying to " + username + "...");
                    etPostInput.requestFocus();
                },
                // 2. Delete Listener (Trash Can)
                (documentId, position) -> {
                    deletePostFromFirebase(documentId);
                },
                // 3. Like Listener (Heart)
                (documentId, currentlyLiked) -> {
                    toggleLikeOnFirebase(documentId, currentlyLiked);
                },
                // 4. Kick Listener (Admin Only)
                (userId, documentId, username) -> {
                    confirmKickUser(userId, documentId, username);
                }
        );
        adapter.setAdminStatus(isAdmin);
        rvGroupPosts.setAdapter(adapter);

        checkUserGroupStatus();
        listenToMemberCount();

        btnJoinGroup.setOnClickListener(v -> handleJoinClick());

        btnSendPost.setOnClickListener(v -> {
            String content = etPostInput.getText().toString().trim();
            if (!content.isEmpty()) {
                savePostToFirebase(content, isReplying, replyToPostId);
                isReplying = false;
                replyToPostId = null;
                etPostInput.setHint("Type something...");
            }
        });

        // Show members when header is clicked
        if (layoutGroupHeader != null) {
            layoutGroupHeader.setOnClickListener(v -> showMembersDialog());
        }

        btnBack.setOnClickListener(v -> finish());
    }

    // --- KICK CONFIRMATION ---
    private void confirmKickUser(String userId, String postId, String username) {
        new AlertDialog.Builder(this)
                .setTitle("Kick " + username + "?")
                .setMessage("This will delete their comment and remove them from the group.")
                .setPositiveButton("Kick & Delete", (dialog, which) -> {
                    deletePostFromFirebase(postId);
                    kickUser(userId);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- GROUP & ADMIN INITIALIZATION ---
    private void initializeGroupInFirebase() {
        DocumentReference groupRef = db.collection("groups").document(currentGroupName);
        groupRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String adminId = doc.getString("adminId");
                if (adminId != null && adminId.equals(currentUserId)) {
                    isAdmin = true;
                    adapter.setAdminStatus(true);

                    // Update button immediately if currently showing "Leave"
                    if (btnJoinGroup.getText().toString().equals("Leave Group")) {
                        btnJoinGroup.setText("Delete Group");
                    }
                }
            }
        });
    }

    private void listenToMemberCount() {
        db.collection("groups").document(currentGroupName)
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;
                    Long count = doc.getLong("memberCount");
                    if (count == null) count = 0L;
                    if (count < 0) count = 0L;

                    String memberText = (count == 1) ? "1 Member" : count + " Members";

                    if (tvMemberCount != null) {
                        tvMemberCount.setText(memberText + " • Public Group");
                    }
                });
    }

    // --- MEMBER LIST & KICKING ---
    private void showMembersDialog() {
        db.collection("groups").document(currentGroupName).collection("members")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<MemberAdapter.MemberItem> members = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String uid = doc.getId();
                        String name = doc.getString("username");
                        if (name == null) name = "User";
                        members.add(new MemberAdapter.MemberItem(uid, name));
                    }
                    showMembersPopup(members);
                });
    }

    private void showMembersPopup(List<MemberAdapter.MemberItem> members) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        RecyclerView rvMembers = new RecyclerView(this);
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        MemberAdapter memberAdapter = new MemberAdapter(members, isAdmin, (userId, pos) -> kickUser(userId));
        rvMembers.setAdapter(memberAdapter);
        builder.setTitle("Members (" + members.size() + ")");
        builder.setView(rvMembers);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void kickUser(String userIdToKick) {
        if (userIdToKick.equals(currentUserId)) {
            Toast.makeText(this, "Use 'Delete Group' to leave.", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("groups").document(currentGroupName).collection("members").document(userIdToKick).delete();
        db.collection("groups").document(currentGroupName).update("memberCount", FieldValue.increment(-1));
        db.collection("users").document(userIdToKick).update("currentGroup", null);
        Toast.makeText(this, "User kicked.", Toast.LENGTH_SHORT).show();
    }

    // --- POST & UI LOGIC ---

    private void toggleLikeOnFirebase(String documentId, boolean currentlyLiked) {
        if (documentId == null) return;
        DocumentReference postRef = db.collection("groups").document(currentGroupName).collection("posts").document(documentId);
        if (currentlyLiked) postRef.update("likedBy", FieldValue.arrayRemove(currentUserId));
        else postRef.update("likedBy", FieldValue.arrayUnion(currentUserId));
    }

    private void savePostToFirebase(String content, boolean isReply, String parentId) {
        String username = "User";
        if (auth.getCurrentUser().getEmail() != null) username = auth.getCurrentUser().getEmail().split("@")[0];
        PostModel newPost = new PostModel(username, currentUserId, content, 0, isReply, parentId);
        db.collection("groups").document(currentGroupName).collection("posts").add(newPost)
                .addOnSuccessListener(documentReference -> etPostInput.setText(""));
    }

    private void deletePostFromFirebase(String documentId) {
        if (documentId == null) return;
        db.collection("groups").document(currentGroupName).collection("posts").document(documentId).delete();
    }

    private void listenForPosts() {
        db.collection("groups").document(currentGroupName).collection("posts")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    List<PostModel> tempList = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        PostModel post = doc.toObject(PostModel.class);
                        post.setDocumentId(doc.getId());
                        tempList.add(post);
                    }
                    reorderPosts(tempList);
                });
    }

    private void reorderPosts(List<PostModel> rawList) {
        postList.clear();
        List<PostModel> parents = new ArrayList<>();
        Map<String, List<PostModel>> replyMap = new HashMap<>();
        for (PostModel post : rawList) {
            if (post.isReply() && post.getReplyToId() != null) {
                String parentId = post.getReplyToId();
                if (!replyMap.containsKey(parentId)) replyMap.put(parentId, new ArrayList<>());
                replyMap.get(parentId).add(post);
            } else {
                parents.add(post);
            }
        }
        for (PostModel parent : parents) {
            postList.add(parent);
            if (replyMap.containsKey(parent.getDocumentId())) postList.addAll(replyMap.get(parent.getDocumentId()));
        }
        adapter.notifyDataSetChanged();
        if (postList.size() > 0) rvGroupPosts.smoothScrollToPosition(postList.size() - 1);
    }

    private void checkUserGroupStatus() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("currentGroup")) {
                        String joinedGroup = documentSnapshot.getString("currentGroup");
                        if (joinedGroup != null && joinedGroup.equals(currentGroupName)) {
                            showJoinedState();
                        } else if (joinedGroup != null && !joinedGroup.isEmpty()) {
                            showLockedState(joinedGroup);
                        } else {
                            showUnjoinedState();
                        }
                    } else {
                        showUnjoinedState();
                    }
                });
    }

    private void handleJoinClick() {
        String btnText = btnJoinGroup.getText().toString();

        if (btnText.equals("Delete Group")) {
            // ADMIN DELETE
            confirmDeleteGroup();
        }
        else if (btnText.equals("Leave Group")) {
            // NORMAL LEAVE
            leaveGroup();
        }
        else if (btnText.equals("Join Group")) {
            // JOIN
            joinGroup();
        }
    }

    private void confirmDeleteGroup() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Group")
                .setMessage("Deleting this group will remove everyone and delete it permanently. Are you sure?")
                .setPositiveButton("Delete", (dialog, which) -> deleteGroup())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteGroup() {
        // Kick everyone first
        db.collection("groups").document(currentGroupName).collection("members")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String memberId = doc.getId();
                        db.collection("users").document(memberId).update("currentGroup", null);
                    }
                    // Then Delete Document
                    db.collection("groups").document(currentGroupName).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Group deleted.", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                });
    }

    private void leaveGroup() {
        Map<String, Object> update = new HashMap<>();
        update.put("currentGroup", null);
        db.collection("users").document(currentUserId).update(update);

        db.collection("groups").document(currentGroupName).collection("members").document(currentUserId).delete();
        db.collection("groups").document(currentGroupName).update("memberCount", FieldValue.increment(-1));

        showUnjoinedState();
    }

    private void joinGroup() {
        Map<String, Object> update = new HashMap<>();
        update.put("currentGroup", currentGroupName);
        db.collection("users").document(currentUserId).set(update, SetOptions.merge());

        Map<String, Object> memberData = new HashMap<>();
        String name = "User";
        if(auth.getCurrentUser().getEmail() != null) name = auth.getCurrentUser().getEmail().split("@")[0];
        memberData.put("username", name);

        db.collection("groups").document(currentGroupName).collection("members").document(currentUserId).set(memberData);
        db.collection("groups").document(currentGroupName).update("memberCount", FieldValue.increment(1));
        showJoinedState();
    }

    private void showJoinedState() {
        if (isAdmin) {
            btnJoinGroup.setText("Delete Group");
        } else {
            btnJoinGroup.setText("Leave Group");
        }
        btnJoinGroup.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
        btnJoinGroup.setEnabled(true);
        rvGroupPosts.setVisibility(View.VISIBLE);
        layoutInputArea.setVisibility(View.VISIBLE);
        tvHiddenText.setVisibility(View.GONE);
        listenForPosts();
    }

    private void showUnjoinedState() {
        btnJoinGroup.setText("Join Group");
        btnJoinGroup.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
        btnJoinGroup.setEnabled(true);
        rvGroupPosts.setVisibility(View.GONE);
        layoutInputArea.setVisibility(View.GONE);
        tvHiddenText.setVisibility(View.VISIBLE);
        tvHiddenText.setText("Join this group to see the posts!");
    }

    private void showLockedState(String otherGroupName) {
        btnJoinGroup.setText("Already in " + otherGroupName);
        btnJoinGroup.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        btnJoinGroup.setEnabled(false);
        rvGroupPosts.setVisibility(View.GONE);
        layoutInputArea.setVisibility(View.GONE);
        tvHiddenText.setVisibility(View.VISIBLE);
        tvHiddenText.setText("You can only join one group at a time.");
    }
}