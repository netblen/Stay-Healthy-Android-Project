package com.example.stayhealthyap;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.PostViewHolder> {

    private List<PostModel> postList;
    private String currentUserId;
    private boolean isAdmin = false;
    private OnReplyClickListener replyListener;
    private OnDeleteClickListener deleteListener;
    private OnLikeClickListener likeListener;
    private OnKickClickListener kickListener; // NEW Listener

    public interface OnReplyClickListener {
        void onReplyClick(String username, String postId);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(String documentId, int position);
    }

    public interface OnLikeClickListener {
        void onLikeClick(String documentId, boolean currentlyLiked);
    }

    // NEW Interface for Kicking
    public interface OnKickClickListener {
        void onKickClick(String userId, String documentId, String username);
    }

    public CommunityAdapter(List<PostModel> postList,
                            OnReplyClickListener replyListener,
                            OnDeleteClickListener deleteListener,
                            OnLikeClickListener likeListener,
                            OnKickClickListener kickListener) { // Added kickListener
        this.postList = postList;
        this.replyListener = replyListener;
        this.deleteListener = deleteListener;
        this.likeListener = likeListener;
        this.kickListener = kickListener;

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    public void setAdminStatus(boolean isAdmin) {
        this.isAdmin = isAdmin;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        PostModel post = postList.get(position);
        holder.tvUsername.setText(post.getUsername());
        holder.tvContent.setText(post.getContent());

        int likeCount = post.getLikedBy().size();
        holder.tvLikes.setText(String.valueOf(likeCount));

        holder.imgHeart.setImageResource(R.drawable.ic_heart);

        boolean isLikedByMe = post.getLikedBy().contains(currentUserId);
        if (isLikedByMe) {
            holder.imgHeart.setColorFilter(Color.RED);
        } else {
            holder.imgHeart.setColorFilter(Color.GRAY);
        }

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
        if (post.isReply()) {
            int leftMargin = (int) (50 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            params.setMargins(leftMargin, params.topMargin, params.rightMargin, params.bottomMargin);
        } else {
            params.setMargins(0, params.topMargin, params.rightMargin, params.bottomMargin);
        }
        holder.itemView.setLayoutParams(params);

        // --- ADMIN / OWNER LOGIC ---

        boolean isMyPost = (post.getUserId() != null && post.getUserId().equals(currentUserId));

        // Delete Button: Show if it's my post OR I am Admin
        if (isMyPost || isAdmin) {
            holder.btnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }

        // Kick Button: Show ONLY if I am Admin AND it's NOT my post
        if (isAdmin && !isMyPost) {
            holder.btnKickUser.setVisibility(View.VISIBLE);
        } else {
            holder.btnKickUser.setVisibility(View.GONE);
        }

        // Click Listeners
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDeleteClick(post.getDocumentId(), position);
        });

        holder.btnKickUser.setOnClickListener(v -> {
            if (kickListener != null) kickListener.onKickClick(post.getUserId(), post.getDocumentId(), post.getUsername());
        });

        holder.btnLikeContainer.setOnClickListener(v -> {
            if (likeListener != null) likeListener.onLikeClick(post.getDocumentId(), isLikedByMe);
        });

        holder.btnReply.setOnClickListener(v -> {
            if (replyListener != null) replyListener.onReplyClick(post.getUsername(), post.getDocumentId());
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvContent, tvLikes, btnReply, btnKickUser; // Added btnKickUser
        ImageButton btnDelete;
        LinearLayout btnLikeContainer;
        ImageView imgHeart;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvPostUsername);
            tvContent = itemView.findViewById(R.id.tvPostContent);
            tvLikes = itemView.findViewById(R.id.tvPostLikes);
            btnDelete = itemView.findViewById(R.id.btnDeletePost);
            btnLikeContainer = itemView.findViewById(R.id.btnLike);
            imgHeart = itemView.findViewById(R.id.imgHeart);
            btnReply = itemView.findViewById(R.id.btnReply);
            btnKickUser = itemView.findViewById(R.id.btnKickUser); // Find ID
        }
    }
}