# Bếp Ngon Merge — Release Notes

## v1.0.0 — bản release đầu tiên

Merge game kiểu Merge Mansion / Travel Town: bàn cờ 5×7, bấm máy sinh để tạo
nguyên liệu, kéo 2 món giống nhau để ghép lên cấp cao hơn, giao đơn cho khách
kiếm vàng và kinh nghiệm.

### Gameplay chính

- **4 dòng nguyên liệu, 40+ món**: Trái cây 🍒, Tiệm bánh 🥐, Đồ uống 🧋 và
  Đồ ăn Việt 🍜 — mỗi dòng có chuỗi merge 7–10 cấp.
- **Máy sinh nâng cấp được**: mỗi dòng có 3 cấp máy (Hộp bánh → Tiệm bánh →
  Nhà máy bánh). Máy cấp cao sinh ra được item tier cao hơn nhưng tier 1 luôn
  là tỉ lệ phổ biến nhất để giữ nhịp game cân bằng.
- **Item hiếm** ✨: 4 chuỗi phụ 3 cấp (xoài cát, bánh tart trứng, trà matcha,
  tôm hùm bơ tỏi...) — xác suất thấp từ máy, giá trị cao, đơn hàng thỉnh thoảng yêu
  cầu. Info máy hiện đầy đủ cả chuỗi merge thường lẫn chuỗi hiếm.
- **Lượt máy sinh**: máy dùng chưa hết thì hồi +1 lượt mỗi 2 phút; dùng sạch
  thì nghỉ 45 giây rồi hồi đầy. Đồng hồ hồi lượt vẫn chạy khi tắt app.
- **Đơn hàng từ thú cưng khách** 🐷🐱🐸: giao đơn nhận vàng + XP; giao nhiều
  đơn liên tiếp trong 30 giây được thưởng combo. Có thể giao tự động khi đủ món.
- **Năng lượng ⚡**: mỗi lần sinh tốn 1⚡, hồi dần theo thời gian (tối đa 100).
- **Lên cấp**: XP từ đơn hàng mở khoá thêm ô bàn cờ (đến Lv.13 mở hết 35 ô),
  thưởng vàng/máy sinh/ô kho mỗi cấp.
- **Sự kiện trong game**: bong bóng QTE bất ngờ (bấm đúng lúc nhận quà), giai
  đoạn Frenzy "GEN MIỄN PHÍ", quà điểm danh hằng ngày theo streak.
- **46 thành tựu / 15 chuỗi** 🏆: merge, đơn hàng, cấp độ, vàng, item hiếm,
  điểm số, minigame, đồ trang trí... claim theo bậc, nhận thưởng vàng.

### Nội dung phụ

- **Minigame "Thả trái"** 🍉: thả trái rơi vào hộp, trái giống chạm nhau tự
  ghép lớn dần (physics jbox2d) — kiếm thêm ⚡.
- **Phòng trang trí** 🛋️: góc nhìn isometric, mua và đặt nội thất, xoay/đổi
  kích thước, sơn tường/sàn theo pattern + màu, chọn bầu trời, undo/redo.
- **Kho đồ**: cất item quý vào túi, mở rộng thêm ô bằng vàng.
- **Lưu offline**: toàn bộ tiến trình lưu tự động trên máy, không cần mạng.

### Kỹ thuật

- Kotlin + Jetpack Compose (Material3), minSdk 26, targetSdk 36.
- Engine game thuần Kotlin tách riêng (`game/`), có unit test.
- R8 minify + shrinkResources bật, APK ký bằng keystore riêng.
- Hiển thị version ở góc dưới-phải màn hình (`v1.0.0`).
