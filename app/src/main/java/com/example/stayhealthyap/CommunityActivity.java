package com.example.stayhealthyap;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class CommunityActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private ImageButton btnCreateGroup;
    private EditText etSearch;
    private RecyclerView rvGroupsList;

    private FirebaseFirestore db;
    private GroupListAdapter adapter;
    private List<GroupModel> groupList;
    private List<GroupModel> fullGroupList; // For searching

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        db = FirebaseFirestore.getInstance();

        btnCreateGroup = findViewById(R.id.btnCreateGroup);
        etSearch = findViewById(R.id.etSearch);
        rvGroupsList = findViewById(R.id.rvGroupsList);

        // Setup List
        rvGroupsList.setLayoutManager(new LinearLayoutManager(this));
        groupList = new ArrayList<>();
        fullGroupList = new ArrayList<>();

        adapter = new GroupListAdapter(this, groupList, groupName -> {
            Intent intent = new Intent(CommunityActivity.this, GroupDetailActivity.class);
            intent.putExtra("GROUP_NAME", groupName);
            startActivity(intent);
        });
        rvGroupsList.setAdapter(adapter);

        // Fetch Groups
        fetchGroups();

        // Search Logic
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterGroups(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Open Create Screen
        btnCreateGroup.setOnClickListener(v -> {
            startActivity(new Intent(CommunityActivity.this, CreateGroupActivity.class));
        });

        setupBottomNavigation();
    }

    private void fetchGroups() {
        db.collection("groups").addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;

            groupList.clear();
            fullGroupList.clear();

            for (DocumentSnapshot doc : value.getDocuments()) {
                GroupModel group = doc.toObject(GroupModel.class);
                if (group != null) {
                    groupList.add(group);
                    fullGroupList.add(group);
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void filterGroups(String text) {
        groupList.clear();
        if (text.isEmpty()) {
            groupList.addAll(fullGroupList);
        } else {
            String query = text.toLowerCase();
            for (GroupModel group : fullGroupList) {
                if (group.getGroupName().toLowerCase().contains(query)) {
                    groupList.add(group);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_communite);

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (itemId == R.id.nav_communite) {
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            }
        });
    }
}