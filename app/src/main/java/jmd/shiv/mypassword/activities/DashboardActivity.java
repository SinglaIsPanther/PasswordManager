package jmd.shiv.mypassword.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;
import javax.crypto.SecretKey;

import jmd.shiv.mypassword.R;
import jmd.shiv.mypassword.adapters.PasswordAdapter;
import jmd.shiv.mypassword.database.PasswordRepository;
import jmd.shiv.mypassword.managers.ClipboardManagerHelper;
import jmd.shiv.mypassword.models.PasswordRecord;
import jmd.shiv.mypassword.security.AuthenticationManager;
import jmd.shiv.mypassword.security.EncryptionManager;

/**
 * यह एक्टिविटी मुख्य डैशबोर्ड है जहाँ कुल खाते, हाल ही में जोड़े गए पासवर्ड, 
 * खोज, जनरेटर, सेटिंग्स और लॉगआउट की सुविधाएं उपलब्ध हैं।
 */
public class DashboardActivity extends BaseSecurityActivity implements PasswordAdapter.OnPasswordItemClickListener {

    private TextView textTotalCount;
    private TextView textFavCount;
    private MaterialCardView cardSearch;
    private MaterialCardView cardTotal;
    private MaterialCardView cardFavorites;
    private MaterialButton btnAllPasswords;
    private MaterialButton btnGenerator;
    private TextView textViewAll;
    private RecyclerView recyclerRecent;
    private LinearLayout layoutEmpty;
    private ExtendedFloatingActionButton fabAddPassword;
    private MaterialCardView cardWebAccess;
    private TextView textWebStatus;
    private ImageView btnSettings;
    private ImageView btnLogout;

    private PasswordRepository repository;
    private PasswordAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        applyEdgeToEdgeInsets(findViewById(R.id.layout_dashboard_root));

        repository = new PasswordRepository(this);
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void initializeViews() {
        textTotalCount = findViewById(R.id.text_dashboard_total_count);
        textFavCount = findViewById(R.id.text_dashboard_fav_count);
        cardSearch = findViewById(R.id.card_dashboard_search);
        cardTotal = findViewById(R.id.card_dashboard_total);
        cardFavorites = findViewById(R.id.card_dashboard_favorites);
        btnAllPasswords = findViewById(R.id.btn_dashboard_all_passwords);
        btnGenerator = findViewById(R.id.btn_dashboard_generator);
        textViewAll = findViewById(R.id.text_dashboard_view_all);
        recyclerRecent = findViewById(R.id.recycler_dashboard_recent);
        layoutEmpty = findViewById(R.id.layout_dashboard_empty);
        fabAddPassword = findViewById(R.id.fab_dashboard_add_password);
        cardWebAccess = findViewById(R.id.card_dashboard_web_access);
        textWebStatus = findViewById(R.id.text_dashboard_web_status);
        btnSettings = findViewById(R.id.btn_dashboard_settings);
        btnLogout = findViewById(R.id.btn_dashboard_logout);
    }

    private void setupRecyclerView() {
        recyclerRecent.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PasswordAdapter(this, this);
        recyclerRecent.setAdapter(adapter);
    }

    private void setupClickListeners() {
        fabAddPassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddPasswordActivity.class);
            startActivity(intent);
        });

        cardSearch.setOnClickListener(v -> openPasswordList(null));
        cardTotal.setOnClickListener(v -> openPasswordList(null));
        cardFavorites.setOnClickListener(v -> openPasswordList("FAVORITES_FIRST"));
        btnAllPasswords.setOnClickListener(v -> openPasswordList(null));
        textViewAll.setOnClickListener(v -> openPasswordList(null));

        cardWebAccess.setOnClickListener(v -> {
            Intent intent = new Intent(this, WebAccessActivity.class);
            startActivity(intent);
        });

        btnGenerator.setOnClickListener(v -> {
            Intent intent = new Intent(this, PasswordGeneratorActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> performUserLogout());
    }

    /**
     * डैशबोर्ड के आँकड़े और हाल ही के रिकॉर्ड्स को लोड करता है।
     */
    private void loadDashboardData() {
        int total = repository.getTotalPasswordCount();
        int fav = repository.getFavoriteCount();

        textTotalCount.setText(String.valueOf(total));
        textFavCount.setText(String.valueOf(fav));

        boolean isServerRunning = jmd.shiv.mypassword.server.WebServerController.getInstance().isServerRunning();
        if (isServerRunning) {
            String wifiIp = jmd.shiv.mypassword.utils.NetworkAddressManager.getLocalWifiIpAddress(this);
            int port = jmd.shiv.mypassword.server.WebServerController.getInstance().getServerPort();
            textWebStatus.setText("सर्वर सक्रिय: http://" + (wifiIp != null ? wifiIp : "localhost") + ":" + port);
            textWebStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.success_green));
        } else {
            textWebStatus.setText("Chrome से कनेक्ट करें (Wi-Fi / VPN)");
            textWebStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
        }

        List<PasswordRecord> recentList = repository.getRecentPasswordRecords(5);
        if (recentList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerRecent.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerRecent.setVisibility(View.VISIBLE);
            adapter.setRecords(recentList);
        }
    }

    private void openPasswordList(String filterType) {
        Intent intent = new Intent(this, PasswordListActivity.class);
        if (filterType != null) {
            intent.putExtra("FILTER_TYPE", filterType);
        }
        startActivity(intent);
    }

    /**
     * यूज़र को सुरक्षित रूप से लॉगआउट करता है।
     */
    private void performUserLogout() {
        AuthenticationManager.getInstance().logout();
        Toast.makeText(this, "सुरक्षित लॉगआउट सफल!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onItemClick(PasswordRecord record) {
        Intent intent = new Intent(this, PasswordDetailsActivity.class);
        intent.putExtra("RECORD_ID", record.getId());
        startActivity(intent);
    }

    @Override
    public void onCopyUsernameClick(PasswordRecord record) {
        if (record.getDecryptedUsername() != null && !record.getDecryptedUsername().isEmpty()) {
            ClipboardManagerHelper.copyToClipboard(this, "Username", record.getDecryptedUsername(), false);
            Toast.makeText(this, "यूजरनेम कॉपी किया गया!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCopyPasswordClick(PasswordRecord record) {
        SecretKey activeKey = AuthenticationManager.getInstance().getActiveMasterKey();
        if (activeKey != null) {
            try {
                String pass = EncryptionManager.decryptBytesToString(record.getPasswordEncrypted(), activeKey);
                if (pass != null) {
                    ClipboardManagerHelper.copyToClipboard(this, "Password", pass, true);
                    Toast.makeText(this, getString(R.string.msg_copied), Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "पासवर्ड डिक्रिप्ट करने में त्रुटि!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onFavoriteClick(PasswordRecord record, int position) {
        boolean newStatus = record.getIsFavorite() == 0;
        repository.toggleFavorite(record.getId(), newStatus);
        record.setIsFavorite(newStatus ? 1 : 0);
        adapter.notifyItemChanged(position);
        textFavCount.setText(String.valueOf(repository.getFavoriteCount()));
    }
}
