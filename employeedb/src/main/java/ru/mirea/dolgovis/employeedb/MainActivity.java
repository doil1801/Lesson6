package ru.mirea.dolgovis.employeedb;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

import ru.mirea.dolgovis.employeedb.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "EmployeeDB";

    private ActivityMainBinding binding;
    private SuperHeroDao heroDao;
    private List<SuperHero> heroes = new ArrayList<>();
    private SuperHero selected;

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

        AppDatabase db = App.getInstance().getDatabase();
        heroDao = db.superHeroDao();

        if (heroDao.getAll().isEmpty()) {
            insertHero("Человек-Паук", "Паутина и ловкость", 7);
            insertHero("Халк", "Огромная сила", 9);
        }

        binding.buttonAdd.setOnClickListener(v -> {
            SuperHero h = readForm();
            if (h != null) {
                heroDao.insert(h);
                clearForm();
                refreshList();
            }
        });

        binding.buttonUpdate.setOnClickListener(v -> {
            if (selected == null) {
                toast("Сначала выберите героя в списке");
                return;
            }
            SuperHero h = readForm();
            if (h != null) {
                selected.name = h.name;
                selected.power = h.power;
                selected.level = h.level;
                heroDao.update(selected);
                Log.d(TAG, "Обновлён: " + selected.name + " lvl " + selected.level);
                clearForm();
                refreshList();
            }
        });

        binding.buttonDelete.setOnClickListener(v -> {
            if (selected == null) {
                toast("Сначала выберите героя в списке");
                return;
            }
            heroDao.delete(selected);
            clearForm();
            refreshList();
        });

        binding.listViewHeroes.setOnItemClickListener((parent, view, position, id) -> {
            selected = heroes.get(position);
            binding.editTextName.setText(selected.name);
            binding.editTextPower.setText(selected.power);
            binding.editTextLevel.setText(String.valueOf(selected.level));
        });

        refreshList();
    }

    private void insertHero(String name, String power, int level) {
        SuperHero h = new SuperHero();
        h.name = name;
        h.power = power;
        h.level = level;
        heroDao.insert(h);
    }

    private SuperHero readForm() {
        String name = binding.editTextName.getText().toString().trim();
        String power = binding.editTextPower.getText().toString().trim();
        String levelStr = binding.editTextLevel.getText().toString().trim();
        if (name.isEmpty() || power.isEmpty() || levelStr.isEmpty()) {
            toast("Заполните все поля");
            return null;
        }
        SuperHero h = new SuperHero();
        h.name = name;
        h.power = power;
        try {
            h.level = Integer.parseInt(levelStr);
        } catch (NumberFormatException e) {
            toast("Уровень должен быть числом");
            return null;
        }
        return h;
    }

    private void clearForm() {
        selected = null;
        binding.editTextName.setText("");
        binding.editTextPower.setText("");
        binding.editTextLevel.setText("");
    }

    private void refreshList() {
        heroes = heroDao.getAll();
        List<String> lines = new ArrayList<>();
        for (SuperHero h : heroes) {
            lines.add(h.id + ". " + h.name + " | " + h.power + " | уровень " + h.level);
            Log.d(TAG, h.id + " " + h.name + " " + h.power + " " + h.level);
        }
        binding.listViewHeroes.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, lines));
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}