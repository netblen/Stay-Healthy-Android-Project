package com.example.stayhealthyap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    public static class MemberItem {
        public String userId;
        public String username;
        public MemberItem(String userId, String username) {
            this.userId = userId;
            this.username = username;
        }
    }

    private List<MemberItem> memberList;
    private boolean isAdmin;
    private OnKickClickListener kickListener;

    public interface OnKickClickListener {
        void onKick(String userId, int position);
    }

    public MemberAdapter(List<MemberItem> memberList, boolean isAdmin, OnKickClickListener kickListener) {
        this.memberList = memberList;
        this.isAdmin = isAdmin;
        this.kickListener = kickListener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        MemberItem member = memberList.get(position);
        holder.tvName.setText(member.username);

        // Only show Kick button if I am Admin
        if (isAdmin) {
            holder.btnKick.setVisibility(View.VISIBLE);
        } else {
            holder.btnKick.setVisibility(View.GONE);
        }

        holder.btnKick.setOnClickListener(v -> {
            kickListener.onKick(member.userId, position);
        });
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        Button btnKick;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvMemberName);
            btnKick = itemView.findViewById(R.id.btnKick);
        }
    }
}