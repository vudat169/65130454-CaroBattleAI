package ck65130454.carobattle;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable; // Xử lý làm trong suốt viền dialog mặc định
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog; // Dùng AlertDialog của AppCompat cho đồng bộ
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Thư viện kết nối Firebase Realtime Database để ghi lịch sử
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

// Thư viện định dạng Ngày/Giờ hệ thống
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private CaroEngine gameEngine;
    private androidx.appcompat.widget.AppCompatButton[][] buttons = new androidx.appcompat.widget.AppCompatButton[4][4];

    private TextView tvTurnStatus;
    private Button btnReset;
    private Button btnBack;

    private String currentStringPlayer = "X";
    private boolean isGameActive = true;
    private String gameMode = "pvp";
    private String difficulty = "easy";

    private String playerXName = "Người chơi X";
    private String playerOName = "Người chơi O";
    private boolean isChangingActivity = false;

    // Biến kết nối tới cơ sở dữ liệu Firebase
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        gameEngine = new CaroEngine();

        // Khởi tạo kết nối tới node "History" trên Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("History");

        if (getIntent() != null) {
            if (getIntent().hasExtra("game_mode")) {
                gameMode = getIntent().getStringExtra("game_mode");
            }
            if (getIntent().hasExtra("difficulty")) {
                difficulty = getIntent().getStringExtra("difficulty");
            }
        }

        tvTurnStatus = findViewById(R.id.tvTurnStatus);
        btnReset = findViewById(R.id.btnReset);
        btnBack = findViewById(R.id.btnBack);

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                String buttonID = "btn_" + i + "_" + j;
                int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                buttons[i][j] = findViewById(resID);

                final int row = i;
                final int col = j;

                buttons[i][j].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onCellClicked(row, col);
                    }
                });
            }
        }

        if (gameMode.equals("pvp")) {
            showNameInputDialog();
        } else {
            determineFirstTurnRandomly();
        }

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetGame();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isChangingActivity = true;
                finish();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                isChangingActivity = true;
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isChangingActivity && MainActivity.bgMediaPlayer != null && MainActivity.bgMediaPlayer.isPlaying()) {
            MainActivity.bgMediaPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isChangingActivity = false;
        if (MainActivity.bgMediaPlayer != null && !MainActivity.bgMediaPlayer.isPlaying()) {
            MainActivity.bgMediaPlayer.start();
        }
    }

    // =========================================================================
    // ĐÃ SỬA: Bắt buộc nhập tên, hiển thị thông báo lỗi mang phong cách game đối kháng
    // =========================================================================
    private void showNameInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        // Nạp file layout custom mới tinh của bạn vào đây
        View dialogView = inflater.inflate(R.layout.dialog_name_input_layout, null);
        builder.setView(dialogView);
        builder.setCancelable(false); // Bắt buộc người chơi thao tác trên bảng mới được vào game

        final AlertDialog alertDialog = builder.create();

        // Ánh xạ chính xác các ID từ file dialog_name_input_layout.xml của bạn
        final EditText etPlayerX = dialogView.findViewById(R.id.etPlayerX);
        final EditText etPlayerO = dialogView.findViewById(R.id.etPlayerO);
        final TextView tvErrorMessage = dialogView.findViewById(R.id.tvErrorMessage); // Ánh xạ TextView báo lỗi
        Button btnStartGame = dialogView.findViewById(R.id.btnStartGame);

        // Bắt sự kiện khi nhấn nút "BẮT ĐẦU CHIẾN"
        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nameX = etPlayerX.getText().toString().trim();
                String nameO = etPlayerO.getText().toString().trim();

                // KIỂM TRA ĐẦU VÀO: Nếu một trong hai hoặc cả hai ô trống tên
                if (nameX.isEmpty() || nameO.isEmpty()) {
                    // Kích hoạt hiển thị TextView thông báo lỗi
                    tvErrorMessage.setVisibility(View.VISIBLE);

                    // Cập nhật thông tin cảnh báo phù hợp với từng ngữ cảnh
                    if (nameX.isEmpty() && nameO.isEmpty()) {
                        tvErrorMessage.setText("⚠️ CẢ HAI NGƯỜI CHƠI CHƯA GHI TÊN !");
                    } else if (nameX.isEmpty()) {
                        tvErrorMessage.setText("⚠️ X CHƯA KHAI BÁO DANH TÍNH !");
                    } else {
                        tvErrorMessage.setText("⚠️ O CHƯA KHAI BÁO DANH TÍNH !");
                    }
                    return; // Chặn đứng luồng xử lý, không cho đóng bảng hay vào game
                }

                // Nếu đã điền đủ thông tin, gán vào các biến toàn cục
                playerXName = nameX;
                playerOName = nameO;

                // Tắt bảng nhập tên và gọi hàm random lượt đi ngay lập tức
                alertDialog.dismiss();
                determineFirstTurnRandomly();
            }
        });

        // Xóa bỏ khung nền xám vuông mặc định giúp bo góc tuyệt đẹp theo tệp XML custom
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        alertDialog.show();
    }

    private void determineFirstTurnRandomly() {
        Random random = new Random();
        int coin = random.nextInt(2);

        if (gameMode.equals("pvp")) {
            if (coin == 0) {
                currentStringPlayer = "X";
                tvTurnStatus.setText("Lượt của: " + playerXName + " (X)");
                showGameDialog("🎲", "Ngẫu nhiên:\n" + playerXName + " đi trước!");
            } else {
                currentStringPlayer = "O";
                tvTurnStatus.setText("Lượt của: " + playerOName + " (O)");
                showGameDialog("🎲", "Ngẫu nhiên:\n" + playerOName + " đi trước!");
            }
        } else {
            String diffText = "Dễ";
            if (difficulty.equals("medium")) diffText = "Bình thường";
            if (difficulty.equals("hard")) diffText = "Khó";

            if (coin == 0) {
                currentStringPlayer = "X";
                tvTurnStatus.setText("Đấu với Máy (" + diffText + ") - Lượt của bạn (X)");
                showGameDialog("🎲", "Ngẫu nhiên:\nBạn được đi trước!");
            } else {
                currentStringPlayer = "O";
                tvTurnStatus.setText("Máy đang suy nghĩ...");
                showGameDialog("🤖", "Ngẫu nhiên:\nMáy được đi trước!");
                botMakeMove();
            }
        }
    }

    private void botMakeMove() {
        if (!isGameActive) return;

        int[] botMove = gameEngine.getBotMove(difficulty, "O", "X");

        if (botMove != null) {
            int botRow = botMove[0];
            int botCol = botMove[1];

            gameEngine.makeMove(botRow, botCol, "O");
            buttons[botRow][botCol].setText("O");
            buttons[botRow][botCol].setTextColor(Color.BLUE);

            if (gameEngine.checkWin("O")) {
                tvTurnStatus.setText("MÁY ĐÃ CHIẾN THẮNG!");
                showGameDialog("😭", "Rất tiếc!\nMáy đã thắng bạn.");
                isGameActive = false;

                // Lưu trận thua vào lịch sử đám mây
                saveMatchHistory("Đấu với Máy", "Máy Thắng");
                return;
            }

            if (gameEngine.isBoardFull()) {
                tvTurnStatus.setText("TRẬN ĐẤU HÒA!");
                showGameDialog("🤝", "Kết quả hòa với Máy.");
                isGameActive = false;

                // Lưu trận hòa vào lịch sử đám mây
                saveMatchHistory("Đấu với Máy", "Hòa");
                return;
            }
        }

        currentStringPlayer = "X";
        tvTurnStatus.setText("Đến lượt bạn (X)");
    }

    private void onCellClicked(int row, int col) {
        if (!isGameActive || !gameEngine.isCellEmpty(row, col)) {
            return;
        }

        // ==========================================
        // LUỒNG 1: XỬ LÝ CHẾ ĐỘ ĐẤU VỚI NGƯỜI (PvP)
        // ==========================================
        if (gameMode.equals("pvp")) {
            gameEngine.makeMove(row, col, currentStringPlayer);
            buttons[row][col].setText(currentStringPlayer);

            if (currentStringPlayer.equals("X")) {
                buttons[row][col].setTextColor(Color.RED);
            } else {
                buttons[row][col].setTextColor(Color.BLUE);
            }

            if (gameEngine.checkWin(currentStringPlayer)) {
                String winnerName = currentStringPlayer.equals("X") ? playerXName : playerOName;
                tvTurnStatus.setText(winnerName.toUpperCase() + " CHIẾN THẮNG!");
                showGameDialog("👑", "Chúc mừng!\n" + winnerName + " đã chiến thắng!");
                isGameActive = false;

                // Lưu trận PvP thắng vào lịch sử đám mây
                saveMatchHistory("Đấu với Người", winnerName + " Thắng");
            } else if (gameEngine.isBoardFull()) {
                tvTurnStatus.setText("TRẬN ĐẤU HÒA!");
                showGameDialog("🤝", "Bàn cờ đã đầy!\nKết quả hòa.");
                isGameActive = false;

                // Lưu trận PvP hòa vào lịch sử đám mây
                saveMatchHistory("Đấu với Người", "Hòa");
            } else {
                currentStringPlayer = currentStringPlayer.equals("X") ? "O" : "X";
                String nextPlayerName = currentStringPlayer.equals("X") ? playerXName : playerOName;
                tvTurnStatus.setText("Lượt của: " + nextPlayerName + " (" + currentStringPlayer + ")");
            }
        }
        // ==========================================
        // LUỒNG 2: XỬ LÝ CHẾ ĐỘ ĐẤU VỚI MÁY (PvE)
        // ==========================================
        else {
            if (!currentStringPlayer.equals("X")) return;

            gameEngine.makeMove(row, col, "X");
            buttons[row][col].setText("X");
            buttons[row][col].setTextColor(Color.RED);

            if (gameEngine.checkWin("X")) {
                tvTurnStatus.setText("BẠN ĐÃ CHIẾN THẮNG MÁY!");
                showGameDialog("🎉", "Chúc mừng!\nBạn đã thắng Máy!");
                isGameActive = false;

                // Lưu trận thắng Máy vào lịch sử đám mây
                saveMatchHistory("Đấu với Máy", "Bạn Thắng");
                return;
            }

            if (gameEngine.isBoardFull()) {
                tvTurnStatus.setText("TRẬN ĐẤU HÒA!");
                showGameDialog("🤝", "Kết quả hòa với Máy.");
                isGameActive = false;

                // Lưu trận hòa Máy vào lịch sử đám mây
                saveMatchHistory("Đấu với Máy", "Hòa");
                return;
            }

            currentStringPlayer = "O";
            tvTurnStatus.setText("Máy đang suy nghĩ...");
            botMakeMove();
        }
    }

    // Hàm xử lý thu thập thông tin và đẩy dữ liệu trực tiếp lên Firebase Realtime Database
    private void saveMatchHistory(String mode, String matchResult) {
        String id = databaseReference.push().getKey();
        if (id == null) return;

        String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        long timestamp = System.currentTimeMillis();

        GameHistory history = new GameHistory(id, mode, matchResult, currentDate, currentTime, timestamp);
        databaseReference.child(id).setValue(history);
    }

    private void resetGame() {
        gameEngine.resetBoard();
        isGameActive = true;

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                buttons[i][j].setText("");
            }
        }
        determineFirstTurnRandomly();
    }

    // =========================================================================
    // Hàm khởi tạo Bảng thông báo Dialog Neon lớn giữa màn hình
    // =========================================================================
    private void showGameDialog(String icon, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.custom_dialog_layout, null);
        builder.setView(dialogView);
        builder.setCancelable(false); // Bắt buộc nhấn nút mới được tắt bảng

        // Ánh xạ các thành phần từ file custom_dialog_layout.xml
        TextView tvDialogIcon = dialogView.findViewById(R.id.tvDialogIcon);
        TextView tvDialogMessage = dialogView.findViewById(R.id.tvDialogMessage);
        Button btnDialogConfirm = dialogView.findViewById(R.id.btnDialogConfirm);

        // Nạp nội dung chữ và icon tương ứng vào bảng
        tvDialogIcon.setText(icon);
        tvDialogMessage.setText(message);

        final AlertDialog alertDialog = builder.create();

        // Xử lý sự kiện click vào nút Xác Nhận -> Đóng bảng thông báo
        btnDialogConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        // Làm trong suốt viền đen bọc ngoài mặc định để hiển thị bo góc mượt mà
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        alertDialog.show();
    }
}