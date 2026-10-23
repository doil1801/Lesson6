package ru.mirea.dolgovis.internalfilestorage;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import ru.mirea.dolgovis.internalfilestorage.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String FILE_NAME = "mirea.txt";

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

        binding.editTextContent.setText(
                "9 мая - День Победы. Праздник в честь победы советского народа "
                        + "в Великой Отечественной войне 1941-1945 годов. "
                        + "Акт о капитуляции Германии подписан 8 мая 1945 года "
                        + "(по московскому времени уже 9 мая).");

        binding.buttonWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeToFile(binding.editTextContent.getText().toString());
            }
        });

        binding.buttonRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readInBackground();
            }
        });
    }

    private void writeToFile(String text) {
        try (FileOutputStream outputStream =
                     openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
            outputStream.write(text.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "Файл записан", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Ошибка записи", e);
            Toast.makeText(this, "Ошибка записи: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void readInBackground() {
        binding.textViewResult.setText("Чтение...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                SystemClock.sleep(2000);
                final String text = getTextFromFile();
                binding.textViewResult.post(new Runnable() {
                    @Override
                    public void run() {
                        binding.textViewResult.setText(
                                text != null ? text : "Файл не найден. Сначала запишите его.");
                    }
                });
            }
        }).start();
    }

    public String getTextFromFile() {
        try (FileInputStream fin = openFileInput(FILE_NAME)) {
            byte[] bytes = new byte[fin.available()];
            int read = fin.read(bytes);
            String text = new String(bytes, 0, Math.max(read, 0), StandardCharsets.UTF_8);
            Log.d(LOG_TAG, text);
            return text;
        } catch (IOException ex) {
            Log.e(LOG_TAG, "Ошибка чтения", ex);
            return null;
        }
    }
}