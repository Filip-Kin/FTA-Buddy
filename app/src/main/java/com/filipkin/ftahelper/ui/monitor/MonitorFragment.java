package com.filipkin.ftahelper.ui.monitor;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.filipkin.ftahelper.MainActivity;
import com.filipkin.ftahelper.R;
import com.filipkin.ftahelper.databinding.FragmentMonitorBinding;
import com.filipkin.ftahelper.util.Fetch;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MonitorFragment extends Fragment {

    private FragmentMonitorBinding binding;
    private String[] fieldStates;
    private String[] dsStates;
    private int[] colors;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMonitorBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("FTABuddy", 0);

        boolean relayEnabled = sharedPreferences.getBoolean("relayEnabled", false);
        binding.relaySwitch.setChecked(relayEnabled);

        String savedEvent = sharedPreferences.getString("eventCode", null);
        if (savedEvent != null) {
            binding.monitorEvent.setText(savedEvent);
            parseEventCodeAndConnect(savedEvent);
        }

        // Change in event code or ip field
        binding.monitorEvent.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String eventInput = binding.monitorEvent.getText().toString();

                sharedPreferences.edit().putString("eventCode", eventInput).apply();

                parseEventCodeAndConnect(eventInput);

                binding.monitorEvent.clearFocus();
            }
            return false;
        });

        // If the relay switch is changed then restart the connection procedure
        binding.relaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("relayEnabled", isChecked).apply();

            String eventInput = binding.monitorEvent.getText().toString();
            if (!eventInput.isEmpty()) {
                parseEventCodeAndConnect(eventInput);
            }
        });

        Resources res = getResources();

        // TODO: Navigate to notes by clicking team number
        /*
        binding.blue1Number.setOnClickListener(v -> {
            System.out.println("Navigate away");
            sharedPreferences.edit().putString("selectedTeam", ((TextView) v).getText().toString()).apply();
            NavOptions.Builder navBuilder = new NavOptions.Builder();
            NavOptions navOptions = navBuilder.setPopUpTo(R.id.navigation_notes, false).build();
            NavController navController = NavHostFragment.findNavController(MonitorFragment.this);
            navController.navigate(R.id.navigation_notes, null, navOptions);
        });
         */

        fieldStates = res.getStringArray(R.array.field_state);
        dsStates = res.getStringArray(R.array.ds_state);
        colors = new int[]{
                ContextCompat.getColor(root.getContext(), R.color.red_bad),
                ContextCompat.getColor(root.getContext(), R.color.green),
                ContextCompat.getColor(root.getContext(), R.color.green),
                ContextCompat.getColor(root.getContext(), R.color.yellow),
                ContextCompat.getColor(root.getContext(), R.color.yellow),
                ContextCompat.getColor(root.getContext(), R.color.red_bypass),
                ContextCompat.getColor(root.getContext(), R.color.black),
                ContextCompat.getColor(root.getContext(), R.color.white)
        };

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (root.isActivated()) {
                    handler.postDelayed(this, 200);
                    updateField(MainActivity.field);
                } else {
                    handler.removeCallbacks(this);
                }
            }
        }, 0);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @SuppressLint("SetTextI18n")
    public void updateField(FieldState newField) {
        binding.matchNumber.setText(getString(R.string.match_display, newField.match));
        binding.fieldState.setText(fieldStates[newField.field]);
        binding.fieldState.setBackgroundColor((newField.field == 1 || newField.field == 3 || newField.field == 4) ? colors[1] : colors[0]);
        binding.timeBehind.setText(newField.time);

        // Yes, I know... this is horrible.
        binding.blue1Number.setText(Integer.toString(newField.blue1.number));
        binding.blue1Ds.setBackgroundColor(colors[newField.blue1.ds]);
        binding.blue1Ds.setText(dsStates[newField.blue1.ds]);
        if (newField.blue1.ds == 6) {
            binding.blue1Ds.setTextColor(colors[7]);
        } else {
            binding.blue1Ds.setTextColor(colors[6]);
        }
        binding.blue1Radio.setBackgroundColor(colors[newField.blue1.radio]);
        if (newField.blue1.rio == 0) {
            binding.blue1Rio.setBackgroundColor(colors[0]);
        } else if (newField.blue1.code == 0) {
            binding.blue1Rio.setBackgroundColor(colors[3]);
        } else {
            binding.blue1Rio.setBackgroundColor(colors[1]);
        }
        binding.blue1Battery.setText(getString(R.string.battery, newField.blue1.battery));
        binding.blue1Ping.setText(getString(R.string.ping, newField.blue1.ping));
        binding.blue1Bwu.setText(getString(R.string.bwu, newField.blue1.bwu));

        binding.blue2Number.setText(Integer.toString(newField.blue2.number));
        binding.blue2Ds.setBackgroundColor(colors[newField.blue2.ds]);
        binding.blue2Ds.setText(dsStates[newField.blue2.ds]);
        if (newField.blue2.ds == 6) {
            binding.blue2Ds.setTextColor(colors[7]);
        } else {
            binding.blue2Ds.setTextColor(colors[6]);
        }
        binding.blue2Radio.setBackgroundColor(colors[newField.blue2.radio]);
        if (newField.blue2.rio == 0) {
            binding.blue2Rio.setBackgroundColor(colors[0]);
        } else if (newField.blue2.code == 0) {
            binding.blue2Rio.setBackgroundColor(colors[3]);
        } else {
            binding.blue2Rio.setBackgroundColor(colors[1]);
        }
        binding.blue2Battery.setText(getString(R.string.battery, newField.blue2.battery));
        binding.blue2Ping.setText(getString(R.string.ping, newField.blue2.ping));
        binding.blue2Bwu.setText(getString(R.string.bwu, newField.blue2.bwu));

        binding.blue3Number.setText(Integer.toString(newField.blue3.number));
        binding.blue3Ds.setBackgroundColor(colors[newField.blue3.ds]);
        binding.blue3Ds.setText(dsStates[newField.blue3.ds]);
        if (newField.blue3.ds == 6) {
            binding.blue3Ds.setTextColor(colors[7]);
        } else {
            binding.blue3Ds.setTextColor(colors[6]);
        }
        binding.blue3Radio.setBackgroundColor(colors[newField.blue3.radio]);
        if (newField.blue3.rio == 0) {
            binding.blue3Rio.setBackgroundColor(colors[0]);
        } else if (newField.blue3.code == 0) {
            binding.blue3Rio.setBackgroundColor(colors[3]);
        } else {
            binding.blue3Rio.setBackgroundColor(colors[1]);
        }
        binding.blue3Battery.setText(getString(R.string.battery, newField.blue3.battery));
        binding.blue3Ping.setText(getString(R.string.ping, newField.blue3.ping));
        binding.blue3Bwu.setText(getString(R.string.bwu, newField.blue3.bwu));

        binding.red1Number.setText(Integer.toString(newField.red1.number));
        binding.red1Ds.setBackgroundColor(colors[newField.red1.ds]);
        binding.red1Ds.setText(dsStates[newField.red1.ds]);
        if (newField.red1.ds == 6) {
            binding.red1Ds.setTextColor(colors[7]);
        } else {
            binding.red1Ds.setTextColor(colors[6]);
        }
        binding.red1Radio.setBackgroundColor(colors[newField.red1.radio]);
        if (newField.red1.rio == 0) {
            binding.red1Rio.setBackgroundColor(colors[0]);
        } else if (newField.red1.code == 0) {
            binding.red1Rio.setBackgroundColor(colors[3]);
        } else {
            binding.red1Rio.setBackgroundColor(colors[1]);
        }
        binding.red1Battery.setText(getString(R.string.battery, newField.red1.battery));
        binding.red1Ping.setText(getString(R.string.ping, newField.red1.ping));
        binding.red1Bwu.setText(getString(R.string.bwu, newField.red1.bwu));

        binding.red2Number.setText(Integer.toString(newField.red2.number));
        binding.red2Ds.setBackgroundColor(colors[newField.red2.ds]);
        binding.red2Ds.setText(dsStates[newField.red2.ds]);
        if (newField.red2.ds == 6) {
            binding.red2Ds.setTextColor(colors[7]);
        } else {
            binding.red2Ds.setTextColor(colors[6]);
        }
        binding.red2Radio.setBackgroundColor(colors[newField.red2.radio]);
        if (newField.red2.rio == 0) {
            binding.red2Rio.setBackgroundColor(colors[0]);
        } else if (newField.red2.code == 0) {
            binding.red2Rio.setBackgroundColor(colors[3]);
        } else {
            binding.red2Rio.setBackgroundColor(colors[1]);
        }
        binding.red2Battery.setText(getString(R.string.battery, newField.red2.battery));
        binding.red2Ping.setText(getString(R.string.ping, newField.red2.ping));
        binding.red2Bwu.setText(getString(R.string.bwu, newField.red2.bwu));

        binding.red3Number.setText(Integer.toString(newField.red3.number));
        binding.red3Ds.setBackgroundColor(colors[newField.red3.ds]);
        binding.red3Ds.setText(dsStates[newField.red3.ds]);
        if (newField.red3.ds == 6) {
            binding.red3Ds.setTextColor(colors[7]);
        } else {
            binding.red3Ds.setTextColor(colors[6]);
        }
        binding.red3Radio.setBackgroundColor(colors[newField.red3.radio]);
        if (newField.red3.rio == 0) {
            binding.red3Rio.setBackgroundColor(colors[0]);
        } else if (newField.red3.code == 0) {
            binding.red3Rio.setBackgroundColor(colors[3]);
        } else {
            binding.red3Rio.setBackgroundColor(colors[1]);
        }
        binding.red3Battery.setText(getString(R.string.battery, newField.red3.battery));
        binding.red3Ping.setText(getString(R.string.ping, newField.red3.ping));
        binding.red3Bwu.setText(getString(R.string.bwu, newField.red3.bwu));
    };

    public void parseEventCodeAndConnect(String eventInput) {
        MainActivity.firstConnection = true;
        if (eventInput.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
            binding.relaySwitch.setChecked(false);
            binding.relaySwitch.setEnabled(false);
            ((MainActivity) requireActivity()).openWebSocket(URI.create("ws://" + eventInput + ":8284/"));
        } else {
            try {
                binding.relaySwitch.setEnabled(true);

                if (binding.relaySwitch.isChecked()) {
                    ((MainActivity) requireActivity()).openWebSocket(eventInput, URI.create("ws://server.filipkin.com:9014/"));
                } else {
                    String requestUrl = "https://ftahelper.filipkin.com/register/" + URLEncoder.encode(eventInput, "UTF-8");
                    Fetch.get(requestUrl, new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error connecting to cloud server", Toast.LENGTH_LONG).show());
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            if (response.code() == 404) {
                                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Event code not found", Toast.LENGTH_SHORT).show());
                                return;
                            }
                            try {
                                JSONObject ipJson = new JSONObject(response.body().string());
                                ((MainActivity) requireActivity()).openWebSocket(URI.create("ws://" + ipJson.getString("local_ip") + ":8284/"));
                            } catch (JSONException e) {
                                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error parsing JSON response from cloud server", Toast.LENGTH_LONG).show());
                            }
                        }
                    });
                }

            } catch (IOException e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error URI encoding event code", Toast.LENGTH_LONG).show());
            }
        }
    }
}