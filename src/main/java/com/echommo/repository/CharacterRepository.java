//package com.echommo.repository;
//
//import com.echommo.entity.Character;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query; // Thêm import này
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface CharacterRepository extends JpaRepository<Character, Integer> {
//
//    Optional<Character> findByUser_UserId(Integer userId);
//
//    boolean existsByName(String name);
//
//    // [FIX PERFORMANCE] Tối ưu hóa: Lấy Character, User và Wallet trong 1 query JOIN FETCH
//    @Query("SELECT c FROM Character c JOIN FETCH c.user u JOIN FETCH u.wallet w WHERE u.userId = :userId")
//    Optional<Character> findByUser_UserIdWithUserAndWallet(Integer userId);
//
//    // [FIX] Sửa tên hàm cho đúng với tên biến trong Entity (level, currentExp)
//    // JPA sẽ tự động hiểu câu lệnh này thành: SELECT * ... ORDER BY level DESC, current_exp DESC LIMIT 10
//    List<Character> findTop10ByOrderByLevelDescCurrentExpDesc();
//}


package com.echommo.repository;

import com.echommo.entity.Character;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List; // <<< CẦN THÊM IMPORT NÀY
import java.util.Optional;

@Repository
public interface CharacterRepository extends JpaRepository<Character, Integer> {

    // Tìm nhân vật theo User ID
    Optional<Character> findByUser_UserId(Integer userId);

    // PHƯƠNG THỨC MỚI CẦN THÊM LẠI: Kiểm tra tên nhân vật đã tồn tại
    boolean existsByName(String name); // <<< THÊM LẠI DÒNG NÀY

    // [QUAN TRỌNG] Hàm này giúp lấy luôn thông tin Ví (Wallet) và User để xử lý tiền nong
    @Query("SELECT c FROM Character c JOIN FETCH c.user u JOIN FETCH u.wallet WHERE u.userId = :userId")
    Optional<Character> findByUser_UserIdWithUserAndWallet(@Param("userId") Integer userId);


    // PHƯƠNG THỨC MỚI CẦN THÊM LẠI: Lấy top 10 cho Leaderboard
    // JPA sẽ tự động hiểu câu lệnh này thành: SELECT * ... ORDER BY level DESC, current_exp DESC LIMIT 10
    List<Character> findTop10ByOrderByLevelDescCurrentExpDesc(); // <<< THÊM LẠI DÒNG NÀY
}