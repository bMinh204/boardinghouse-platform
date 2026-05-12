# Kịch Bản Kiểm Thử Hệ Thống TroTotTNU

## 1. Phạm Vi Kiểm Thử

Kịch bản này dùng để kiểm thử ứng dụng quản lý và kết nối thuê phòng trọ TroTotTNU, bao gồm:

- Đăng ký, kích hoạt, đăng nhập, đăng xuất
- Quản lý phòng trọ theo vai trò người thuê, chủ trọ, admin
- Tìm kiếm, xem chi tiết, lưu yêu thích, gửi yêu cầu thuê
- Chat, đánh giá và khảo sát phòng
- Dashboard và kiểm duyệt của admin
- Chatbot gợi ý phòng
- Phân quyền và bảo mật endpoint

## 2. Điều Kiện Tiền Quyết

- MySQL đang chạy trên cổng `3306`
- Database: `boarding_house_platform`
- Tài khoản database mặc định:
  - Username: `root`
  - Password: `123456`
- Build thành công bằng:

```powershell
.\build.cmd
```

- Chạy ứng dụng:

```powershell
.\run_server.cmd
```

- Truy cập: `http://localhost:8080`
- Tài khoản admin seed:
  - Email: `admin@trototn.vn`
  - Mật khẩu: `admin123`

## 3. Kiểm Thử Build Và Khởi Động

| Mã TC | Tên kịch bản | Bước thực hiện | Kết quả mong đợi |
| --- | --- | --- | --- |
| TC-BUILD-01 | Build ứng dụng | Chạy `.\build.cmd` | Build thành công, tạo file `.jar` trong `target` |
| TC-BUILD-02 | Unit test | Chạy `mvn test` với Maven trong workspace | Tất cả test pass |
| TC-RUN-01 | Khởi động ứng dụng | Chạy `.\run_server.cmd` | Ứng dụng khởi động trên cổng `8080` |
| TC-RUN-02 | Mở trang chủ | Truy cập `/` | Trang chủ hiển thị thành công |

## 4. Kiểm Thử Tài Khoản Và Xác Thực

| Mã TC | Tên kịch bản | Dữ liệu đầu vào | Bước thực hiện | Kết quả mong đợi |
| --- | --- | --- | --- | --- |
| TC-AUTH-01 | Đăng nhập admin hợp lệ | `admin@trototn.vn / admin123` | Đăng nhập tại màn hình auth | Đăng nhập thành công, hiển thị vai trò Admin |
| TC-AUTH-02 | Đăng nhập sai mật khẩu | Email đúng, mật khẩu sai | Gửi form đăng nhập | Hiển thị thông báo `Sai email hoặc mật khẩu` |
| TC-AUTH-03 | Đăng ký người thuê | Họ tên, email mới, mật khẩu >= 6 ký tự, role TENANT | Gửi form đăng ký | Tạo tài khoản, yêu cầu kích hoạt OTP |
| TC-AUTH-04 | Đăng ký email trùng | Email đã tồn tại | Gửi form đăng ký | Hiển thị thông báo `Email đã tồn tại` |
| TC-AUTH-05 | Đăng ký mật khẩu ngắn | Mật khẩu < 6 ký tự | Gửi form đăng ký | Hiển thị thông báo mật khẩu tối thiểu 6 ký tự |
| TC-AUTH-06 | Kích hoạt OTP hợp lệ | Email mới và OTP đúng | Nhập OTP kích hoạt | Tài khoản được kích hoạt |
| TC-AUTH-07 | Đăng xuất | Tài khoản đã đăng nhập | Bấm Đăng xuất | Session bị hủy, giao diện về trạng thái chưa đăng nhập |

## 5. Kiểm Thử Tìm Kiếm Và Xem Phòng

| Mã TC | Tên kịch bản | Bước thực hiện | Kết quả mong đợi |
| --- | --- | --- | --- |
| TC-ROOM-01 | Xem danh sách phòng công khai | Truy cập trang chủ hoặc `GET /api/rooms` | Danh sách phòng hiển thị, không cần đăng nhập |
| TC-ROOM-02 | Tìm phòng theo từ khóa | Nhập keyword vào form tìm kiếm | Danh sách phòng được lọc theo keyword |
| TC-ROOM-03 | Lọc theo khu vực | Chọn hoặc nhập khu vực | Chỉ hiển thị phòng thuộc khu vực phù hợp |
| TC-ROOM-04 | Lọc theo khoảng giá | Nhập minPrice/maxPrice | Chỉ hiển thị phòng trong khoảng giá |
| TC-ROOM-05 | Xem chi tiết phòng | Bấm vào một phòng | Hiển thị ảnh, giá, tiện nghi, mô tả, bản đồ, đánh giá |
| TC-ROOM-06 | Phòng không tồn tại | Gọi `GET /api/rooms/{id}` với id không tồn tại | Trả lỗi hợp lệ, server không crash |

