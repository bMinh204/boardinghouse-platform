# Hướng dẫn Cài đặt và Chạy dự án Trọ Tốt ICTU

Tài liệu này hướng dẫn các bước để cài đặt môi trường, cấu hình và chạy ứng dụng web Trọ Tốt ICTU trên máy tính cá nhân (localhost).

## 1. Yêu cầu phần mềm (Prerequisites)

Trước khi bắt đầu, hãy đảm bảo bạn đã cài đặt các phần mềm sau:

- **Java Development Kit (JDK):** Phiên bản **17** hoặc mới hơn.
  - *Kiểm tra phiên bản:* Mở terminal và gõ `java -version`.
- **Apache Maven:** Phiên bản 3.6+ để quản lý và build dự án.
  - *Kiểm tra phiên bản:* Gõ `mvn -version`.
  - *Lưu ý:* Nếu bạn chưa cài Maven vào biến môi trường hệ thống, bạn có thể dùng file `mvnw` (Maven Wrapper) đi kèm trong dự án.
- **MySQL Server:** Phiên bản 8.0 hoặc mới hơn.
  - Trong quá trình cài đặt, hãy ghi nhớ mật khẩu của tài khoản `root`.
- **IDE (Tùy chọn, Khuyến khích):** Một trình soạn thảo code như **IntelliJ IDEA** (bản Community miễn phí là đủ) hoặc **Visual Studio Code** với Gói mở rộng cho Java (Extension Pack for Java).

---

## 2. Cài đặt Cơ sở dữ liệu (Database Setup)

Ứng dụng sử dụng MySQL để lưu trữ dữ liệu.

1.  **Đăng nhập vào MySQL:** Mở terminal và đăng nhập vào MySQL với quyền root:
    ```sql
    mysql -u root -p
    ```
    (Nhập mật khẩu của bạn khi được hỏi).

2.  **Tạo Database:** Chạy lệnh SQL sau để tạo cơ sở dữ liệu cho dự án.
    ```sql
    CREATE DATABASE boarding_house_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    ```
    *Ghi chú: Tên database này (`boarding_house_platform`) được cấu hình trong file `application.properties`. Hibernate sẽ tự động tạo các bảng cần thiết khi ứng dụng khởi động lần đầu.*

---

## 3. Cấu hình Môi trường (Configuration)

Ứng dụng cần biết thông tin để kết nối tới MySQL của bạn. Thông tin này được đọc từ các **biến môi trường (Environment Variables)**.

Mở terminal (PowerShell hoặc CMD trên Windows, Terminal trên macOS/Linux) và đặt các biến sau:

```powershell
# --- Dành cho Windows (PowerShell) ---

# Tên đăng nhập MySQL (thường là 'root')
$env:DB_USERNAME="root"

# Mật khẩu bạn đã đặt cho tài khoản root khi cài MySQL
$env:DB_PASSWORD="your_mysql_password"

# (Tùy chọn) URL kết nối, thường không cần đổi nếu chạy MySQL ở cổng mặc định 3306
$env:DB_URL="jdbc:mysql://localhost:3306/boarding_house_platform?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh"
```

**Quan trọng:**
- Thay `"your_mysql_password"` bằng mật khẩu MySQL thực tế của bạn.
- Nếu bạn không thiết lập các biến môi trường này, ứng dụng sẽ thử kết nối với giá trị mặc định là `username=root` và `password=123456`.

---

## 4. Chạy ứng dụng (Running the Application)

Có hai cách phổ biến để chạy dự án:

### Cách 1: Chạy bằng IDE (Khuyến khích cho Lập trình viên)

Đây là cách đơn giản và tiện lợi nhất để phát triển.

1.  **Mở Dự án:**
    - **IntelliJ IDEA:** Chọn `File > Open...` và trỏ tới thư mục gốc `boardinghouse_platform`.
    - **VS Code:** Chọn `File > Open Folder...` và trỏ tới thư mục gốc `boardinghouse_platform`.
2.  **Chạy Ứng dụng:**
    - IDE sẽ tự động nhận diện đây là một dự án Maven.
    - Tìm đến file `BoardinghousePlatformApplication.java` trong thư mục `src/main/java/com/trototn/boardinghouse`.
    - Nhấn vào nút ▶ (Run) màu xanh lá cây bên cạnh class `BoardinghousePlatformApplication` để khởi chạy.
    - IDE sẽ tự động build và chạy server Spring Boot.

### Cách 2: Chạy bằng Maven từ Dòng lệnh

Cách này phù hợp khi bạn không dùng IDE hoặc muốn chạy trên server.

1.  Mở terminal và di chuyển vào thư mục gốc của dự án (`boardinghouse_platform`).
2.  Đảm bảo bạn đã thiết lập các biến môi trường ở Bước 3.
3.  Chạy lệnh sau:
    ```bash
    mvn spring-boot:run
    ```
    Maven sẽ tải các thư viện cần thiết, biên dịch code và khởi động server.

---

## 5. Truy cập ứng dụng

Sau khi server khởi động thành công (bạn sẽ thấy logo Spring và các dòng log trong terminal), hãy:

1.  Mở trình duyệt web (Chrome, Firefox, ...).
2.  Truy cập vào địa chỉ: `http://localhost:8080`.

### Tài khoản Admin Mặc định

Hệ thống sẽ tự động tạo một tài khoản Admin khi khởi động lần đầu nếu nó chưa tồn tại trong database.

- **Email:** `admin@trototn.vn`
- **Mật khẩu:** `admin123`

Bạn có thể dùng tài khoản này để đăng nhập và thực hiện các chức năng quản trị.

---

## 6. Xử lý sự cố (Troubleshooting)

- **Lỗi `Port 8080 was already in use`:**
  - **Nguyên nhân:** Một chương trình khác (có thể là một lần chạy cũ của dự án này) đang chiếm giữ cổng 8080.
  - **Giải pháp:** Dừng ứng dụng đang chạy đó, hoặc đổi cổng trong file `src/main/resources/application.properties` bằng cách thêm dòng `server.port=8081` và truy cập `http://localhost:8081`.

- **Lỗi kết nối Database (`Communications link failure`, `Access denied for user 'root'@'localhost'`):**
  - **Nguyên nhân:** Thông tin kết nối tới MySQL không chính xác.
  - **Giải pháp:**
    1.  Kiểm tra xem MySQL Server đã được khởi động chưa.
    2.  Kiểm tra lại giá trị của biến môi trường `DB_USERNAME` và `DB_PASSWORD` đã đúng với tài khoản MySQL của bạn chưa.
    3.  Đảm bảo database `boarding_house_platform` đã được tạo (theo Bước 2).

- **Lỗi phiên bản Java (`Unsupported class file major version`):**
  - **Nguyên nhân:** Bạn đang dùng phiên bản Java thấp hơn yêu cầu (JDK 17).
  - **Giải pháp:** Cài đặt JDK 17 và cấu hình IDE/hệ thống của bạn để sử dụng phiên bản này.
