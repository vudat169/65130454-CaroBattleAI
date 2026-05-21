package ck65130454.carobattle;

import android.content.Intent;
import android.graphics.Color; // Thêm thư viện màu sắc để đổi màu chữ X/O
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText; // THÊM MỚI: Thư viện ô nhập văn bản
import android.widget.LinearLayout; // THÊM MỚI: Thư viện tạo bố cục nhanh bằng code
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback; // THÊM MỚI: Thư viện xử lý nút Quay lại chuẩn Android mới
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Random; // Thư viện tạo số ngẫu nhiên

public class GameActivity extends AppCompatActivity {

    private CaroEngine gameEngine; // Đối tượng xử lý logic mảng và thuật toán thắng thua

    // ĐỔI MỚI: Đổi kiểu dữ liệu sang AppCompatButton để khớp với file XML ô vuông
    private androidx.appcompat.widget.AppCompatButton[][] buttons = new androidx.appcompat.widget.AppCompatButton[4][4];

    private TextView tvTurnStatus;
    private Button btnReset;
    private Button btnBack; // Biến điều khiển nút Trở về

    private String currentStringPlayer = "X"; // Đại diện người chơi hiện tại ("X" hoặc "O")
    private boolean isGameActive = true; // Trạng thái trận đấu (true: đang chơi, false: đã dừng)
    private String gameMode = "pvp"; // Chế độ chơi mặc định ("pvp" hoặc "pve")
    private String difficulty = "easy"; // Mức độ khó của Bot ("easy", "medium", "hard")

    // THÊM MỚI: Biến toàn cục lưu tên tự đặt của 2 người chơi
    private String playerXName = "Người chơi X";
    private String playerOName = "Người chơi O";

    // THÊM MỚI: Biến kiểm soát trạng thái chuyển đổi màn hình để tránh tắt nhạc nhầm
    private boolean isChangingActivity = false;

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

        // 2. Nhận dữ liệu chế độ chơi và ĐỘ KHÓ từ MainActivity truyền sang
        if (getIntent() != null) {
            if (getIntent().hasExtra("game_mode")) {
                gameMode = getIntent().getStringExtra("game_mode");
            }
            if (getIntent().hasExtra("difficulty")) {
                difficulty = getIntent().getStringExtra("difficulty");
            }
        }

        // 3. Ánh xạ các thành phần hiển thị trạng thái và nút chức năng
        tvTurnStatus = findViewById(R.id.tvTurnStatus);
        btnReset = findViewById(R.id.btnReset);
        btnBack = findViewById(R.id.btnBack); // Ánh xạ nút Trở về từ XML

        // 4. Sử dụng vòng lặp tự động ánh xạ và lắng nghe sự kiện cho 16 nút bấm dựa vào tọa độ ID
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

        // 5. ĐÃ SỬA: Nếu là PvP thì hiện hộp thoại điền tên trước, PvE thì tiến hành random lượt ngay
        if (gameMode.equals("pvp")) {
            showNameInputDialog();
        } else {
            determineFirstTurnRandomly();
        }

