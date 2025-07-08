# App Detection Service - Hướng dẫn sử dụng

## Tổng quan
Dự án đã được cập nhật với hai tính năng chính:
1. **Xin quyền vẽ Overlay và Usage Stats**: Cho phép ứng dụng vẽ overlay và truy cập thống kê sử dụng
2. **Service Detect ứng dụng**: Phát hiện ứng dụng đang được mở mới nhất và tự động khóa nếu cần

## Các file đã được thêm/cập nhật

### 1. AndroidManifest.xml
- Thêm quyền `SYSTEM_ALERT_WINDOW` (vẽ overlay)
- Thêm quyền `PACKAGE_USAGE_STATS` (truy cập thống kê sử dụng)
- Thêm quyền `GET_TASKS` (truy cập thông tin ứng dụng đang chạy)
- Đăng ký `AppDetectionService`

### 2. AppDetectionService.kt (MỚI)
- Service chạy nền để phát hiện ứng dụng đang được mở
- Sử dụng `UsageStatsManager` để theo dõi sự thay đổi ứng dụng
- Tự động khóa ứng dụng nếu nằm trong danh sách cần khóa
- Cung cấp interface `AppChangeListener` để thông báo khi ứng dụng thay đổi

### 3. PermissionManager.kt (MỚI)
- Quản lý việc kiểm tra và yêu cầu quyền
- Hiển thị dialog hướng dẫn người dùng cấp quyền
- Xử lý kết quả từ Settings

### 4. AppLockConfig.kt (MỚI)
- Quản lý cấu hình ứng dụng cần khóa
- Lưu trữ danh sách ứng dụng trong SharedPreferences
- Cung cấp các phương thức thêm/xóa/kiểm tra ứng dụng khóa

### 5. MainActivity.kt (CẬP NHẬT)
- Tích hợp `PermissionManager` để yêu cầu quyền
- Khởi động `AppDetectionService`
- Implement `AppChangeListener` để nhận thông báo khi ứng dụng thay đổi
- Thêm button "Kiểm tra quyền & Khởi động Service"

### 6. activity_main.xml (CẬP NHẬT)
- Thêm button "Kiểm tra quyền & Khởi động Service"

## Cách sử dụng

### 1. Khởi động ứng dụng
- Mở ứng dụng AppLockDemo
- Nhấn button "Kiểm tra quyền & Khởi động Service"

### 2. Cấp quyền
- **Quyền vẽ Overlay**: Cho phép ứng dụng hiển thị màn hình khóa trên các ứng dụng khác
- **Quyền Usage Stats**: Cho phép ứng dụng theo dõi ứng dụng nào đang được sử dụng

### 3. Service hoạt động
- Service sẽ chạy nền và theo dõi ứng dụng đang được mở
- Khi phát hiện ứng dụng trong danh sách cần khóa, sẽ tự động hiển thị màn hình PIN

## Danh sách ứng dụng mặc định cần khóa
- Facebook (`com.facebook.katana`)
- Instagram (`com.instagram.android`)
- WhatsApp (`com.whatsapp`)
- Gmail (`com.google.android.gm`)
- Chrome (`com.android.chrome`)

## Tùy chỉnh

### Thêm ứng dụng vào danh sách khóa
```kotlin
val appLockConfig = AppLockConfig(context)
appLockConfig.addLockedApp("com.example.app")
```

### Xóa ứng dụng khỏi danh sách khóa
```kotlin
appLockConfig.removeLockedApp("com.example.app")
```

### Bật/tắt tính năng khóa ứng dụng
```kotlin
appLockConfig.setAppLockEnabled(true) // hoặc false
```

## Lưu ý quan trọng

1. **Quyền Usage Stats**: Đây là quyền đặc biệt, người dùng phải vào Settings > Apps > Special access > Usage access để cấp quyền

2. **Quyền Overlay**: Người dùng phải vào Settings > Apps > Special access > Display over other apps để cấp quyền

3. **Service chạy nền**: Service sẽ tiêu thụ pin, nên cân nhắc tối ưu hóa nếu cần

4. **Tương thích**: Tính năng hoạt động tốt trên Android 6.0+ (API 23+)

## Troubleshooting

### Service không hoạt động
- Kiểm tra xem đã cấp đủ quyền chưa
- Kiểm tra log để xem có lỗi gì không
- Đảm bảo service đã được đăng ký trong AndroidManifest.xml

### Không phát hiện được ứng dụng
- Kiểm tra quyền Usage Stats
- Một số ROM có thể chặn tính năng này
- Thử khởi động lại service

### Màn hình khóa không hiển thị
- Kiểm tra quyền Overlay
- Đảm bảo PIN đã được thiết lập
- Kiểm tra ứng dụng có trong danh sách khóa không