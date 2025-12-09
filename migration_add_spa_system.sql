-- =========================================================
-- ECHOMMO: THÊM HỆ THỐNG SPA NGHỈ NGƠI (RESTING SYSTEM)
-- Công thức: Thời gian nghỉ = % máu thiếu × 300 giây (tối đa 5 phút)
-- =========================================================

USE echommo_db;

-- Thêm 3 cột mới vào bảng characters để lưu trạng thái spa
ALTER TABLE characters
ADD COLUMN spa_start_time DATETIME NULL COMMENT 'Thời gian bắt đầu nghỉ ngơi',
ADD COLUMN spa_end_time DATETIME NULL COMMENT 'Thời gian kết thúc nghỉ ngơi',
ADD COLUMN spa_package_type VARCHAR(20) NULL COMMENT 'Loại gói spa đã chọn (STANDARD/VIP/ROYAL)';

-- Hoàn tất!
SELECT 'Hệ thống Spa đã được cài đặt thành công!' AS status;
