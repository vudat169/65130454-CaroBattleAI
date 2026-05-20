package ck65130454.carobattle;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    // Khai báo các biến Button để điều khiển giao diện
    private Button btnPvP, btnPvE, btnHistory, btnExit;

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

        // 1. Ánh xạ (Kế thừa từ các ID đã đặt bên file activity_main.xml)
        btnPvP = findViewById(R.id.btnPvP);
        btnPvE = findViewById(R.id.btnPvE);
        btnHistory = findViewById(R.id.btnHistory);
        btnExit = findViewById(R.id.btnExit);

        // 2. Cài đặt sự kiện Click cho nút "Đấu với Người"
        btnPvP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                intent.putExtra("game_mode", "pvp"); // Gửi nhãn "pvp" sang màn hình Game
                startActivity(intent);
            }
        });

        // 3. Cài đặt sự kiện Click cho nút "Đấu với Máy"
        btnPvE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                intent.putExtra("game_mode", "pve"); // Gửi nhãn "pve" sang màn hình Game
                startActivity(intent);
            }
        });

        // 4. Cài đặt sự kiện Click cho nút "Lịch sử đấu"
        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });

        // 5. Cài đặt sự kiện Click cho nút "Thoát"
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Đóng hoàn toàn Activity này để thoát ứng dụng
            }
        });
    }
}