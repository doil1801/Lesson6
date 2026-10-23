package ru.mirea.dolgovis.notebook;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import ru.mirea.dolgovis.notebook.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Notebook";
    private static final int REQUEST_CODE_PERMISSION = 100;

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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !hasLegacyPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_PERMISSION);
        }

        binding.buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveClicked();
            }
        });

        binding.buttonLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadClicked();
            }
        });
    }

    private boolean hasLegacyPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION
                && (grantResults.length == 0
                || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, "Без разрешения сохранение недоступно",
                    Toast.LENGTH_LONG).show();
        }
    }

    private String fileName() {
        String name = binding.editTextFileName.getText().toString().trim();
        if (name.isEmpty()) return null;
        return name.endsWith(".txt") ? name : name + ".txt";
    }

    private void saveClicked() {
        String name = fileName();
        if (name == null) {
            Toast.makeText(this, "Введите название файла", Toast.LENGTH_SHORT).show();
            return;
        }
        String quote = binding.editTextQuote.getText().toString();
        boolean ok = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? writeMediaStore(name, quote)
                : writeLegacy(name, quote);
        Toast.makeText(this, ok ? "Файл сохранён: " + name : "Ошибка сохранения",
                Toast.LENGTH_SHORT).show();
    }

    private void loadClicked() {
        String name = fileName();
        if (name == null) {
            Toast.makeText(this, "Введите название файла", Toast.LENGTH_SHORT).show();
            return;
        }
        String text = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? readMediaStore(name)
                : readLegacy(name);
        if (text == null) {
            Toast.makeText(this, "Файл не найден", Toast.LENGTH_SHORT).show();
        } else {
            binding.editTextQuote.setText(text);
        }
    }


    private boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    private boolean writeLegacy(String name, String text) {
        if (!isExternalStorageWritable() || !hasLegacyPermission()) return false;
        File dir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS);
        if (!dir.exists() && !dir.mkdirs()) return false;
        File file = new File(dir, name);
        try (OutputStreamWriter out = new OutputStreamWriter(
                new FileOutputStream(file, false), StandardCharsets.UTF_8)) {
            out.write(text); // перезапись
            return true;
        } catch (IOException e) {
            Log.w(TAG, "Error writing " + file, e);
            return false;
        }
    }

    private String readLegacy(String name) {
        File dir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS);
        File file = new File(dir, name);
        try (FileInputStream in = new FileInputStream(file)) {
            return readAll(in);
        } catch (IOException e) {
            Log.w(TAG, "Read failed: " + e.getMessage());
            return null;
        }
    }

    private Uri findInMediaStore(String name) {
        Uri collection = MediaStore.Files.getContentUri("external");
        String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=? AND "
                + MediaStore.MediaColumns.RELATIVE_PATH + "=?";
        String[] args = {name, Environment.DIRECTORY_DOCUMENTS + "/"};
        try (Cursor c = getContentResolver().query(collection,
                new String[]{MediaStore.MediaColumns._ID}, selection, args, null)) {
            if (c != null && c.moveToFirst()) {
                long id = c.getLong(0);
                return Uri.withAppendedPath(collection, String.valueOf(id));
            }
        }
        return null;
    }

    private boolean writeMediaStore(String name, String text) {
        ContentResolver resolver = getContentResolver();
        Uri uri = findInMediaStore(name);
        if (uri == null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOCUMENTS);
            uri = resolver.insert(MediaStore.Files.getContentUri("external"), values);
        }
        if (uri == null) return false;
        try (OutputStream out = resolver.openOutputStream(uri, "wt")) {
            if (out == null) return false;
            out.write(text.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            Log.w(TAG, "Error writing " + name, e);
            return false;
        }
    }

    private String readMediaStore(String name) {
        Uri uri = findInMediaStore(name);
        if (uri == null) return null;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            return in == null ? null : readAll(in);
        } catch (IOException e) {
            Log.w(TAG, "Read failed: " + e.getMessage());
            return null;
        }
    }

    private String readAll(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8));
        String line = reader.readLine();
        while (line != null) {
            sb.append(line);
            line = reader.readLine();
            if (line != null) sb.append('\n');
        }
        Log.w(TAG, "Read from file successful: " + sb);
        return sb.toString();
    }
}