## 6. Kiểm Thử Vai Trò Người Thuê

| Mã TC | Tên kịch bản | Điều kiện | Bước thực hiện | Kết quả mong đợi |
| --- | --- | --- | --- | --- |
| TC-TENANT-01 | Xem phòng yêu thích | Đăng nhập TENANT | Mở khu vực Phòng đã lưu | Hiển thị danh sách phòng yêu thích |
| TC-TENANT-02 | Lưu phòng yêu thích | Đăng nhập TENANT | Bấm Yêu thích trên phòng | Phòng được thêm vào danh sách yêu thích |
| TC-TENANT-03 | Bỏ lưu yêu thích | Phòng đã được lưu | Bấm Bỏ lưu | Phòng bị xóa khỏi danh sách yêu thích |
| TC-TENANT-04 | Gửi yêu cầu thuê | Đăng nhập TENANT, phòng hợp lệ | Nhập ngày vào ở và ghi chú | Tạo yêu cầu thuê trạng thái PENDING |
| TC-TENANT-05 | Gửi đánh giá phòng | Đăng nhập TENANT | Nhập điểm và bình luận | Đánh giá được lưu, điểm trung bình cập nhật |
| TC-TENANT-06 | Tạo hội thoại với chủ trọ | Đăng nhập TENANT | Gửi tin nhắn từ chi tiết phòng | Tạo conversation mới |

## 7. Kiểm Thử Vai Trò Chủ Trọ

| Mã TC | Tên kịch bản | Điều kiện | Bước thực hiện | Kết quả mong đợi |
| --- | --- | --- | --- | --- |
| TC-LANDLORD-01 | Tạo tin phòng | Đăng nhập LANDLORD | Nhập thông tin phòng và submit | Tin phòng được tạo, trạng thái kiểm duyệt PENDING |
| TC-LANDLORD-02 | Xem phòng của tôi | Đăng nhập LANDLORD | Mở mục quản lý phòng | Chỉ hiển thị phòng thuộc chủ trọ đang đăng nhập |
| TC-LANDLORD-03 | Cập nhật tin phòng | Có phòng thuộc chủ trọ | Sửa thông tin và lưu | Phòng cập nhật thành công |
| TC-LANDLORD-04 | Xóa tin phòng | Có phòng thuộc chủ trọ | Bấm xóa phòng | Phòng bị xóa hoặc không còn hiển thị |
| TC-LANDLORD-05 | Cập nhật trạng thái phòng | Có phòng thuộc chủ trọ | Chuyển AVAILABLE/OCCUPIED/MAINTENANCE | Trạng thái phòng cập nhật đúng |
| TC-LANDLORD-06 | Xem dashboard chủ trọ | Đăng nhập LANDLORD | Mở dashboard landlord | Hiển thị tổng phòng, phòng trống, đã thuê, lượt xem |
| TC-LANDLORD-07 | Xử lý yêu cầu thuê | Có rental request | Duyệt hoặc từ chối | Trạng thái yêu cầu được cập nhật |

## 8. Kiểm Thử Vai Trò Admin

| Mã TC | Tên kịch bản | Điều kiện | Bước thực hiện | Kết quả mong đợi |
| --- | --- | --- | --- | --- |
| TC-ADMIN-01 | Xem dashboard admin | Đăng nhập ADMIN | Mở dashboard admin | Hiển thị tổng user, chủ trọ, người thuê, phòng, hội thoại |
| TC-ADMIN-02 | Xem phòng chờ duyệt | Đăng nhập ADMIN | Mở danh sách phòng pending | Hiển thị các tin phòng đang chờ duyệt |
| TC-ADMIN-03 | Duyệt tin phòng | Có phòng PENDING | Bấm Duyệt | Phòng chuyển sang APPROVED |
| TC-ADMIN-04 | Từ chối tin phòng | Có phòng PENDING | Bấm Từ chối | Phòng chuyển sang REJECTED |
| TC-ADMIN-05 | Quản lý user | Đăng nhập ADMIN | Mở danh sách user | Hiển thị danh sách user |
| TC-ADMIN-06 | Đổi vai trò user | Đăng nhập ADMIN | Đổi TENANT/LANDLORD/ADMIN | Vai trò user cập nhật đúng |
| TC-ADMIN-07 | Khóa/mở khóa user | Đăng nhập ADMIN | Cập nhật locked/active | Trạng thái user cập nhật đúng |
| TC-ADMIN-08 | Xóa user khác | Đăng nhập ADMIN | Xóa một user không phải chính mình | User và dữ liệu liên quan được xóa |
| TC-ADMIN-09 | Chặn xóa chính mình | Đăng nhập ADMIN | Xóa tài khoản đang đăng nhập | Hệ thống từ chối thao tác |
| TC-ADMIN-10 | Backup dữ liệu | Đăng nhập ADMIN | Bấm backup JSON | Tải về file JSON dữ liệu |

