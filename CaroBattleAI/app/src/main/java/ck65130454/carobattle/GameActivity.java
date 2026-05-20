package ck65130454.carobattle;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class GameActivity extends AppCompatActivity {

    private CaroEngine gameEngine; // Đối tượng xử lý logic mảng và thuật toán thắng thua
    private Button[][] buttons = new Button[4][4]; // Mảng 2 chiều chứa 16 nút bấm trên giao diện
    private TextView tvTurnStatus;
    private Button btnReset;

    private String currentStringPlayer = "X"; // Mặc định người chơi X đi trước
    private boolean isGameActive = true; // Trạng thái trận đấu (true: đang chơi, false: đã dừng)
    private String gameMode = "pvp"; // Chế độ chơi mặc định

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game);

        // Giữ nguyên đoạn xử lý căn lề hệ thống của bạn để giao diện không bị lỗi tràn viền
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Khởi tạo thực thể cho bộ não CaroEngine
        gameEngine = new CaroEngine();

        // 2. Nhận dữ liệu chế độ chơi từ MainActivity truyền sang
        if (getIntent() != null && getIntent().hasExtra("game_mode")) {
            gameMode = getIntent().getStringExtra("game_mode");
        }

        // 3. Ánh xạ các thành phần hiển thị trạng thái và nút chức năng
        tvTurnStatus = findViewById(R.id.tvTurnStatus);
        btnReset = findViewById(R.id.btnReset);

        if (gameMode.equals("pve")) {
            tvTurnStatus.setText("Chế độ: Đấu với Máy (Chưa code AI)");
        } else {
            tvTurnStatus.setText("Lượt của: Người chơi X");
        }

        // 4. Sử dụng vòng lặp tự động ánh xạ và lắng nghe sự kiện cho 16 nút bấm dựa vào tọa độ ID
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                // Tạo chuỗi tên ID tương ứng như "btn_0_0", "btn_0_1",... để tìm tài nguyên
                String buttonID = "btn_" + i + "_" + j;
                int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                buttons[i][j] = findViewById(resID);

                final int row = i;
                final int col = j;

                // Thiết lập sự kiện lắng nghe thao tác nhấn của người dùng trên ô cờ
                buttons[i][j].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onCellClicked(row, col);
                    }
                });
            }
        }

        // 5. Thiết lập sự kiện cho nút "Chơi lại"
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetGame();
            }
        });
    }

    // Phương thức xử lý khi một ô cờ tại vị trí (row, col) được nhấn chọn
    private void onCellClicked(int row, int col) {
        // Nếu trận đấu đã ngưng hoặc ô cờ đã được đánh dấu từ trước thì không thực thi tiếp
        if (!isGameActive || !gameEngine.isCellEmpty(row, col)) {
            return;
        }

        // Xử lý luồng luật chơi cho chế độ Đấu với Người (PvP)
        if (gameMode.equals("pvp")) {
            // Cập nhật giá trị vào mảng logic bên trong CaroEngine
            gameEngine.makeMove(row, col, currentStringPlayer);

            // Ghi ký tự hiển thị trực tiếp lên nút bấm giao diện
            buttons[row][col].setText(currentStringPlayer);

            // Kiểm tra điều kiện thắng sau nước đi hiện tại
            if (gameEngine.checkWin(currentStringPlayer)) {
                tvTurnStatus.setText("NGƯỜI CHƠI " + currentStringPlayer + " CHIẾN THẮNG!");
                Toast.makeText(this, "Chúc mừng người chơi " + currentStringPlayer + "!", Toast.LENGTH_LONG).show();
                isGameActive = false;
            }
            // Kiểm tra điều kiện hòa khi toàn bộ các ô cờ trên lưới đều đã kín chỗ
            else if (gameEngine.isBoardFull()) {
                tvTurnStatus.setText("TRẬN ĐẤU HÒA!");
                Toast.makeText(this, "Bàn cờ đã đầy! Kết quả hòa.", Toast.LENGTH_LONG).show();
                isGameActive = false;
            }
            // Nếu chưa kết thúc, tiến hành chuyển lượt cho người chơi tiếp theo
            else {
                currentStringPlayer = currentStringPlayer.equals("X") ? "O" : "X";
                tvTurnStatus.setText("Lượt của: Người chơi " + currentStringPlayer);
            }
        } else {
            Toast.makeText(this, "Tính năng Đấu với Máy đang được xây dựng!", Toast.LENGTH_SHORT).show();
        }
    }

    // Phương thức đưa các thông số trận đấu và giao diện về trạng thái xuất phát
    private void resetGame() {
        gameEngine.resetBoard(); // Reset dữ liệu mảng ngầm
        currentStringPlayer = "X"; // Mặc định người chơi X đi tiên phong
        isGameActive = true;

        if (gameMode.equals("pve")) {
            tvTurnStatus.setText("Chế độ: Đấu với Máy (Chưa code AI)");
        } else {
            tvTurnStatus.setText("Lượt của: Người chơi X");
        }

        // Quét qua mảng và làm sạch toàn bộ văn bản hiển thị trên các nút bấm bàn cờ
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                buttons[i][j].setText("");
            }
        }
    }
}