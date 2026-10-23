package ru.mirea.dolgovis.securesharedpreferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

import ru.mirea.dolgovis.securesharedpreferences.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String KEY_POET = "secure";
    private static final String DEFAULT_POET = "Александр Сергеевич Пушкин";

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left + 16, systemBars.top + 16,
                    systemBars.right + 16, systemBars.bottom + 16);
            return insets;
        });

        try {
            String mainKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            SharedPreferences secureSharedPreferences = EncryptedSharedPreferences.create(
                    "secret_shared_prefs",
                    mainKeyAlias,
                    getBaseContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);

            if (!secureSharedPreferences.contains(KEY_POET)) {
                secureSharedPreferences.edit().putString(KEY_POET, DEFAULT_POET).apply();
            }

            String result = secureSharedPreferences.getString(KEY_POET, "неизвестно");
            binding.textViewPoetName.setText(result);
            Log.d(TAG, "Poet from secure prefs: " + result);

        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Ошибка шифрованных настроек", e);
            Toast.makeText(this, "Ошибка шифрования: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}