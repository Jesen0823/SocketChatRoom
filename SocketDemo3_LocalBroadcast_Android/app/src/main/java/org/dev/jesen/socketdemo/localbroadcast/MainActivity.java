package org.dev.jesen.socketdemo.localbroadcast;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private UDPProvider.Provider mProvider;

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

        // 生成设备唯一标示
        String sn = UUID.randomUUID().toString();
        mProvider = new UDPProvider.Provider(sn);
        mProvider.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mProvider.exit();
    }
}