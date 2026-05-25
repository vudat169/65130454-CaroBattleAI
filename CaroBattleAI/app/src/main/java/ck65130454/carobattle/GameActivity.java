package ck65130454.carobattle;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable; // Xử lý làm trong suốt viền dialog mặc định
import android.os.Bundle;
import android.os.CountDownTimer; // Thư viện đếm ngược thời gian
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar; // Quản lý thanh thời gian
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private CaroEngine gameEngine;
    private androidx.appcompat.widget.AppCompatButton[][] buttons = new androidx.appcompat.widget.AppCompatButton[4][4];

    private TextView tvTurnStatus;
    private Button btnReset;
    private Button btnBack;

    // Quản lý luồng thời gian
    private ProgressBar pbTimer;
    private TextView tvTimerCountdown;
    private CountDownTimer countDownTimer;

    private String currentStringPlayer = "X";
    private boolean isGameActive = true;
    private String gameMode = "pvp";
    private String difficulty = "easy";

    private String playerXName = "Người chơi X";
    private String playerOName = "Người chơi O";
    private boolean isChangingActivity = false;

    private boolean isNameInputDone = false;

    // ĐÃ THÊM: Biến kiểm tra xem người chơi đã bấm "Xác nhận" đóng Dialog tung xúc xắc đầu trận chưa
    private boolean isFirstTurnDialogOpen = false;

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

        // Ánh xạ các thành phần đếm ngược thời gian
        pbTimer = findViewById(R.id.pbTimer);
        tvTimerCountdown = findViewById(R.id.tvTimerCountdown);

        // Đặt thanh thời gian và số giây ở trạng thái tĩnh ban đầu
        pbTimer.setMax(30);
        pbTimer.setProgress(30);
        tvTimerCountdown.setText("30s");
        tvTimerCountdown.setTextColor(Color.parseColor("#FFCC00")); // Màu vàng chờ mặc định

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
            isNameInputDone = false;
            showNameInputDialog();
        } else {
            isNameInputDone = true;
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
                cancelTimer();
                isChangingActivity = true;
                finish();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cancelTimer();
                isChangingActivity = true;
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        cancelTimer();
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

        // Chỉ chạy lại thời gian nếu game đang chạy, đã nhập tên xong và KHÔNG bị kẹt ở Dialog mở đầu
        if (isGameActive && isNameInputDone && !isFirstTurnDialogOpen) {
            if (gameMode.equals("pvp") || currentStringPlayer.equals("X")) {
                startTurnTimer();
            }
        }
    }

    // =========================================================================
    // Điều khiển đếm ngược 30 giây & đồng bộ đổi màu chữ/thanh đo
    // =========================================================================
    private void startTurnTimer() {
        cancelTimer();

        // Nếu game kết thúc, chưa nhập tên, hoặc đang hiện Dialog mở màn -> CHẶN không cho đếm giờ
        if (!isGameActive || !isNameInputDone || isFirstTurnDialogOpen) return;

        if (currentStringPlayer.equals("X")) {
            pbTimer.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EB0F0F")));
            tvTimerCountdown.setTextColor(Color.parseColor("#EB0F0F"));
        } else {
            pbTimer.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1A65E4")));
            tvTimerCountdown.setTextColor(Color.parseColor("#1A65E4"));
        }

        pbTimer.setMax(30);
        pbTimer.setProgress(30);
        tvTimerCountdown.setText("30s");

        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000);
                pbTimer.setProgress(secondsLeft);
                tvTimerCountdown.setText(secondsLeft + "s");
            }

            @Override
            public void onFinish() {
                pbTimer.setProgress(0);
                tvTimerCountdown.setText("0s");

                if (isGameActive) {
                    makeRandomMoveForPlayer();
                }
            }
        }.start();
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    // Thuật toán quét ô trống và hạ bài tự động khi hết thời gian
    private void makeRandomMoveForPlayer() {
        List<int[]> emptyCells = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (gameEngine.isCellEmpty(i, j)) {
                    emptyCells.add(new int[]{i, j});
                }
            }
        }

        if (!emptyCells.isEmpty()) {
            Random random = new Random();
            int[] luckyCell = emptyCells.get(random.nextInt(emptyCells.size()));
            int row = luckyCell[0];
            int col = luckyCell[1];

            onCellClicked(row, col);
        }
    }

    private void showNameInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.dialog_name_input_layout, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        final AlertDialog alertDialog = builder.create();

        final EditText etPlayerX = dialogView.findViewById(R.id.etPlayerX);
        final EditText etPlayerO = dialogView.findViewById(R.id.etPlayerO);
        final TextView tvErrorMessage = dialogView.findViewById(R.id.tvErrorMessage);
        Button btnStartGame = dialogView.findViewById(R.id.btnStartGame);

        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nameX = etPlayerX.getText().toString().trim();
                String nameO = etPlayerO.getText().toString().trim();

                if (nameX.isEmpty() || nameO.isEmpty()) {
                    tvErrorMessage.setVisibility(View.VISIBLE);
                    if (nameX.isEmpty() && nameO.isEmpty()) {
                        tvErrorMessage.setText("⚠️ CẢ HAI NGƯỜI CHƠI CHƯA GHI TÊN !");
                    } else if (nameX.isEmpty()) {
                        tvErrorMessage.setText("⚠️ X CHƯA KHAI BÁO DANH TÍNH !");
                    } else {
                        tvErrorMessage.setText("⚠️ O CHƯA KHAI BÁO DANH TÍNH !");
                    }
                    return;
                }

                playerXName = nameX;
                playerOName = nameO;
                isNameInputDone = true;

                alertDialog.dismiss();
                determineFirstTurnRandomly();
            }
        });

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        alertDialog.show();
    }

    // =========================================================================
    // ĐÃ SỬA: Cả PvP và PvE khi tung đồng xu xong đều bị Khóa thời gian chờ bấm Xác nhận
    // =========================================================================
    private void determineFirstTurnRandomly() {
        Random random = new Random();
        int coin = random.nextInt(2);

        // Đánh dấu là đang mở Dialog bốc thăm khởi đầu -> Chặn đứng thời gian chạy ngầm
        isFirstTurnDialogOpen = true;

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
                cancelTimer();
                showGameDialog("😭", "Rất tiếc!\nMáy đã thắng bạn.");
                isGameActive = false;
                saveMatchHistory("Đấu với Máy", "Máy Thắng");
                return;
            }

            if (gameEngine.isBoardFull()) {
                tvTurnStatus.setText("TRẬN ĐẤU HÒA!");
                cancelTimer();
                showGameDialog("🤝", "Kết quả hòa với Máy.");
                isGameActive = false;
                saveMatchHistory("Đấu với Máy", "Hòa");
                return;
            }
        }

        currentStringPlayer = "X";
        tvTurnStatus.setText("Đến lượt bạn (X)");
        startTurnTimer(); // Máy đi xong -> Trả quyền cho người và kích hoạt đếm ngược 30s
    }

    private void onCellClicked(int row, int col) {
        if (!isGameActive || !gameEngine.isCellEmpty(row, col)) {
            return;
        }

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
                cancelTimer();
                showGameDialog("👑", "Chúc mừng!\n" + winnerName + " đã chiến thắng!");
                isGameActive = false;
                saveMatchHistory("Đấu với Người", winnerName + " Thắng");
            } else if (gameEngine.isBoardFull()) {
                tvTurnStatus.setText("TRẬN ĐẤU HÒA!");
                cancelTimer();
                showGameDialog("🤝", "Bàn cờ đã đầy!\nKết quả hòa.");
                isGameActive = false;
                saveMatchHistory("Đấu với Người", "Hòa");
            } else {
                currentStringPlayer = currentStringPlayer.equals("X") ? "O" : "X";
                String nextPlayerName = currentStringPlayer.equals("X") ? playerXName : playerOName;
                tvTurnStatus.setText("Lượt của: " + nextPlayerName + " (" + currentStringPlayer + ")");
                startTurnTimer();
            }
        } else {
            if (!currentStringPlayer.equals("X")) return;

            gameEngine.makeMove(row, col, "X");
            buttons[row][col].setText("X");
            buttons[row][col].setTextColor(Color.RED);

            if (gameEngine.checkWin("X")) {
                tvTurnStatus.setText("BẠN ĐÃ CHIẾN THẮNG MÁY!");
                cancelTimer();
                showGameDialog("🎉", "Chúc mừng!\nBạn đã thắng Máy!");
                isGameActive = false;
                saveMatchHistory("Đấu với Máy", "Bạn Thắng");
                return;
            }

            if (gameEngine.isBoardFull()) {
                tvTurnStatus.setText("TRẬN ĐẤU HÒA!");
                cancelTimer();
                showGameDialog("🤝", "Kết quả hòa với Máy.");
                isGameActive = false;
                saveMatchHistory("Đấu với Máy", "Hòa");
                return;
            }

            currentStringPlayer = "O";
            tvTurnStatus.setText("Máy đang suy nghĩ...");
            cancelTimer(); // Tắt tạm thời trong lúc Máy tính toán
            botMakeMove();
        }
    }

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
        cancelTimer();
        gameEngine.resetBoard();
        isGameActive = true;

        // Trả thanh đo thời gian về tĩnh lặng ban đầu trước khi gieo xúc xắc mới
        pbTimer.setMax(30);
        pbTimer.setProgress(30);
        tvTimerCountdown.setText("30s");
        tvTimerCountdown.setTextColor(Color.parseColor("#FFCC00"));

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                buttons[i][j].setText("");
            }
        }
        determineFirstTurnRandomly();
    }

    private void showGameDialog(String icon, String message) {
        cancelTimer();

        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.custom_dialog_layout, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        TextView tvDialogIcon = dialogView.findViewById(R.id.tvDialogIcon);
        TextView tvDialogMessage = dialogView.findViewById(R.id.tvDialogMessage);
        Button btnDialogConfirm = dialogView.findViewById(R.id.btnDialogConfirm);

        tvDialogIcon.setText(icon);
        tvDialogMessage.setText(message);

        final AlertDialog alertDialog = builder.create();

        btnDialogConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();

                // ĐÁ SỬA: Người chơi bấm nút đóng Dialog mở màn thành công -> Mở khóa cấm dòng chảy thời gian
                if (isFirstTurnDialogOpen) {
                    isFirstTurnDialogOpen = false;

                    // Nếu là chế độ đấu Máy và KẾT QUẢ gieo đồng xu là Máy đi trước -> Ra lệnh cho Máy đánh luôn
                    if (gameMode.equals("pve") && currentStringPlayer.equals("O")) {
                        botMakeMove();
                        return; // Chặn lại không chạy timer của người chơi
                    }
                }

                // Luồng kích hoạt tính giờ thông thường (Dành cho PvP hoặc khi Người chơi được đánh PvE)
                if (isGameActive && isNameInputDone && (gameMode.equals("pvp") || currentStringPlayer.equals("X"))) {
                    startTurnTimer();
                }
            }
        });

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        alertDialog.show();
    }
}