package vn.edu.dlu.dhopm.core;

import vn.edu.dlu.dhopm.model.Entry;

import java.util.*;

/**
 * Bo tinh toan can tren Damped Upper Bound Occupancy (DUBO) theo Dinh nghia 6 trong bai bao DHOPM.
 *
 * Cong thuc:
 *   DUBO(X, k) = sum_{i=k}^{u} ( n_i * (l_k / l_i) * f^(TL - T_k) )
 *   DUBO(X) = max_{1 <= k <= u} { DUBO(X, k) }
 *
 * Trong do:
 * - L = {l_1, l_2, ..., l_u} la tap do dai cac giao dich chua X (sap tang dan)
 * - n_k la so luong giao dich co do dai l_k
 * - T_k la TID lon nhat trong cac giao dich co do dai l_k chua X
 * - TL la TID moi nhat cua toan bo CSDL da quet
 */
public class DUBOCalculator {

    /**
     * Tinh toan DUBO cho mot tap hop cac entry <TID, TLen> chua mau X.
     *
     * @param entries Danh sach cac entry cua mau X
     * @param f       He so suy giam (0 < f < 1)
     * @param TL      TID moi nhat trong CSDL
     * @return Gia tri can tren DUBO(X)
     */
    public static double calculate(List<Entry> entries, double f, int TL) {
        if (entries == null || entries.isEmpty()) {
            return 0.0;
        }
        if (!Double.isFinite(f) || f <= 0.0 || f > 1.0) {
            throw new IllegalArgumentException("Decay factor f must be in (0, 1]");
        }

        // 1. Gom nhom cac entry theo do dai giao dich (tlen)
        // Map: tlen -> Danh sach cac TID co do dai do
        Map<Integer, List<Integer>> lengthToTids = new TreeMap<>();
        for (Entry e : entries) {
            lengthToTids.computeIfAbsent(e.tlen(), k -> new ArrayList<>()).add(e.tid());
        }

        List<Integer> sortedLens = new ArrayList<>(lengthToTids.keySet());
        int u = sortedLens.size();

        double maxDUBO = 0.0;

        // 2. Tinh toan DUBO(X, k) cho moi k tu 0 den u-1
        for (int k = 0; k < u; k++) {
            int lk = sortedLens.get(k);
            List<Integer> tidsOfLk = lengthToTids.get(lk);
            int Tk = Collections.max(tidsOfLk); // TID lon nhat trong cac GD co do dai l_k

            double sum = 0.0;
            for (int i = k; i < u; i++) {
                int li = sortedLens.get(i);
                int ni = lengthToTids.get(li).size();
                sum += ni * ((double) lk / li);
            }

            double decay = Math.pow(f, TL - Tk);
            double duboK = sum * decay;

            if (duboK > maxDUBO) {
                maxDUBO = duboK;
            }
        }

        return maxDUBO;
    }
}
