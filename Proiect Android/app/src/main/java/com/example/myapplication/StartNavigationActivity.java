package com.example.myapplication;

import android.annotation.SuppressLint;
import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.myapplication.problem.Location;
import com.example.myapplication.problem.Waypoint;
import com.google.android.material.navigation.NavigationView;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static android.widget.Toast.*;

public class StartNavigationActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mToggle;
    private ApplicationData appData = new ApplicationData();
    private ActionBar actionBar;
    private Location start, destination;
    private Dialog errorDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        errorDialog = new Dialog(this);

        setContentView(R.layout.start_navigation);
        actionBar = getSupportActionBar();
        actionBar.setTitle(appData.getCurrentBuilding().getName());
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        mToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.open, R.string.close);
        mDrawerLayout.addDrawerListener(mToggle);
        mToggle.syncState();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.bringToFront();
        navigationView.setNavigationItemSelectedListener(this);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);


        ImageView scanLocation = findViewById(R.id.cameraBtn);
        scanLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestCameraPermission("cameraBtn");

            }
        });
        startNavigationButton();
        generateSuggestedPlaces(4);
    }

    private void startNavigationButton() {
        final Button startNavigation = findViewById(R.id.navigationBtn);
        startNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNavigation.setEnabled(false);
                String startName = ((TextView) findViewById(R.id.currentLocation)).getText().toString();
                if (appData.getCurrentBuilding().getLocation(startName) == null) {
                    makeText(getApplicationContext(), "Invalid Starting Point", LENGTH_SHORT).show();
                    startNavigation.setEnabled(true);

                } else {
                    String destinationName = ((TextView) findViewById(R.id.destination)).getText().toString();
                    if (appData.getCurrentBuilding().getLocation(destinationName) == null) {
                        makeText(getApplicationContext(), "Invalid Destination", LENGTH_SHORT).show();
                        startNavigation.setEnabled(true);
                    } else {
                        requestCameraPermission("navigationBtn");
                    }
                }

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (mToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.chooseBuilding) {
            showBuildings();
        }
        return false;
    }

    private void showBuildings() {
        List<String> buildingNames = appData.getBuildings();
        final String[] buildings = new String[buildingNames.size()];
        int i = 0;
        for (String temp : buildingNames) {
            buildings[i] = temp;
            i++;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick a building");
        builder.setItems(buildings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                appData.setCurrentBuildingName(buildings[which]);
                appData.setCurrentBuilding(appData.getBuildingByName(buildings[which]));
                generateSuggestedPlaces(4);
                actionBar = getSupportActionBar();
                actionBar.setTitle(buildings[which]);
            }

        });
        builder.show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 1) {
            String msg = data.getStringExtra("returnedData");
            Location location = appData.getCurrentBuilding().getLocationById(Integer.parseInt(msg));
            EditText editText = findViewById(R.id.currentLocation);
            editText.setText(location.getName());
        }
    }

    public void getWaypoints(String start, String destination) {
        // the url is different for every computer.
        // for emulator use 10.0.0.2:5000/
        // for device, run ipconfig in cmd and get ipv4 address

        String url = "http://192.168.1.3:5000/route/";

        url = url.concat(appData.getCurrentBuilding().getName() + "?start=" + start + "&" + "destination=" + destination);
        System.out.println(url);
        final RequestQueue requestQueue = Volley.newRequestQueue(StartNavigationActivity.this);

        JsonArrayRequest jsonObjectRequest = new JsonArrayRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d("JsonObject", "Response: " + response.toString());

                        List<Waypoint> waypointList = JsonParser.parseRoute(response);
                        for (Waypoint waypoint : waypointList)
                            System.out.println(waypoint);
                        appData.setWaypoints(waypointList);
                        Intent intent = new Intent(StartNavigationActivity.this, NavigationActivity.class);
                        intent.putStringArrayListExtra("instructions", appData.getAllInstructions());
                        intent.putIntegerArrayListExtra("codesToScan", appData.getAllCodesToScan());
                        startActivity(intent);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showPopup();
                        Log.e("JsonError", "Error on get JSON request!");
                    }
                });
        requestQueue.add(jsonObjectRequest);
    }

    private void generateSuggestedPlaces(int howMany) {
        final ListView lv = (ListView) findViewById(R.id.listView);
        List<Location> topLocations = appData.getCurrentBuilding().getTopLocations(howMany);

        String[] locations = new String[topLocations.size()];

        int i = 0;
        for (Location temp : topLocations) {
            locations[i] = temp.getName();
            i++;
        }

        List<String> locationsList = new ArrayList<String>(Arrays.asList(locations));
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>
                (this, R.layout.custom_list_item, locationsList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Get the Item from ListView
                View view = super.getView(position, convertView, parent);

                // Initialize a TextView for ListView each Item
                TextView tv = (TextView) view.findViewById(R.id.list_item_text);

                // Set the text color of TextView (ListView Item)
                //tv.setTextColor(Color.BLACK);
               // tv.setBackgroundColor(Color.parseColor("#F1F6FB"));

                // Generate ListView Item using TextView
                return view;
            }
        };
        lv.setAdapter(arrayAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onItemClick(AdapterView<?> parent,
                                    View view, int position, long id) {
                String myLocation = (String) parent.getAdapter().getItem(position);
                String startName = ((TextView) findViewById(R.id.currentLocation)).getText().toString();
                String destinationName = ((TextView) findViewById(R.id.destination)).getText().toString();
                if (startName.length()==0) {
                    start = ApplicationData.currentBuilding.getLocation(myLocation);
                    TextView mTextView = (TextView) findViewById(R.id.currentLocation);
                    mTextView.setText(myLocation);
                } else if (destinationName.length()==0) {
                    destination = ApplicationData.currentBuilding.getLocation(myLocation);
                    TextView mTextView = (TextView) findViewById(R.id.destination);
                    mTextView.setText(myLocation);
                } else {
                    makeText(getApplicationContext(), "You already have an input", LENGTH_SHORT).show();
                }

            }

        });

    }


    public void refreshActivity() {
        finish();
        startActivity(getIntent());
    }

    public void showPopup() {
        errorDialog.setContentView(R.layout.error_popup);
        LinearLayout popup = (LinearLayout) errorDialog.findViewById(R.id.errorInfo);
        popup.setVisibility(View.VISIBLE);
        Button closePopup = (Button) errorDialog.findViewById(R.id.refreshBtn);
        closePopup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                errorDialog.dismiss();
                refreshActivity();
            }
        });
        errorDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        errorDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        errorDialog.show();
    }

    public void openTimetableActivity(MenuItem item) {
        Intent intent = new Intent(StartNavigationActivity.this, TimetableActivity.class);
        startActivity(intent);
    }
    public void requestCameraPermission(final String check) {

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        // permission is granted
                        if(check.equals("cameraBtn")){
                            Intent intent = new Intent(StartNavigationActivity.this, ScanLocationActivity.class);
                            startActivityForResult(intent, 1);}
                        else{
                            final Button startNavigation = findViewById(R.id.navigationBtn);
                            String startName = ((TextView) findViewById(R.id.currentLocation)).getText().toString();
                            String destinationName = ((TextView) findViewById(R.id.destination)).getText().toString();
                            Location start = ApplicationData.currentBuilding.getLocation(startName);
                            Location destination = ApplicationData.currentBuilding.getLocation(destinationName);
                            getWaypoints(String.valueOf(start.getId()), String.valueOf(destination.getId()));
                            Timer buttonTimer = new Timer();
                            buttonTimer.schedule(new TimerTask() {

                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {

                                        @Override
                                        public void run() {
                                            startNavigation.setEnabled(true);
                                        }
                                    });
                                }
                            }, 5000);

                        }
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        // check for permanent denial of permission
                        if (response.isPermanentlyDenied()) {
                            if (check.equals("navigationBtn")) {
                                final Button startNavigation = findViewById(R.id.navigationBtn);
                                startNavigation.setEnabled(true);
                            }
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        if(check.equals("navigationBtn")){
                            final Button startNavigation = findViewById(R.id.navigationBtn);
                            startNavigation.setEnabled(true);}
                        token.continuePermissionRequest();
                    }
                }).check();

    }


    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(StartNavigationActivity.this);
        builder.setTitle("Need Permissions");
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                openSettings();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();

    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }
}