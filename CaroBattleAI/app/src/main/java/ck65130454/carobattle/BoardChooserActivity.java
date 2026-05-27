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

public class BoardChooserActivity extends AppCompatActivity {

    // Khai báo các nút bấm điều hướng
    private androidx.appcompat.widget.AppCompatButton btnBoard4x4;
    private androidx.appcompat.widget.AppCompatButton btnBoard5x5;
    private Button btnCancel;

    // Biến lưu thông tin chế độ chơi nhận từ màn hình trước
    private String gameMode = "pvp";
    private String difficulty = "easy";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_board_chooser);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Ánh xạ các nút bấm từ layout XML sang Java
        btnBoard4x4 = findViewById(R.id.btnBoard4x4);
        btnBoard5x5 = findViewById(R.id.btnBoard5x5);
        btnCancel = findViewById(R.id.btnCancel);

        // 2. Nhận lại cấu hình (Chế độ chơi & Độ khó) mà người dùng đã chọn ở màn hình trước đó
        if (getIntent() != null) {
            if (getIntent().hasExtra("game_mode")) {
                gameMode = getIntent().getStringExtra("game_mode");
            }
            if (getIntent().hasExtra("difficulty")) {
                difficulty = getIntent().getStringExtra("difficulty");
            }
        }

        // 3. Xử lý sự kiện khi chọn Bàn cờ 4x4 -> Mở GameActivity
        btnBoard4x4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BoardChooserActivity.this, GameActivity.class);
                intent.putExtra("game_mode", gameMode);
                intent.putExtra("difficulty", difficulty);
                startActivity(intent);
                finish(); // Đóng màn hình chọn để tránh bị lặp khi bấm nút Back quay lại
            }
        });

        // 4. Xử lý sự kiện khi chọn Bàn cờ 5x5 -> Mở Game5x5Activity
        btnBoard5x5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BoardChooserActivity.this, Game5x5Activity.class);
                intent.putExtra("game_mode", gameMode);
                intent.putExtra("difficulty", difficulty);
                startActivity(intent);
                finish(); // Đóng màn hình chọn
            }
        });

        // 5. Xử lý sự kiện khi bấm nút Quay lại
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Đóng màn hình này và quay về menu trước đó
            }
        });
    }
}