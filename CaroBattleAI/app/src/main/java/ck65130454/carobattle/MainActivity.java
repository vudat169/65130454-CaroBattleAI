package ck65130454.carobattle;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable; // THÊM MỚI: Để làm trong suốt nền viền đen mặc định của Dialog
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater; // THÊM MỚI: Để nạp file layout XML độ khó custom
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog; // ĐỔI MỚI: Dùng AlertDialog của AppCompat cho đồng bộ
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    // Khai báo các biến Button để điều khiển giao diện
    private Button btnPvP, btnPvE, btnHistory, btnExit;

    // Biến MediaPlayer dùng chung (public static) cho toàn bộ App
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

        // Tự động khởi tạo và phát nhạc ngay khi vừa mở Màn hình chính
        if (bgMediaPlayer == null) {
            bgMediaPlayer = MediaPlayer.create(this, R.raw.bg_music);
            if (bgMediaPlayer != null) {
                bgMediaPlayer.setLooping(true); // Phát lặp lại liên tục
                bgMediaPlayer.setVolume(0.8f, 0.8f); // Âm lượng 80% êm dịu
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

        // 3. ĐÃ NÂNG CẤP: Cài đặt sự kiện Click cho nút "Đấu với Máy" (Hiển thị hộp thoại Custom Neon)
        btnPvE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                LayoutInflater inflater = getLayoutInflater();
                // Nạp file giao diện chọn độ khó custom vào code Java
                View dialogView = inflater.inflate(R.layout.dialog_difficulty_layout, null);
                builder.setView(dialogView);

                final AlertDialog alertDialog = builder.create();

                // Ánh xạ 3 nút bấm (Dễ, Bình thường, Khó) từ file dialog_difficulty_layout.xml
                Button btnEasy = dialogView.findViewById(R.id.btnEasy);
                Button btnMedium = dialogView.findViewById(R.id.btnMedium);
                Button btnHard = dialogView.findViewById(R.id.btnHard);

                // Thao tác Click chọn mức DỄ (Easy)
                btnEasy.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialog.dismiss(); // Đóng bảng thông báo
                        isChangingActivity = true; // Giữ nhạc nền chạy liên tục không ngắt

                        Intent intent = new Intent(MainActivity.this, GameActivity.class);
                        intent.putExtra("game_mode", "pve");
                        intent.putExtra("difficulty", "easy");
                        startActivity(intent);
                    }
                });

                // Thao tác Click chọn mức BÌNH THƯỜNG (Medium)
                btnMedium.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialog.dismiss();
                        isChangingActivity = true;

                        Intent intent = new Intent(MainActivity.this, GameActivity.class);
                        intent.putExtra("game_mode", "pve");
                        intent.putExtra("difficulty", "medium");
                        startActivity(intent);
                    }
                });

                // Thao tác Click chọn mức KHÓ (Hard)
                btnHard.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialog.dismiss();
                        isChangingActivity = true;

                        Intent intent = new Intent(MainActivity.this, GameActivity.class);
                        intent.putExtra("game_mode", "pve");
                        intent.putExtra("difficulty", "hard");
                        startActivity(intent);
                    }
                });

                // Tối ưu quan trọng: Làm trong suốt phần nền vuông đen mặc định của hệ thống để hiển thị bo góc mượt mà
                if (alertDialog.getWindow() != null) {
                    alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                }

                // Hiển thị hộp thoại phong cách Cyberpunk lên màn hình
                alertDialog.show();
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

    // Quản lý trạng thái nhạc khi người dùng tương tác ẩn/hiện màn hình chính
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