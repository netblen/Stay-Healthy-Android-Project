package com.example.stayhealthyap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.GroupViewHolder> {

    private List<GroupModel> groupList;
    private OnGroupClickListener listener;
    private Context context;

    public interface OnGroupClickListener {
        void onGroupClick(String groupName);
    }

    public GroupListAdapter(Context context, List<GroupModel> groupList, OnGroupClickListener listener) {
        this.context = context;
        this.groupList = groupList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_row, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        GroupModel group = groupList.get(position);

        // Fix for "Ghost Groups" (Empty blocks)
        if (group.getGroupName() == null || group.getGroupName().isEmpty()) {
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            return;
        } else {
            holder.itemView.setVisibility(View.VISIBLE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        holder.tvName.setText(group.getGroupName());

        // Fix Grammar: "1 Member" vs "2 Members"
        long count = group.getMemberCount();
        if (count == 1) {
            holder.tvMembers.setText("1 Member");
        } else {
            holder.tvMembers.setText(count + " Members");
        }

        // Icon Logic
        int iconResId = R.drawable.ic_run;
        if ("ic_swimming".equals(group.getIcon())) iconResId = R.drawable.ic_swimming;
        else if ("ic_circuit_workout".equals(group.getIcon())) iconResId = R.drawable.ic_circuit_workout;
        else if ("ic_walk".equals(group.getIcon())) iconResId = R.drawable.ic_walk;

        holder.imgIcon.setImageResource(iconResId);

        holder.itemView.setOnClickListener(v -> listener.onGroupClick(group.getGroupName()));
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvMembers;
        ImageView imgIcon;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvGroupNameRow);
            tvMembers = itemView.findViewById(R.id.tvMemberCountRow);
            imgIcon = itemView.findViewById(R.id.imgGroupIconRow);
        }
    }
}