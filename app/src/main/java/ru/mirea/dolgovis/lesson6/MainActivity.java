package ru.mirea.dolgovis.lesson6;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import ru.mirea.dolgovis.lesson6.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "mirea_settings";
    private static final String KEY_GROUP = "GROUP";
    private static final String KEY_NUMBER = "NUMBER";
    private static final String KEY_MOVIE = "MOVIE";

    private ActivityMainBinding binding;
    private SharedPreferences sharedPref;

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

        sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        binding.editTextGroup.setText(sharedPref.getString(KEY_GROUP, ""));
        int number = sharedPref.getInt(KEY_NUMBER, 0);
        binding.editTextNumber.setText(number == 0 ? "" : String.valueOf(number));
        binding.editTextMovie.setText(sharedPref.getString(KEY_MOVIE, ""));

        binding.buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData();
            }
        });
    }

    private void saveData() {
        String group = binding.editTextGroup.getText().toString().trim();
        String numberStr = binding.editTextNumber.getText().toString().trim();
        String movie = binding.editTextMovie.getText().toString().trim();

        int number = 0;
        if (!numberStr.isEmpty()) {
            try {
                number = Integer.parseInt(numberStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Некорректный номер по списку",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(KEY_GROUP, group);
        editor.putInt(KEY_NUMBER, number);
        editor.putString(KEY_MOVIE, movie);
        editor.apply();

        Toast.makeText(this, "Данные сохранены", Toast.LENGTH_SHORT).show();
    }
}