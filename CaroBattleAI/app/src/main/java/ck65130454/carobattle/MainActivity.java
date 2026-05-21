package ck65130454.carobattle;

import android.content.Intent;
import android.media.MediaPlayer; // THÊM MỚI: Thư viện phát nhạc nền
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

    // THÊM MỚI: Biến MediaPlayer dùng chung (public static) cho toàn bộ App
    public static MediaPlayer bgMediaPlayer;
    // Biến kiểm soát trạng thái xem có đang chủ động chuyển màn hình hay không
    private boolean isChangingActivity = false;

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

        // THÊM MỚI: Tự động khởi tạo và phát nhạc ngay khi vừa mở Màn hình chính
        if (bgMediaPlayer == null) {
            bgMediaPlayer = MediaPlayer.create(this, R.raw.bg_music);
            if (bgMediaPlayer != null) {
                bgMediaPlayer.setLooping(true); // Phát lặp lại liên tục
                bgMediaPlayer.setVolume(0.4f, 0.4f); // Âm lượng 40% êm dịu
                bgMediaPlayer.start();
            }
        }

        // 1. Ánh xạ (Kế thừa từ các ID đã đặt bên file activity_main.xml)
        btnPvP = findViewById(R.id.btnPvP);
        btnPvE = findViewById(R.id.btnPvE);
        btnHistory = findViewById(R.id.btnHistory);
        btnExit = findViewById(R.id.btnExit);

        // 2. Cài đặt sự kiện Click cho nút "Đấu với Người"
        btnPvP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isChangingActivity = true; // Đánh dấu là đang chuyển màn hình
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                intent.putExtra("game_mode", "pvp"); // Gửi nhãn "pvp" sang màn hình Game
                startActivity(intent);
            }
        });

        // 3. Cài đặt sự kiện Click cho nút "Đấu với Máy" (Hiển thị hộp thoại chọn độ khó)
        btnPvE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Định nghĩa danh sách các mức độ khó hiển thị trên menu pop-up
                final String[] levels = {"Dễ (Easy)", "Bình thường (Medium)", "Khó (Hard)"};

                // Khởi tạo một Dialog thông báo của hệ thống Android
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Chọn độ khó của Máy:");

                // Bắt sự kiện click chọn phần tử trong danh sách
                builder.setItems(levels, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        String selectedDifficulty = "easy"; // Vị trí thứ 0 là Dễ
                        if (which == 1) selectedDifficulty = "medium"; // Vị trí thứ 1 là Bình thường
                        if (which == 2) selectedDifficulty = "hard";   // Vị trí thứ 2 là Khó

                        isChangingActivity = true; // Đánh dấu là đang chuyển màn hình
                        // Đóng gói cả chế độ chơi và độ khó để chuyển tiếp sang màn hình chơi
                        Intent intent = new Intent(MainActivity.this, GameActivity.class);
                        intent.putExtra("game_mode", "pve");
                        intent.putExtra("difficulty", selectedDifficulty);
                        startActivity(intent);
                    }
                });

                // Hiển thị hộp thoại lên màn hình
                builder.show();
            }
        });

        // 4. Cài đặt sự kiện Click cho nút "Lịch sử đấu"
        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isChangingActivity = true; // Đánh dấu là đang chuyển màn hình
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });

        // 5. Cài đặt sự kiện Click cho nút "Thoát"
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Khi bấm thoát hoàn toàn app, giải phóng nhạc nền luôn
                if (bgMediaPlayer != null) {
                    bgMediaPlayer.stop();
                    bgMediaPlayer.release();
                    bgMediaPlayer = null;
                }
                finish(); // Đóng hoàn toàn Activity này để thoát ứng dụng
            }
        });
    }

    // THÊM MỚI: Quản lý trạng thái nhạc khi người dùng tương tác ẩn/hiện màn hình chính
    @Override
    protected void onPause() {
        super.onPause();
        // Nếu người dùng bấm Home thoát app (isChangingActivity = false), tạm dừng nhạc
        if (!isChangingActivity && bgMediaPlayer != null && bgMediaPlayer.isPlaying()) {
            bgMediaPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isChangingActivity = false; // Reset lại trạng thái
        // Khi quay trở lại màn hình chính, tiếp tục phát nhạc nền
        if (bgMediaPlayer != null && !bgMediaPlayer.isPlaying()) {
            bgMediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Đảm bảo dọn dẹp bộ nhớ sạch sẽ khi hệ thống hủy ứng dụng hoàn toàn
        if (isFinishing() && bgMediaPlayer != null) {
            bgMediaPlayer.stop();
            bgMediaPlayer.release();
            bgMediaPlayer = null;
        }
    }
}