package jmd.shiv.mypassword.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;

import jmd.shiv.mypassword.R;
import jmd.shiv.mypassword.models.PasswordRecord;
import jmd.shiv.mypassword.security.AuthenticationManager;
import jmd.shiv.mypassword.security.EncryptionManager;

/**
 * यह एडॉप्टर RecyclerView में सभी सहेजे गए पासवर्ड खातों के कार्ड्स 
 * को कुशलतापूर्वक प्रदर्शित और प्रबंधित करता है।
 */
public class PasswordAdapter extends RecyclerView.Adapter<PasswordAdapter.PasswordViewHolder> {

    public interface OnPasswordItemClickListener {
        void onItemClick(PasswordRecord record);
        void onCopyUsernameClick(PasswordRecord record);
        void onCopyPasswordClick(PasswordRecord record);
        void onFavoriteClick(PasswordRecord record, int position);
    }

    private final Context context;
    private final List<PasswordRecord> recordsList = new ArrayList<>();
    private final OnPasswordItemClickListener listener;

    public PasswordAdapter(Context context, OnPasswordItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    /**
     * नई सूची के साथ एडॉप्टर का डेटा अपडेट करता है।
     */
    public void setRecords(List<PasswordRecord> newList) {
        this.recordsList.clear();
        if (newList != null) {
            this.recordsList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PasswordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_password_card, parent, false);
        return new PasswordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PasswordViewHolder holder, int position) {
        PasswordRecord record = recordsList.get(position);
        holder.bind(record, listener);
    }

    @Override
    public int getItemCount() {
        return recordsList.size();
    }

    public class PasswordViewHolder extends RecyclerView.ViewHolder {

        private final TextView textAvatar;
        private final TextView textWebsiteName;
        private final TextView textUsername;
        private final TextView textRole;
        private final ImageView btnFavorite;
        private final MaterialButton btnCopyUsername;
        private final MaterialButton btnCopyPassword;

        public PasswordViewHolder(@NonNull View itemView) {
            super(itemView);
            textAvatar = itemView.findViewById(R.id.text_item_avatar);
            textWebsiteName = itemView.findViewById(R.id.text_item_website_name);
            textUsername = itemView.findViewById(R.id.text_item_username);
            textRole = itemView.findViewById(R.id.text_item_role);
            btnFavorite = itemView.findViewById(R.id.btn_item_favorite);
            btnCopyUsername = itemView.findViewById(R.id.btn_item_copy_username);
            btnCopyPassword = itemView.findViewById(R.id.btn_item_copy_password);
        }

        public void bind(PasswordRecord record, OnPasswordItemClickListener clickListener) {
            String website = record.getWebsiteName() != null ? record.getWebsiteName() : "";
            textWebsiteName.setText(website);

            // अवतार पहला अक्षर
            if (!website.isEmpty()) {
                textAvatar.setText(website.substring(0, 1).toUpperCase());
            } else {
                textAvatar.setText("?");
            }

            // यूजरनेम डिक्रिप्शन (यदि पहले से डिक्रिप्ट नहीं है)
            if (record.getDecryptedUsername() == null) {
                SecretKey activeKey = AuthenticationManager.getInstance().getActiveMasterKey();
                if (activeKey != null) {
                    try {
                        String user = EncryptionManager.decryptBytesToString(record.getUsernameEncrypted(), activeKey);
                        record.setDecryptedUsername(user != null ? user : "");
                    } catch (Exception e) {
                        record.setDecryptedUsername("•••");
                    }
                }
            }
            textUsername.setText(record.getDecryptedUsername() != null ? record.getDecryptedUsername() : "");

            // रोल
            String role = record.getRole();
            if (role != null && !role.trim().isEmpty()) {
                textRole.setText(role);
                textRole.setVisibility(View.VISIBLE);
            } else {
                textRole.setVisibility(View.GONE);
            }

            // पसंदीदा आइकन
            if (record.getIsFavorite() == 1) {
                btnFavorite.setImageResource(R.drawable.ic_favorite);
            } else {
                btnFavorite.setImageResource(R.drawable.ic_favorite_border);
            }

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onItemClick(record);
                }
            });

            btnFavorite.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onFavoriteClick(record, getAdapterPosition());
                }
            });

            btnCopyUsername.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onCopyUsernameClick(record);
                }
            });

            btnCopyPassword.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onCopyPasswordClick(record);
                }
            });
        }
    }
}
