package jmd.shiv.mypassword.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import jmd.shiv.mypassword.R;
import jmd.shiv.mypassword.database.PasswordRepository;
import jmd.shiv.mypassword.managers.ClipboardManagerHelper;
import jmd.shiv.mypassword.models.AppSettings;
import jmd.shiv.mypassword.utils.PasswordGenerator;

/**
 * यह एक्टिविटी SecureRandom पर आधारित पासवर्ड जनरेटर मॉड्यूल को प्रबंधित करती है, 
 * जिससे यूज़र लंबाई और विभिन्न कैरेक्टर श्रेणियों के साथ जटिल पासवर्ड बना सकता है।
 * यह प्राथमिकताएं डेटाबेस में सहेजी जाती हैं ताकि मोबाइल और Chrome दोनों जगह वही नियम लागू रहें।
 */
public class PasswordGeneratorActivity extends BaseSecurityActivity {

    private ImageView btnBack;
    private TextView textResult;
    private MaterialButton btnRefresh;
    private MaterialButton btnCopy;
    private TextView textLengthLabel;
    private TextView textLengthValue;
    private SeekBar seekbarLength;
    private SwitchMaterial switchUpper;
    private SwitchMaterial switchLower;
    private SwitchMaterial switchNumbers;
    private SwitchMaterial switchSymbols;
    private MaterialButton btnClear;
    private MaterialButton btnUse;

    private PasswordRepository repository;

    // न्यूनतम 4 और अधिकतम 15 अक्षर (यूज़र आवश्यकता अनुसार)
    private static final int MIN_LENGTH = 4;
    private static final int MAX_LENGTH = 15;
    private int currentLength = 12;
    private String currentGeneratedPassword = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_generator);
        applyEdgeToEdgeInsets(findViewById(R.id.layout_generator_root));

        repository = new PasswordRepository(this);
        initializeViews();
        loadSavedSettings();
        setupSeekBar();
        setupClickListeners();
        generateNewPassword();
    }

    private void loadSavedSettings() {
        AppSettings settings = repository.getAppSettings();
        currentLength = settings.getGenLength();
        switchUpper.setChecked(settings.getGenIncludeUppercase() == 1);
        switchLower.setChecked(settings.getGenIncludeLowercase() == 1);
        switchNumbers.setChecked(settings.getGenIncludeNumbers() == 1);
        switchSymbols.setChecked(settings.getGenIncludeSymbols() == 1);
    }

    private void persistCurrentSettings(boolean upper, boolean lower, boolean num, boolean sym) {
        AppSettings settings = repository.getAppSettings();
        settings.setGenLength(currentLength);
        settings.setGenIncludeUppercase(upper ? 1 : 0);
        settings.setGenIncludeLowercase(lower ? 1 : 0);
        settings.setGenIncludeNumbers(num ? 1 : 0);
        settings.setGenIncludeSymbols(sym ? 1 : 0);
        repository.saveAppSettings(settings);
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btn_generator_back);
        textResult = findViewById(R.id.text_generator_result);
        btnRefresh = findViewById(R.id.btn_generator_refresh);
        btnCopy = findViewById(R.id.btn_generator_copy);
        textLengthLabel = findViewById(R.id.text_generator_length_label);
        textLengthValue = findViewById(R.id.text_generator_length_value);
        seekbarLength = findViewById(R.id.seekbar_generator_length);
        switchUpper = findViewById(R.id.switch_generator_upper);
        switchLower = findViewById(R.id.switch_generator_lower);
        switchNumbers = findViewById(R.id.switch_generator_numbers);
        switchSymbols = findViewById(R.id.switch_generator_symbols);
        btnClear = findViewById(R.id.btn_generator_clear);
        btnUse = findViewById(R.id.btn_generator_use);
    }

    private void setupSeekBar() {
        // SeekBar 0 से 11 = लंबाई 4 से 15
        seekbarLength.setMax(MAX_LENGTH - MIN_LENGTH);
        seekbarLength.setProgress(currentLength - MIN_LENGTH);
        updateLengthDisplay(currentLength);

        seekbarLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentLength = progress + MIN_LENGTH;
                updateLengthDisplay(currentLength);
                if (fromUser) {
                    generateNewPassword();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateLengthDisplay(int length) {
        textLengthLabel.setText("पासवर्ड लंबाई: " + length);
        textLengthValue.setText(String.valueOf(length));
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnRefresh.setOnClickListener(v -> generateNewPassword());

        switchUpper.setOnCheckedChangeListener((b, isChecked) -> generateNewPassword());
        switchLower.setOnCheckedChangeListener((b, isChecked) -> generateNewPassword());
        switchNumbers.setOnCheckedChangeListener((b, isChecked) -> generateNewPassword());
        switchSymbols.setOnCheckedChangeListener((b, isChecked) -> generateNewPassword());

        btnCopy.setOnClickListener(v -> {
            if (!currentGeneratedPassword.isEmpty()) {
                ClipboardManagerHelper.copyToClipboard(this, "Generated Password", currentGeneratedPassword, true);
                Toast.makeText(this, getString(R.string.msg_copied), Toast.LENGTH_LONG).show();
            }
        });

        btnClear.setOnClickListener(v -> {
            currentGeneratedPassword = "";
            textResult.setText("••••••••••••••••");
        });

        btnUse.setOnClickListener(v -> {
            if (!currentGeneratedPassword.isEmpty()) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("GENERATED_PASSWORD", currentGeneratedPassword);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "कृपया पहले पासवर्ड जनरेट करें!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * चयनित श्रेणियों के आधार पर नया पासवर्ड जनरेट करना।
     */
    private void generateNewPassword() {
        boolean upper = switchUpper.isChecked();
        boolean lower = switchLower.isChecked();
        boolean num = switchNumbers.isChecked();
        boolean sym = switchSymbols.isChecked();

        // यदि यूज़र ने सभी बंद कर दिए तो डिफ़ॉल्ट रूप से अंक चालू रखें
        if (!upper && !lower && !num && !sym) {
            switchNumbers.setChecked(true);
            num = true;
        }

        persistCurrentSettings(upper, lower, num, sym);

        currentGeneratedPassword = PasswordGenerator.generatePassword(currentLength, upper, lower, num, sym);
        textResult.setText(currentGeneratedPassword);
    }
}
