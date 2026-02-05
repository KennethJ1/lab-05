package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private Button deleteCityButton;
    private City selectedCity;

    private FirebaseFirestore db;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        deleteCityButton = findViewById(R.id.buttonDeleteCity);
        cityListView = findViewById(R.id.listviewCities);

        // create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        citiesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
                return;
            }
            if (value == null) {
                return;
            }

            cityArrayList.clear();
            for (QueryDocumentSnapshot snapshot : value) {
                // Read fields that match what we write in addCity/updateCity
                String name = snapshot.getString("name");
                String province = snapshot.getString("province");

                // Defensive: avoid adding null entries
                if (name != null && province != null) {
                    cityArrayList.add(new City(name, province));
                }
            }
            cityArrayAdapter.notifyDataSetChanged();
        });

        // set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(),"Add City");
        });

        deleteCityButton.setOnClickListener(v -> {
            if (selectedCity == null) {
                Toast.makeText(this, "Select a city first", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Delete city")
                    .setMessage("Delete \"" + selectedCity.getName() + "\"?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (d, w) -> {
                        deleteCity(selectedCity);
                        selectedCity = null;
                    })
                    .show();
        });

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            selectedCity = city;
            if (city != null) {
                Toast.makeText(MainActivity.this, "Selected: " + city.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        cityListView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            selectedCity = city;
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(), "City Details");
            return true;
        });

    }

    @Override
    public void updateCity(City city, String title, String year) {
        String oldName = city.getName();

        city.setName(title);
        city.setProvince(year);

        HashMap<String, String> data = new HashMap<>();
        data.put("name", city.getName());
        data.put("province", city.getProvince());

        // If the city name changed, the document ID changes too.
        // Delete the old doc and write the new one.
        citiesRef.document(oldName).delete();
        citiesRef.document(city.getName()).set(data);
    }

    @Override
    public void addCity(City city) {
        HashMap<String, String> data = new HashMap<>();
        data.put("name", city.getName());
        data.put("province", city.getProvince());

        DocumentReference docRef = citiesRef.document(city.getName());
        docRef.set(data);
    }

    @Override
    public void deleteCity(City city) {
        if (city == null) {
            return;
        }

        citiesRef.document(city.getName())
                .delete()
                .addOnSuccessListener(unused -> {
                    Log.d("Firestore", "Deleted " + city.getName());
                    Toast.makeText(MainActivity.this, "Deleted " + city.getName(), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Delete failed", e);
                    Toast.makeText(MainActivity.this, "Delete failed", Toast.LENGTH_SHORT).show();
                });
    }

    public void addDummyData(){
        City m1 = new City("Edmonton", "AB");
        City m2 = new City("Vancouver", "BC");
        cityArrayList.add(m1);
        cityArrayList.add(m2);
        cityArrayAdapter.notifyDataSetChanged();
    }


}