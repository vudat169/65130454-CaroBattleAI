package ck65130454.carobattle;

public class CaroEngine {
    private String[][] board; // Mảng 2 chiều 4x4 đại diện cho bàn cờ trên bộ nhớ
    private int size = 4;

    public CaroEngine() {
        board = new String[size][size];
        resetBoard();
    }

    // Hàm xóa sạch bàn cờ về trạng thái trống ban đầu
    public void resetBoard() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                board[i][j] = ""; // Ký tự rỗng nghĩa là chưa ai đánh
            }
        }
    }

    // Hàm cập nhật nước đi của người chơi vào mảng logic
    public void makeMove(int row, int col, String player) {
        if (row >= 0 && row < size && col >= 0 && col < size) {
            board[row][col] = player; // Ghi nhận "X" hoặc "O"
        }
    }

    // Hàm kiểm tra xem ô đó đã có ai đánh chưa
    public boolean isCellEmpty(int row, int col) {
        return board[row][col].equals("");
    }

    // Hàm kiểm tra xem bàn cờ đã kín chỗ (Hòa) chưa
    public boolean isBoardFull() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j].equals("")) {
                    return false; // Vẫn còn ít nhất 1 ô trống
                }
            }
        }
        return true; // Hết ô trống
    }

    // THUẬT TOÁN KIỂM TRA THẮNG THUA (OPTION B: ĐỦ 4 Ô LIÊN TIẾP)
    public boolean checkWin(String player) {
        // 1. Kiểm tra tất cả các hàng ngang
        for (int i = 0; i < size; i++) {
            if (board[i][0].equals(player) && board[i][1].equals(player) &&
                    board[i][2].equals(player) && board[i][3].equals(player)) {
                return true;
            }
        }

        // 2. Kiểm tra tất cả các hàng dọc
        for (int j = 0; j < size; j++) {
            if (board[0][j].equals(player) && board[1][j].equals(player) &&
                    board[2][j].equals(player) && board[3][j].equals(player)) {
                return true;
            }
        }

        // 3. Kiểm tra đường chéo chính (Từ góc trên trái xuống góc dưới phải)
        if (board[0][0].equals(player) && board[1][1].equals(player) &&
                board[2][2].equals(player) && board[3][3].equals(player)) {
            return true;
        }

        // 4. Kiểm tra đường chéo phụ (Từ góc trên phải xuống góc dưới trái)
        if (board[0][3].equals(player) && board[1][2].equals(player) &&
                board[2][1].equals(player) && board[3][0].equals(player)) {
            return true;
        }

        return false; // Chưa ai đạt đủ 4 ô liên tiếp
    }

    // Hàm lấy mảng dữ liệu (Sẽ dùng cho Bot AI ở các bước sau)
    public String[][] getBoard() {
        return board;
    }
}