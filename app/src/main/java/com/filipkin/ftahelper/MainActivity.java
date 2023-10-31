package com.filipkin.ftahelper;

import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.filipkin.ftahelper.databinding.ActivityMainBinding;
import com.filipkin.ftahelper.ui.monitor.FieldState;
import com.filipkin.ftahelper.ui.monitor.MonitorFragment;
import com.filipkin.ftahelper.util.WebSocket;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    public static boolean firstConnection = true;
    public static URI currently_connected_uri;
    private WebSocket ws;
    public static FieldState field = new FieldState() {};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_monitor, R.id.navigation_flashcards, R.id.navigation_reference, R.id.navigation_notes)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        KeyboardVisibilityEvent.setEventListener(this, isOpen -> {
            if (isOpen) {
                binding.navView.setVisibility(View.GONE);
            } else {
                binding.navView.setVisibility(View.VISIBLE);
            }
        });
    }


    public void openWebSocket(URI uri) {
        openWebSocket(null, uri);
    }

    public void openWebSocket(String eventInput, URI uri) {
        Log.i("Websocket", "Current connection " + currently_connected_uri);
        if (uri.equals(currently_connected_uri)) return;
        Log.i("Websocket", "Connection initializing to " + uri);
        firstConnection = true;
        if (ws != null && ws.open) ws.close();
        ws = new WebSocket(uri) {
            int failedConnections = 0;
            @Override
            public void onTextReceived(String s) {
                final String message = s;
                runOnUiThread(() -> {
                    Log.i("WebSocket", message);
                    try {
                        JSONObject jObject = new JSONObject(message);
                        if (!jObject.getString("type").equals("monitorUpdate")) return;
                        field.field = jObject.getInt("field");
                        field.match = jObject.getInt("match");
                        field.time = jObject.getString("time");

                        JSONObject blue1 = jObject.getJSONObject("blue1");
                        JSONObject blue2 = jObject.getJSONObject("blue2");
                        JSONObject blue3 = jObject.getJSONObject("blue3");
                        JSONObject red1 = jObject.getJSONObject("red1");
                        JSONObject red2 = jObject.getJSONObject("red2");
                        JSONObject red3 = jObject.getJSONObject("red3");

                        Vibrator v = (Vibrator) getSystemService(getApplicationContext().VIBRATOR_SERVICE);

                        // Vibrate on lost connection if match in progress
                        if (field.field < 4) {
                            if ((field.blue1.ds == 1 && field.blue1.ds > blue1.getInt("ds")) ||
                                    (field.blue2.ds == 1 && field.blue2.ds > blue2.getInt("ds")) ||
                                    (field.blue3.ds == 1 && field.blue3.ds > blue3.getInt("ds")) ||
                                    (field.red1.ds == 1 && field.red1.ds > red1.getInt("ds")) ||
                                    (field.red2.ds == 1 && field.red2.ds > red2.getInt("ds")) ||
                                    (field.red3.ds == 1 && field.red3.ds > red3.getInt("ds"))) {
                                v.vibrate(VibrationEffect.createWaveform(new long[]{1000, 200, 300, 200, 300}, new int[]{255, 0, 255, 0, 255}, -1));
                                Log.i("Monitor", "Lost Driverstation Ethernet");
                            } else if ((field.blue1.ds == 1 && field.blue1.ds < blue1.getInt("ds")) ||
                                    (field.blue2.ds == 1 && field.blue2.ds < blue2.getInt("ds")) ||
                                    (field.blue3.ds == 1 && field.blue3.ds < blue3.getInt("ds")) ||
                                    (field.red1.ds == 1 && field.red1.ds < red1.getInt("ds")) ||
                                    (field.red2.ds == 1 && field.red2.ds < red2.getInt("ds")) ||
                                    (field.red3.ds == 1 && field.red3.ds < red3.getInt("ds"))) {
                                v.vibrate(VibrationEffect.createWaveform(new long[]{300, 200, 300, 200, 300}, new int[]{255, 0, 255, 0, 255}, -1));
                                Log.i("Monitor", "Lost Driverstation");
                            } else if (field.blue1.radio > blue1.getInt("radio") ||
                                    field.blue2.radio > blue2.getInt("radio") ||
                                    field.blue3.radio > blue3.getInt("radio") ||
                                    field.red1.radio > red1.getInt("radio") ||
                                    field.red2.radio > red2.getInt("radio") ||
                                    field.red3.radio > red3.getInt("radio")) {
                                v.vibrate(VibrationEffect.createWaveform(new long[]{300, 200, 300}, new int[]{255, 0, 255}, -1));
                                Log.i("Monitor", "Lost Radio");
                            } else if (field.blue1.rio > blue1.getInt("rio") ||
                                    field.blue2.rio > blue2.getInt("rio") ||
                                    field.blue3.rio > blue3.getInt("rio") ||
                                    field.red1.rio > red1.getInt("rio") ||
                                    field.red2.rio > red2.getInt("rio") ||
                                    field.red3.rio > red3.getInt("rio")) {
                                v.vibrate(VibrationEffect.createOneShot(500, 255));
                                Log.i("Monitor", "Lost Rio");
                            } else if (field.blue1.code > blue1.getInt("code") ||
                                    field.blue2.code > blue2.getInt("code") ||
                                    field.blue3.code > blue3.getInt("code") ||
                                    field.red1.code > red1.getInt("code") ||
                                    field.red2.code > red2.getInt("code") ||
                                    field.red3.code > red3.getInt("code")) {
                                v.vibrate(VibrationEffect.createOneShot(200, 255));
                                Log.i("Monitor", "Lost Code");
                            }
                        }

                        field.blue1.number = blue1.getInt("number");
                        field.blue1.ds = blue1.getInt("ds");
                        field.blue1.radio = blue1.getInt("radio");
                        field.blue1.rio = blue1.getInt("rio");
                        field.blue1.code = blue1.getInt("code");
                        field.blue1.bwu = blue1.getDouble("bwu");
                        field.blue1.battery = blue1.getDouble("battery");
                        field.blue1.ping = blue1.getInt("ping");
                        field.blue1.packets = blue1.getInt("packets");

                        field.blue2.number = blue2.getInt("number");
                        field.blue2.ds = blue2.getInt("ds");
                        field.blue2.radio = blue2.getInt("radio");
                        field.blue2.rio = blue2.getInt("rio");
                        field.blue2.code = blue2.getInt("code");
                        field.blue2.bwu = blue2.getDouble("bwu");
                        field.blue2.battery = blue2.getDouble("battery");
                        field.blue2.ping = blue2.getInt("ping");
                        field.blue2.packets = blue2.getInt("packets");

                        field.blue3.number = blue3.getInt("number");
                        field.blue3.ds = blue3.getInt("ds");
                        field.blue3.radio = blue3.getInt("radio");
                        field.blue3.rio = blue3.getInt("rio");
                        field.blue3.code = blue3.getInt("code");
                        field.blue3.bwu = blue3.getDouble("bwu");
                        field.blue3.battery = blue3.getDouble("battery");
                        field.blue3.ping = blue3.getInt("ping");
                        field.blue3.packets = blue3.getInt("packets");

                        field.red1.number = red1.getInt("number");
                        field.red1.ds = red1.getInt("ds");
                        field.red1.radio = red1.getInt("radio");
                        field.red1.rio = red1.getInt("rio");
                        field.red1.code = red1.getInt("code");
                        field.red1.bwu = red1.getDouble("bwu");
                        field.red1.battery = red1.getDouble("battery");
                        field.red1.ping = red1.getInt("ping");
                        field.red1.packets = red1.getInt("packets");

                        field.red2.number = red2.getInt("number");
                        field.red2.ds = red2.getInt("ds");
                        field.red2.radio = red2.getInt("radio");
                        field.red2.rio = red2.getInt("rio");
                        field.red2.code = red2.getInt("code");
                        field.red2.bwu = red2.getDouble("bwu");
                        field.red2.battery = red2.getDouble("battery");
                        field.red2.ping = red2.getInt("ping");
                        field.red2.packets = red2.getInt("packets");

                        field.red3.number = red3.getInt("number");
                        field.red3.ds = red3.getInt("ds");
                        field.red3.radio = red3.getInt("radio");
                        field.red3.rio = red3.getInt("rio");
                        field.red3.code = red3.getInt("code");
                        field.red3.bwu = red3.getDouble("bwu");
                        field.red3.battery = red3.getDouble("battery");
                        field.red3.ping = red3.getInt("ping");
                        field.red3.packets = red3.getInt("packets");

                        // TODO: Notify fragment of update
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                });
            }

            @Override
            public void onOpen() {
                Log.i("Websocket", "Connection established");
                if (!firstConnection) return;
                firstConnection = false;
                currently_connected_uri = uri;
                if (eventInput != null) {
                    this.send("client-"+eventInput);
                }
                this.open = true;
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Connected to websocket", Toast.LENGTH_SHORT).show());

                // Keep alive ping every minute
                new Timer().scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        ws.send("ping");
                    }
                }, 0, 60000);
            }

            @Override
            public void onException(Exception e) {
                failedConnections++;
                if (failedConnections == 3) {
                    this.close();
                }
                System.out.println(e.getMessage());
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Error connecting " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onCloseReceived() {
                Log.i("WebSocket", "Closed");
                currently_connected_uri = null;
            }
        };
    }
}