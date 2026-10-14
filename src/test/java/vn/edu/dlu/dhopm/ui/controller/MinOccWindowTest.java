package vn.edu.dlu.dhopm.ui.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests cho phương thức tính kích thước cửa sổ Damped Window từ minOcc.
 * Công thức: W = ⌊log_f(minOcc)⌋ = ⌊ln(minOcc) / ln(f)⌋
 * Nguồn lý thuyết: aaa.md §2.3 "Hệ quả từ chứng minh (Window)"
 *
 * @author Nguyễn Thanh Tâm (2312741)
 */
@DisplayName("minOcc & Damped Window Tests")
class MinOccWindowTest {

    // ── Hợp lệ: công thức W = floor(ln(minOcc)/ln(f)) ────────────────────────

    @Test
    @DisplayName("f=0.90, minOcc=0.1 → W=21")
    void test_f090_minOcc01() {
        assertEquals(21, MainController.calcWindowSize(0.90, 0.1));
    }

    @Test
    @DisplayName("f=0.90, minOcc=0.001 → W=65")
    void test_f090_minOcc001() {
        assertEquals(65, MainController.calcWindowSize(0.90, 0.001));
    }

    @Test
    @DisplayName("f=0.90, minOcc=0.0001 → W=87")
    void test_f090_minOcc0001() {
        assertEquals(87, MainController.calcWindowSize(0.90, 0.0001));
    }

    @Test
    @DisplayName("f=0.95, minOcc=0.1 → W=44")
    void test_f095_minOcc01() {
        assertEquals(44, MainController.calcWindowSize(0.95, 0.1));
    }

    @Test
    @DisplayName("f=0.99, minOcc=0.1 → W=229")
    void test_f099_minOcc01() {
        assertEquals(229, MainController.calcWindowSize(0.99, 0.1));
    }

    @Test
    @DisplayName("W > 0 khi tham số hợp lệ")
    void testWindowPositive() {
        assertTrue(MainController.calcWindowSize(0.9, 0.0001) > 0);
    }

    @Test
    @DisplayName("maxDO = f^W xấp xỉ minOcc")
    void testMaxDO_approx_minOcc() {
        double f = 0.9;
        double minOcc = 0.0001;
        int w = MainController.calcWindowSize(f, minOcc);
        double maxDo = Math.pow(f, w);
        // maxDO phải <= minOcc * 2 (tính floor nên xấp xỉ)
        assertTrue(maxDo <= minOcc * 2.0,
            "maxDO=" + maxDo + " nên xấp xỉ minOcc=" + minOcc);
    }

    @Test
    @DisplayName("W tăng khi f tăng (cố định minOcc=0.001)")
    void testWindowIncreases_with_f() {
        double minOcc = 0.001;
        int w1 = MainController.calcWindowSize(0.90, minOcc);
        int w2 = MainController.calcWindowSize(0.95, minOcc);
        int w3 = MainController.calcWindowSize(0.99, minOcc);
        assertTrue(w1 < w2, "W(f=0.90) < W(f=0.95)");
        assertTrue(w2 < w3, "W(f=0.95) < W(f=0.99)");
    }

    @Test
    @DisplayName("W tăng khi minOcc giảm (cố định f=0.90)")
    void testWindowIncreases_with_smaller_minOcc() {
        double f = 0.90;
        int w1 = MainController.calcWindowSize(f, 0.1);
        int w2 = MainController.calcWindowSize(f, 0.001);
        int w3 = MainController.calcWindowSize(f, 0.000001);
        assertTrue(w1 < w2, "W(minOcc=0.1) < W(minOcc=0.001)");
        assertTrue(w2 < w3, "W(minOcc=0.001) < W(minOcc=1e-6)");
    }

    // ── Trường hợp biên / không hợp lệ → trả về 0 ──────────────────────────

    @Test
    @DisplayName("f=0.0 → W=0 (không hợp lệ)")
    void testInvalid_f_zero() {
        assertEquals(0, MainController.calcWindowSize(0.0, 0.001));
    }

    @Test
    @DisplayName("f=1.0 → W=0 (không hợp lệ)")
    void testInvalid_f_one() {
        assertEquals(0, MainController.calcWindowSize(1.0, 0.001));
    }

    @Test
    @DisplayName("f>1 → W=0 (không hợp lệ)")
    void testInvalid_f_gt_one() {
        assertEquals(0, MainController.calcWindowSize(1.1, 0.001));
    }

    @Test
    @DisplayName("minOcc=0.0 → W=0 (không hợp lệ)")
    void testInvalid_minOcc_zero() {
        assertEquals(0, MainController.calcWindowSize(0.9, 0.0));
    }

    @Test
    @DisplayName("minOcc=1.0 → W=0 (không hợp lệ)")
    void testInvalid_minOcc_one() {
        assertEquals(0, MainController.calcWindowSize(0.9, 1.0));
    }

    @Test
    @DisplayName("minOcc âm → W=0 (không hợp lệ)")
    void testInvalid_minOcc_negative() {
        assertEquals(0, MainController.calcWindowSize(0.9, -0.5));
    }
}