## 9. Kiểm Thử Chatbot

| Mã TC | Tên kịch bản | Dữ liệu đầu vào | Kết quả mong đợi |
| --- | --- | --- | --- |
| TC-BOT-01 | Gợi ý theo ngân sách | `phòng dưới 2 triệu` | Chatbot trả lời và lọc phòng theo ngân sách |
| TC-BOT-02 | Gợi ý theo khu vực | `phòng gần ICTU` | Chatbot nhận diện khu vực và gợi ý phòng |
| TC-BOT-03 | Gợi ý theo tiện nghi | `phòng có điều hòa` | Chatbot nhận diện tiện nghi |
| TC-BOT-04 | Prompt rỗng | Chuỗi rỗng | Không crash, trả thông báo phù hợp |

## 10. Kiểm Thử Phân Quyền Và Bảo Mật

| Mã TC | Endpoint | Anonymous | TENANT | LANDLORD | ADMIN |
| --- | --- | --- | --- | --- | --- |
| TC-SEC-01 | `/api/rooms` | 200 | 200 | 200 | 200 |
| TC-SEC-02 | `/api/rooms/favorites` | 403 | 200 | 403 | 403 |
| TC-SEC-03 | `/api/rooms/mine` | 403 | 403 | 200 | 403 |
| TC-SEC-04 | `/api/rooms/pending` | 403 | 403 | 403 | 200 |
| TC-SEC-05 | `/api/dashboard/landlord` | 403 | 403 | 200 | 403 |
| TC-SEC-06 | `/api/dashboard/admin` | 403 | 403 | 403 | 200 |
| TC-SEC-07 | `/api/dashboard/backup` | 403 | 403 | 403 | 200 |
| TC-SEC-08 | `/api/users` | 403 | 403 | 403 | 200 |

## 11. Kiểm Thử Biên Và Lỗi

| Mã TC | Tên kịch bản | Bước thực hiện | Kết quả mong đợi |
| --- | --- | --- | --- |
| TC-ERR-01 | Sai URL API | Gọi endpoint không tồn tại | Trả 404, server không crash |
| TC-ERR-02 | Payload thiếu trường bắt buộc | Gửi body thiếu email/password | Trả 400 hoặc thông báo hợp lệ |
| TC-ERR-03 | JSON sai định dạng | Gửi body không phải JSON hợp lệ | Trả lỗi client, server không crash |
| TC-ERR-04 | Upload file quá lớn | Upload file lớn hơn giới hạn | Trả lỗi vượt giới hạn |
| TC-ERR-05 | DB password sai | Chạy app với `DB_PASSWORD` sai | App báo lỗi kết nối DB |

## 12. Kiểm Thử Hồi Quy Sau Mỗi Lần Sửa

Sau mỗi lần sửa code, chạy tối thiểu:

```powershell
.\build.cmd
E:\DATN\project\tools\apache-maven-3.9.9\bin\mvn.cmd -q -s E:\DATN\project\boardinghouse_platform\maven-settings.xml test
```

Sau đó kiểm tra nhanh:

- Trang `/` trả HTTP 200
- `GET /api/auth/me` trả HTTP 200
- `GET /api/rooms` trả HTTP 200
- Đăng nhập admin thành công
- Admin mở dashboard thành công
- Anonymous bị chặn khi vào endpoint admin

## 13. Tiêu Chí Chấp Nhận

Hệ thống được xem là đạt nếu:

- Build và test tự động thành công
- Ứng dụng khởi động được với cấu hình DB mặc định
- Các trang chính trả HTTP 200
- Các API public hoạt động
- Đăng nhập/đăng xuất đúng
- Phân quyền đúng theo vai trò
- Không có lỗi 500 trong các luồng chính
- Response tiếng Việt hiển thị đúng UTF-8