        // 6. Thiết lập sự kiện cho nút "Chơi lại"
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetGame();
            }
        });

        // Thiết lập sự kiện cho nút "Trở về" trên giao diện màn hình
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isChangingActivity = true; // Đánh dấu chủ động chuyển màn hình về MainActivity
                finish();
            }
        });

        // ĐÃ CẬP NHẬT: Xử lý nút quay lại hệ thống (phím cứng hoặc cử chỉ Back vuốt cạnh màn hình của điện thoại)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                isChangingActivity = true; // Đánh dấu chủ động chuyển màn hình để giữ nhạc nền
                finish(); // Đóng màn hình chơi game để quay lại màn hình chính
            }
        });
    }

    // Tạm dừng nhạc nếu người dùng bấm nút Home thoát hẳn ứng dụng ra ngoài
    @Override
    protected void onPause() {
        super.onPause();
        // Chỉ tạm dừng nhạc từ MainActivity truyền sang nếu không phải hành động chuyển đổi màn hình trong app
        if (!isChangingActivity && MainActivity.bgMediaPlayer != null && MainActivity.bgMediaPlayer.isPlaying()) {
            MainActivity.bgMediaPlayer.pause();
        }
    }

    // Tiếp tục phát nhạc khi người chơi quay lại màn chơi
    @Override
    protected void onResume() {
        super.onResume();
        isChangingActivity = false; // Đặt lại trạng thái ban đầu khi màn hình hoạt động
        if (MainActivity.bgMediaPlayer != null && !MainActivity.bgMediaPlayer.isPlaying()) {
            MainActivity.bgMediaPlayer.start();
        }
    }

    // Hàm hiển thị hộp thoại pop-up cho phép nhập tên người chơi nhanh bằng code
    private void showNameInputDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Nhập tên người chơi");
        builder.setCancelable(false); // Bắt buộc nhập hoặc ấn bắt đầu, không cho bấm ra ngoài màn hình để tắt

        // Tạo layout dọc xếp chồng 2 ô nhập văn bản
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

        // Xử lý sự kiện khi ấn nút "Bắt đầu" trên hộp thoại
        builder.setPositiveButton("Bắt đầu", (dialog, which) -> {
            String nameX = inputX.getText().toString().trim();
            String nameO = inputO.getText().toString().trim();

            // Nếu người dùng có nhập thì lấy tên đó + hậu tố quân cờ, nếu để trống thì dùng tên mặc định
            if (!nameX.isEmpty()) playerXName = nameX + " (X)";
            else playerXName = "Người chơi X";

            if (!nameO.isEmpty()) playerOName = nameO + " (O)";
            else playerOName = "Người chơi O";

            // Sau khi ghi nhận tên xong mới bắt đầu tung xúc xắc chọn lượt đi
            determineFirstTurnRandomly();
        });

        builder.show();
    }

    // Hàm quyết định ngẫu nhiên ai là người đi trước (0: X, 1: O)
    private void determineFirstTurnRandomly() {
        Random random = new Random();
        int coin = random.nextInt(2); // Trả về ngẫu nhiên số 0 hoặc 1

        if (gameMode.equals("pvp")) {
            // Chế độ chơi 2 người đấu với nhau (Đã cập nhật gọi theo Tên tự đặt)
            if (coin == 0) {
                currentStringPlayer = "X";
                tvTurnStatus.setText("Lượt của: " + playerXName);
                Toast.makeText(this, "Ngẫu nhiên: " + playerXName + " đi trước!", Toast.LENGTH_SHORT).show();
            } else {
                currentStringPlayer = "O";
                tvTurnStatus.setText("Lượt của: " + playerOName);
                Toast.makeText(this, "Ngẫu nhiên: " + playerOName + " đi trước!", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Chế độ chơi với Máy (Người luôn là X, Máy luôn là O)
            String diffText = "Dễ";
            if (difficulty.equals("medium")) diffText = "Bình thường";
            if (difficulty.equals("hard")) diffText = "Khó";

            if (coin == 0) {
                currentStringPlayer = "X"; // Người đi trước
                tvTurnStatus.setText("Đấu với Máy (" + diffText + ") - Lượt của bạn (X)");
                Toast.makeText(this, "Ngẫu nhiên: Bạn được đi trước!", Toast.LENGTH_SHORT).show();
            } else {
                currentStringPlayer = "O"; // Máy đi trước
                tvTurnStatus.setText("Máy đang suy nghĩ...");
                Toast.makeText(this, "Ngẫu nhiên: Máy được đi trước!", Toast.LENGTH_SHORT).show();

                // Ép Máy tự động hạ quân cờ đầu tiên mà không đợi người chơi nhấn
                botMakeMove();
            }
        }
    }

    // Hàm xử lý nước đi của Máy để tái sử dụng
    private void botMakeMove() {
        if (!isGameActive) return;

        int[] botMove = gameEngine.getBotMove(difficulty, "O", "X");

        if (botMove != null) {
            int botRow = botMove[0];
            int botCol = botMove[1];

            // Cập nhật mảng và hiển thị ký tự O màu Xanh dương
            gameEngine.makeMove(botRow, botCol, "O");
            buttons[botRow][botCol].setText("O");
            buttons[botRow][botCol].setTextColor(Color.BLUE);

            // Kiểm tra trạng thái thắng/hòa cho Máy
            if (gameEngine.checkWin("O")) {
                tvTurnStatus.setText("MÁY ĐÃ CHIẾN THẮNG!");
                Toast.makeText(this, "Rất tiếc! Máy đã thắng bạn.", Toast.LENGTH_LONG).show();
                isGameActive = false;
                return;
            }

            if (gameEngine.isBoardFull()) {
                tvTurnStatus.setText("TRẬN ĐẤU HÒA!");
                Toast.makeText(this, "Kết quả hòa với Máy.", Toast.LENGTH_LONG).show();
                isGameActive = false;
                return;
            }
        }

        // Sau khi máy đi xong thì chuyển quyền lại cho Người chơi (X)
        currentStringPlayer = "X";
        tvTurnStatus.setText("Đến lượt bạn (X)");
    }

    // Phương thức xử lý khi một ô cờ tại vị trí (row, col) được nhấn chọn
    private void onCellClicked(int row, int col) {
        if (!isGameActive || !gameEngine.isCellEmpty(row, col)) {
            return;
        }

        // ==========================================
        // LUỒNG 1: XỬ LÝ CHẾ ĐỘ ĐẦU VỚI NGƯỜI (PvP)
        // ==========================================
        if (gameMode.equals("pvp")) {
            gameEngine.makeMove(row, col, currentStringPlayer);
            buttons[row][col].setText(currentStringPlayer);

            if (currentStringPlayer.equals("X")) {
                buttons[row][col].setTextColor(Color.RED);
            } else {
                buttons[row][col].setTextColor(Color.BLUE);
            }

            // Thay đổi text hiển thị thắng cuộc và đổi lượt dựa trên Tên thật vừa nhập
            if (gameEngine.checkWin(currentStringPlayer)) {
                String winnerName = currentStringPlayer.equals("X") ? playerXName : playerOName;
                tvTurnStatus.setText(winnerName.toUpperCase() + " CHIẾN THẮNG!");
                Toast.makeText(this, "Chúc mừng " + winnerName + "!", Toast.LENGTH_LONG).show();
                isGameActive = false;
            } else if (gameEngine.isBoardFull()) {
                tvTurnStatus.setText("TRẬN ĐẤU HÒA!");
                Toast.makeText(this, "Bàn cờ đã đầy! Kết quả hòa.", Toast.LENGTH_LONG).show();
                isGameActive = false;
            } else {
                currentStringPlayer = currentStringPlayer.equals("X") ? "O" : "X";
                String nextPlayerName = currentStringPlayer.equals("X") ? playerXName : playerOName;
                tvTurnStatus.setText("Lượt của: " + nextPlayerName);
            }
        }
        // ==========================================
        // LUỒNG 2: XỬ LÝ CHẾ ĐỘ ĐẦU VỚI MÁY (PvE)
        // ==========================================
        else {
            // Chặn không cho người dùng bấm lung tung khi đang là lượt của Máy (O)
            if (!currentStringPlayer.equals("X")) return;

            // LƯỢT CỦA NGƯỜI (Luôn đóng vai quân X)
            gameEngine.makeMove(row, col, "X");
            buttons[row][col].setText("X");
            buttons[row][col].setTextColor(Color.RED);

            if (gameEngine.checkWin("X")) {
                tvTurnStatus.setText("BẠN ĐÃ CHIẾN THẮNG MÁY!");
                Toast.makeText(this, "Chúc mừng bạn đã thắng Máy!", Toast.LENGTH_LONG).show();
                isGameActive = false;
                return;
            }

            if (gameEngine.isBoardFull()) {
                tvTurnStatus.setText("TRẬN ĐẤU HÒA!");
                Toast.makeText(this, "Kết quả hòa với Máy.", Toast.LENGTH_LONG).show();
                isGameActive = false;
                return;
            }

            // Chuyển sang lượt Máy
            currentStringPlayer = "O";
            tvTurnStatus.setText("Máy đang suy nghĩ...");

            // Kích hoạt máy tự động tính toán hạ quân O liền ngay sau đó
            botMakeMove();
        }
    }

    // Phương thức đưa các thông số trận đấu và giao diện về trạng thái xuất phát
    private void resetGame() {
        gameEngine.resetBoard(); // Reset dữ liệu mảng ngầm
        isGameActive = true;

        // Quét qua mảng và làm sạch toàn bộ văn bản hiển thị trên các nút bấm bàn cờ
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                buttons[i][j].setText("");
            }
        }

        // Khi bấm "Chơi lại", giữ nguyên tên cũ đã đặt và chỉ tiến hành tung xúc xắc chia lại lượt đi đầu tiên
        determineFirstTurnRandomly();
    }
}