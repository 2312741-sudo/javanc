package vn.edu.dlu.dhopm.model;

import java.util.*;

/**
 * Cau truc Global DHO-List quan ly cac node cua cac item trong toan bo CSDL.
 * Cung cap 2 tien trinh co ban:
 * 1. constructOrUpdate: Quet mot lan duy nhat de chen cac entry <TID, TLen>
 * 2. reconstruct: Tinh lai DO theo TL va he so suy giam f, sap xep theo Support tang dan
 */
public class DHOList {
    private final Map<String, DHONode> nodeMap;
    private final List<DHONode> sortedNodes;

    public DHOList() {
        this.nodeMap = new LinkedHashMap<>();
        this.sortedNodes = new ArrayList<>();
    }

    /**
     * Pha 1: Xay dung hoac cap nhat Global DHO-List bang cach quet 1 lan cac giao dich moi.
     *
     * @param transactions Danh sach cac giao dich can nap (DB0 hoac cac phan gia tang DB1, DB2, ...)
     */
    public void constructOrUpdate(List<Transaction> transactions) {
        for (Transaction t : transactions) {
            int tid = t.getTid();
            int tlen = t.getLength();

            for (String item : t.getItems()) {
                DHONode node = nodeMap.computeIfAbsent(item, DHONode::new);
                node.addEntry(tid, tlen);
            }
        }
    }

    /**
     * Pha 2: Tai cau truc Global DHO-List khi co yeu cau khai pha.
     * - Tinh toan lai damped occupancy (DO) cho tung node dua tren TL va f
     * - Sap xep cac node theo thu tu Support TANG DAN (de uu tien cat tia som bang DUBO)
     *
     * @param f  He so suy giam (0 < f < 1)
     * @param TL Ma dinh danh giao dich moi nhat da quet
     */
    public void reconstruct(double f, int TL) {
        // 1. Tinh lai DO cho tung node
        for (DHONode node : nodeMap.values()) {
            node.resetDO();
            for (Entry entry : node.getEntries()) {
                double occupancy = 1.0 / entry.tlen();
                double decay = Math.pow(f, TL - entry.tid());
                node.accumulateDO(occupancy * decay);
            }
        }

        // 2. Sap xep theo Support tang dan
        sortedNodes.clear();
        sortedNodes.addAll(nodeMap.values());
        sortedNodes.sort(Comparator.comparingInt(DHONode::getSupport)
                .thenComparing(DHONode::getItemName));
    }

    public DHONode getNode(String item) {
        return nodeMap.get(item);
    }

    public List<DHONode> getSortedNodes() {
        return Collections.unmodifiableList(sortedNodes);
    }

    public Collection<DHONode> getAllNodes() {
        return Collections.unmodifiableCollection(nodeMap.values());
    }

    public int getItemCount() {
        return nodeMap.size();
    }

    public void clear() {
        nodeMap.clear();
        sortedNodes.clear();
    }
}
