package ck65130454.carobattle;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

public class HistoryActivity extends AppCompatActivity {

    private ListView lvHistory;
    private Button btnBackToMain;

    private DatabaseReference databaseReference;
    private ArrayList<GameHistory> historyList;
    private ArrayAdapter<GameHistory> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        // Đoạn xử lý căn lề hệ thống chống tràn viền giữ nguyên của bạn
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Ánh xạ các thành phần từ giao diện activity_history.xml sang Java
        lvHistory = findViewById(R.id.lvHistory);
        btnBackToMain = findViewById(R.id.btnBackToMain);

        // 2. Khởi tạo danh sách và kết nối đến nhánh dữ liệu "History" trên Firebase
        historyList = new ArrayList<>();
        databaseReference = FirebaseDatabase.getInstance().getReference("History");

        // 3. Khởi tạo Adapter tùy biến để hiển thị 2 dòng thông tin (Chế độ + Kết quả & Ngày + Giờ)
        adapter = new ArrayAdapter<GameHistory>(this, android.R.layout.simple_list_item_2, historyList) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                // Tái sử dụng lại layout mặc định gồm 2 dòng chữ xếp chồng của Android (simple_list_item_2)
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
                }

                GameHistory currentItem = getItem(position);

                TextView text1 = convertView.findViewById(android.R.id.text1);
                TextView text2 = convertView.findViewById(android.R.id.text2);

                if (currentItem != null) {
                    // Dòng 1: Hiển thị Chế độ chơi và Kết quả trận đấu
                    text1.setText("🎮 " + currentItem.gameMode + " ➜ " + currentItem.result);
                    text1.setTextSize(16);
                    text1.setTextColor(android.graphics.Color.parseColor("#333333"));

                    // Dòng 2: Hiển thị thông tin Ngày tháng và Thời gian diễn ra trận đấu
                    text2.setText("📅 " + currentItem.date + "   ⏱ " + currentItem.time);
                    text2.setTextSize(13);
                    text2.setTextColor(android.graphics.Color.GRAY);
                }

                return convertView;
            }
        };

        // Gắn adapter vào ListView để sẵn sàng hiển thị dữ liệu
        lvHistory.setAdapter(adapter);

        // 4. Bắt đầu tải dữ liệu lịch sử từ Firebase về điện thoại
        loadHistoryFromFirebase();

        // 5. Thiết lập sự kiện click cho nút "Trở về màn hình chính"
        btnBackToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Đóng màn hình này để quay lại màn hình trước đó (MainActivity)
            }
        });
    }

    // Hàm lắng nghe sự thay đổi và đồng bộ dữ liệu Realtime từ Firebase
    private void loadHistoryFromFirebase() {
        // Sắp xếp dữ liệu theo biến "timestamp" tăng dần từ Firebase
        databaseReference.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                historyList.clear(); // Xóa sạch dữ liệu cũ lưu tạm trong máy để tránh trùng lặp

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    GameHistory history = childSnapshot.getValue(GameHistory.class);
                    if (history != null) {
                        historyList.add(history);
                    }
                }

                // Do Firebase sắp xếp tăng dần (trận cũ lên trước), ta đảo ngược mảng để trận mới nhất lên đầu
                Collections.reverse(historyList);

                // Lệnh thông báo cho giao diện biết dữ liệu đã thay đổi để vẽ lại danh sách lên màn hình
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Hiển thị thông báo nếu xảy ra lỗi bảo mật hoặc mất mạng kết nối
                Toast.makeText(HistoryActivity.this, "Lỗi tải lịch sử: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}