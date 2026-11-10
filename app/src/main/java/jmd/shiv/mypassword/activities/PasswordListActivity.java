package jmd.shiv.mypassword.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
 * यह एक्टिविटी सभी सहेजे गए खातों की सूची, त्वरित खोज (Search) 
 * और विभिन्न श्रेणियों में छँटाई (Sorting) प्रदर्शित करती है।
 */
public class PasswordListActivity extends BaseSecurityActivity implements PasswordAdapter.OnPasswordItemClickListener {

    private ImageView btnBack;
    private EditText editSearch;
    private ImageView btnClearSearch;
    private ChipGroup chipGroupSort;
    private Chip chipRecent;
    private Chip chipAz;
    private Chip chipZa;
    private Chip chipFavorites;
    private RecyclerView recyclerPasswords;
    private LinearLayout layoutEmpty;
    private TextView textEmptyMsg;
    private FloatingActionButton fabAdd;

    private PasswordRepository repository;
    private PasswordAdapter adapter;
    private String currentSortType = "RECENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_list);
        applyEdgeToEdgeInsets(findViewById(R.id.layout_list_root));

        repository = new PasswordRepository(this);
        initializeViews();
        setupRecyclerView();
        setupSearchAndFilters();
        setupClickListeners();

        // यदि डैशबोर्ड से फ़िल्टर आया हो (उदा. Favorites)
        if (getIntent().hasExtra("FILTER_TYPE")) {
            String filter = getIntent().getStringExtra("FILTER_TYPE");
            if ("FAVORITES_FIRST".equalsIgnoreCase(filter)) {
                chipFavorites.setChecked(true);
                currentSortType = "FAVORITES_FIRST";
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPasswordRecords();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btn_list_back);
        editSearch = findViewById(R.id.edit_list_search);
        btnClearSearch = findViewById(R.id.btn_list_clear_search);
        chipGroupSort = findViewById(R.id.chip_group_sort);
        chipRecent = findViewById(R.id.chip_sort_recent);
        chipAz = findViewById(R.id.chip_sort_az);
        chipZa = findViewById(R.id.chip_sort_za);
        chipFavorites = findViewById(R.id.chip_sort_favorites);
        recyclerPasswords = findViewById(R.id.recycler_passwords_list);
        layoutEmpty = findViewById(R.id.layout_list_empty);
        textEmptyMsg = findViewById(R.id.text_list_empty_msg);
        fabAdd = findViewById(R.id.fab_list_add);
    }

    private void setupRecyclerView() {
        recyclerPasswords.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PasswordAdapter(this, this);
        recyclerPasswords.setAdapter(adapter);
    }

    private void setupSearchAndFilters() {
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                performSearch(query);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> editSearch.setText(""));

        chipGroupSort.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                chipRecent.setChecked(true);
                currentSortType = "RECENT";
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chip_sort_az) {
                    currentSortType = "A_Z";
                } else if (id == R.id.chip_sort_za) {
                    currentSortType = "Z_A";
                } else if (id == R.id.chip_sort_favorites) {
                    currentSortType = "FAVORITES_FIRST";
                } else {
                    currentSortType = "RECENT";
                }
            }
            loadPasswordRecords();
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddPasswordActivity.class);
            startActivity(intent);
        });
    }

    /**
     * चयनित छँटाई (Sort) के आधार पर डेटाबेस से सभी पासवर्ड रिकॉर्ड्स लोड करना।
     */
    private void loadPasswordRecords() {
        String currentQuery = editSearch.getText() != null ? editSearch.getText().toString().trim() : "";
        if (!currentQuery.isEmpty()) {
            performSearch(currentQuery);
            return;
        }

        List<PasswordRecord> list = repository.getAllPasswordRecords(currentSortType);
        displayRecordsList(list);
    }

    /**
     * सुरक्षित सर्च: वेबसाइट नाम, URL या रोल से खोज (पासवर्ड कभी सर्च नहीं होता)।
     */
    private void performSearch(String query) {
        List<PasswordRecord> list = repository.searchPasswordRecords(query);
        displayRecordsList(list);
    }

    private void displayRecordsList(List<PasswordRecord> list) {
        if (list.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerPasswords.setVisibility(View.GONE);
            String q = editSearch.getText().toString().trim();
            if (!q.isEmpty()) {
                textEmptyMsg.setText("'" + q + "' से संबंधित कोई रिकॉर्ड नहीं मिला।");
            } else {
                textEmptyMsg.setText(R.string.msg_no_records);
            }
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerPasswords.setVisibility(View.VISIBLE);
            adapter.setRecords(list);
        }
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
    }
}
