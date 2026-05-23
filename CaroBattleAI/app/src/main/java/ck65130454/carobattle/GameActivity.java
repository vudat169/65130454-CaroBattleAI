package ck65130454.carobattle;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// THÊM MỚI: Thư viện kết nối Firebase Realtime Database để ghi lịch sử
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

// THÊM MỚI: Thư viện định dạng Ngày/Giờ hệ thống
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

    // THÊM MỚI: Biến kết nối tới cơ sở dữ liệu Firebase
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

        // THÊM MỚI: Khởi tạo kết nối tới node "History" trên Firebase
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

    private void showNameInputDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Nhập tên người chơi");
        builder.setCancelable(false);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 20);

        final EditText inputX = new EditText(this);
        inputX.setHint("Tên người chơi X (Ví dụ: Minh)");
        layout.addView(inputX);

        final EditText inputO = new EditText(this);
        inputO.setHint("Tên người chơi O (Ví dụ: Hoàng)");
        layout.addView(inputO);

        builder.setView(layout);

        builder.setPositiveButton("Bắt đầu", (dialog, which) -> {
            String nameX = inputX.getText().toString().trim();
            String nameO = inputO.getText().toString().trim();

            if (!nameX.isEmpty()) playerXName = nameX;
            else playerXName = "Người chơi X";

            if (!nameO.isEmpty()) playerOName = nameO;
            else playerOName = "Người chơi O";

            determineFirstTurnRandomly();
        });

        builder.show();
    }

    private void determineFirstTurnRandomly() {
        Random random = new Random();
        int coin = random.nextInt(2);

        if (gameMode.equals("pvp")) {
            if (coin == 0) {
                currentStringPlayer = "X";
                tvTurnStatus.setText("Lượt của: " + playerXName + " (X)");
                Toast.makeText(this, "Ngẫu nhiên: " + playerXName + " đi trước!", Toast.LENGTH_SHORT).show();
            } else {
                currentStringPlayer = "O";
                tvTurnStatus.setText("Lượt của: " + playerOName + " (O)");
                Toast.makeText(this, "Ngẫu nhiên: " + playerOName + " đi trước!", Toast.LENGTH_SHORT).show();
            }
        } else {
            String diffText = "Dễ";
            if (difficulty.equals("medium")) diffText = "Bình thường";
            if (difficulty.equals("hard")) diffText = "Khó";

            if (coin == 0) {
                currentStringPlayer = "X";
                tvTurnStatus.setText("Đấu với Máy (" + diffText + ") - Lượt của bạn (X)");
                Toast.makeText(this, "Ngẫu nhiên: Bạn được đi trước!", Toast.LENGTH_SHORT).show();
            } else {
                currentStringPlayer = "O";
                tvTurnStatus.setText("Máy đang suy nghĩ...");
                Toast.makeText(this, "Ngẫu nhiên: Máy được đi trước!", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Rất tiếc! Máy đã thắng bạn.", Toast.LENGTH_LONG).show();
                isGameActive = false;

                // THÊM MỚI: Lưu trận thua vào lịch sử đám mây
                saveMatchHistory("Đấu với Máy", "Máy Thắng");
                return;
            }

            if (gameEngine.isBoardFull()) {
                tvTurnStatus.setText("TRẬN ĐẤU HÒA!");
                Toast.makeText(this, "Kết quả hòa với Máy.", Toast.LENGTH_LONG).show();
                isGameActive = false;

                // THÊM MỚI: Lưu trận hòa vào lịch sử đám mây
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
                Toast.makeText(this, "Chúc mừng " + winnerName + "!", Toast.LENGTH_LONG).show();
                isGameActive = false;

                // THÊM MỚI: Lưu trận PvP thắng vào lịch sử đám mây
                saveMatchHistory("Đấu với Người", winnerName + " Thắng");
            } else if (gameEngine.isBoardFull()) {
                tvTurnStatus.setText("TRẬN ĐẤU HÒA!");
                Toast.makeText(this, "Bàn cờ đã đầy! Kết quả hòa.", Toast.LENGTH_LONG).show();
                isGameActive = false;

                // THÊM MỚI: Lưu trận PvP hòa vào lịch sử đám mây
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
                Toast.makeText(this, "Chúc mừng bạn đã thắng Máy!", Toast.LENGTH_LONG).show();
                isGameActive = false;

                // THÊM MỚI: Lưu trận thắng Máy vào lịch sử đám mây
                saveMatchHistory("Đấu với Máy", "Bạn Thắng");
                return;
            }

            if (gameEngine.isBoardFull()) {
                tvTurnStatus.setText("TRẬN ĐẤU HÒA!");
                Toast.makeText(this, "Kết quả hòa với Máy.", Toast.LENGTH_LONG).show();
                isGameActive = false;

                // THÊM MỚI: Lưu trận hòa Máy vào lịch sử đám mây
                saveMatchHistory("Đấu với Máy", "Hòa");
                return;
            }

            currentStringPlayer = "O";
            tvTurnStatus.setText("Máy đang suy nghĩ...");
            botMakeMove();
        }
    }

    // THÊM MỚI: Hàm xử lý thu thập thông tin và đẩy dữ liệu trực tiếp lên Firebase Realtime Database
    private void saveMatchHistory(String mode, String matchResult) {
        // 1. Tạo id ngẫu nhiên duy nhất cho mỗi trận đấu thông qua Firebase push key
        String id = databaseReference.push().getKey();
        if (id == null) return;

        // 2. Định dạng lấy chuỗi Ngày và chuỗi Giờ thực tế từ điện thoại người dùng
        String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        long timestamp = System.currentTimeMillis(); // Dùng để thuật toán sắp xếp thứ tự trận mới lên đầu

        // 3. Đóng gói thông tin trận đấu vào object cấu trúc dữ liệu GameHistory mà bạn vừa tạo thành công
        GameHistory history = new GameHistory(id, mode, matchResult, currentDate, currentTime, timestamp);

        // 4. Đẩy thẳng dữ liệu lên nhánh Firebase đám mây
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
}