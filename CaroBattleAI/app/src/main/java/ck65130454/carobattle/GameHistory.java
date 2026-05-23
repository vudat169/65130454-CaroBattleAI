package ck65130454.carobattle;

public class GameHistory {
    public String id;
    public String gameMode;
    public String result;
    public String date;
    public String time;
    public long timestamp;

    // Hàm khởi tạo trống bắt buộc phải có cho Firebase
    public GameHistory() {
    }

    // Hàm khởi tạo đầy đủ tham số
    public GameHistory(String id, String gameMode, String result, String date, String time, long timestamp) {
        this.id = id;
        this.gameMode = gameMode;
        this.result = result;
        this.date = date;
        this.time = time;
        this.timestamp = timestamp;
    }
}