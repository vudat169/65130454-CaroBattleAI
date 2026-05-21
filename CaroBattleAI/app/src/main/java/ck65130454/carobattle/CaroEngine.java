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

    // =================================================================
    // PHẦN THÊM MỚI: THUẬT TOÁN PHÂN TÍCH NƯỚC ĐI CỦA MÁY (BOT AI)
    // =================================================================

    // Hàm chính để điều hướng tìm nước đi dựa trên độ khó, trả về mảng [dòng, cột]
    public int[] getBotMove(String difficulty, String botPlayer, String humanPlayer) {
        if (difficulty.equals("hard")) {
            return getHardMove(botPlayer, humanPlayer);
        } else if (difficulty.equals("medium")) {
            return getMediumMove(botPlayer, humanPlayer);
        } else {
            return getRandomMove(); // Mặc định chế độ Dễ
        }
    }

    // 1. Cấp độ Dễ: Nhắm mắt đánh bừa vào bất kỳ ô nào còn trống
    private int[] getRandomMove() {
        java.util.ArrayList<int[]> emptyCells = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j].equals("")) {
                    emptyCells.add(new int[]{i, j});
                }
            }
        }
        if (!emptyCells.isEmpty()) {
            int randomIndex = new java.util.Random().nextInt(emptyCells.size());
            return emptyCells.get(randomIndex);
        }
        return null;
    }

    // 2. Cấp độ Bình thường: Biết đi chặn nếu Người chơi chuẩn bị ăn 4 ô liên tiếp
    private int[] getMediumMove(String botPlayer, String humanPlayer) {
        // Tìm nước để CHẶN đường thắng sắp tới của Con người
        int[] blockMove = findWinningMove(humanPlayer);
        if (blockMove != null) return blockMove;

        // Nếu không có gì nguy hiểm thì quay lại đánh ngẫu nhiên
        return getRandomMove();
    }

    // 3. Cấp độ Khó: Ưu tiên tự thắng > Chặn người chơi > Chiếm tâm bàn cờ > Random
    private int[] getHardMove(String botPlayer, String humanPlayer) {
        // Ưu tiên 1: Tự tìm nước đi giúp MÁY THẮNG LUÔN trong lượt này
        int[] winMove = findWinningMove(botPlayer);
        if (winMove != null) return winMove;

        // Ưu tiên 2: CHẶN đứng đường thắng chí mạng của NGƯỜI CHƠI
        int[] blockMove = findWinningMove(humanPlayer);
        if (blockMove != null) return blockMove;

        // Ưu tiên 3: Ưu tiên chiếm giữ 4 ô chiến lược ở vùng trung tâm (1,1), (1,2), (2,1), (2,2)
        int[][] centerCells = {{1, 1}, {1, 2}, {2, 1}, {2, 2}};
        java.util.ArrayList<int[]> emptyCenters = new java.util.ArrayList<>();
        for (int[] cell : centerCells) {
            if (board[cell[0]][cell[1]].equals("")) {
                emptyCenters.add(cell);
            }
        }
        if (!emptyCenters.isEmpty()) {
            int randomIndex = new java.util.Random().nextInt(emptyCenters.size());
            return emptyCenters.get(randomIndex);
        }

        // Ưu tiên 4: Đánh bừa vào các ô rìa còn lại
        return getRandomMove();
    }

    // Hàm bổ trợ: Giả lập đánh thử một ô xem người chơi đó có chiến thắng lập tức không
    private int[] findWinningMove(String player) {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j].equals("")) {
                    // Đánh thử ngầm vào bộ nhớ tạm
                    board[i][j] = player;
                    // Kiểm tra xem nước này có kích hoạt điều kiện thắng không
                    boolean isWin = checkWin(player);
                    // Reset trả lại trạng thái trống sau khi tính toán xong
                    board[i][j] = "";

                    if (isWin) {
                        return new int[]{i, j}; // Trả về tọa độ điểm vàng này
                    }
                }
            }
        }
        return null;
    }
}