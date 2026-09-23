package vn.edu.dlu.dhopm.core;

import vn.edu.dlu.dhopm.model.Transaction;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tien ich nap du lieu giao dich tu file hoac chuoi text.
 */
public class DatasetLoader {

    /**
     * Parse tap giao dich tu InputStream (resource hoac file).
     * Dinh dang: TID item1 item2 item3 ...
     * Hoac: item1 item2 item3 ... (TID tu dong tang)
     */
    public static List<Transaction> loadFromStream(InputStream in) {
        List<Transaction> transactions = new ArrayList<>();
        if (in == null) return transactions;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            int autoTid = 1;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("[\\s,]+");
                if (parts.length == 0) continue;

                int tid;
                int startItemIdx;
                try {
                    tid = Integer.parseInt(parts[0]);
                    startItemIdx = 1;
                } catch (NumberFormatException e) {
                    tid = autoTid++;
                    startItemIdx = 0;
                }

                List<String> items = new ArrayList<>();
                for (int i = startItemIdx; i < parts.length; i++) {
                    if (!parts[i].isBlank()) {
                        items.add(parts[i].trim().toUpperCase());
                    }
                }

                if (!items.isEmpty()) {
                    transactions.add(new Transaction(tid, items));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return transactions;
    }

    /**
     * Tra ve danh sach 8 giao dich chuan cua bai bao (Table 1).
     */
    public static List<Transaction> getPaperDataset() {
        List<Transaction> list = new ArrayList<>();
        list.add(new Transaction(1, Arrays.asList("A", "C", "D", "E")));
        list.add(new Transaction(2, Arrays.asList("A", "E", "F")));
        list.add(new Transaction(3, Arrays.asList("B", "C", "D", "E")));
        list.add(new Transaction(4, Arrays.asList("C", "D", "F")));
        list.add(new Transaction(5, Arrays.asList("B", "F")));
        list.add(new Transaction(6, Arrays.asList("D", "E", "F")));
        list.add(new Transaction(7, Arrays.asList("A", "B", "C", "F")));
        list.add(new Transaction(8, Arrays.asList("A", "E", "G")));
        return list;
    }

    /**
     * Tra ve mot tap giao dich mau de tiep tuc bom stream.
     */
    public static List<Transaction> getSyntheticStreamDataset() {
        List<Transaction> list = new ArrayList<>();
        list.add(new Transaction(9, Arrays.asList("A", "E")));
        list.add(new Transaction(10, Arrays.asList("B", "F")));
        list.add(new Transaction(11, Arrays.asList("A", "B", "C", "F")));
        list.add(new Transaction(12, Arrays.asList("A", "E", "G")));
        list.add(new Transaction(13, Arrays.asList("D", "E", "F")));
        list.add(new Transaction(14, Arrays.asList("A", "E")));
        list.add(new Transaction(15, Arrays.asList("B", "C", "D", "E")));
        return list;
    }

    /**
     * Tra ve tap giao dich DB0 (T1..T4) cua bai bao.
     */
    public static List<Transaction> getDB0Dataset() {
        return Arrays.asList(
                new Transaction(1, Arrays.asList("A", "C", "D", "E")),
                new Transaction(2, Arrays.asList("A", "E", "F")),
                new Transaction(3, Arrays.asList("B", "C", "D", "E")),
                new Transaction(4, Arrays.asList("C", "D", "F"))
        );
    }

    /**
     * Tra ve tap giao dich TC7_Custom (10 giao dich) tu Lab 1.
     */
    public static List<Transaction> getTC7CustomDataset() {
        return Arrays.asList(
                new Transaction(1, Arrays.asList("A", "B", "C")),
                new Transaction(2, Arrays.asList("A", "B")),
                new Transaction(3, Arrays.asList("B", "C", "D")),
                new Transaction(4, Arrays.asList("A", "C")),
                new Transaction(5, Arrays.asList("A", "B", "C", "D")),
                new Transaction(6, Arrays.asList("B", "D")),
                new Transaction(7, Arrays.asList("A", "C")),
                new Transaction(8, Arrays.asList("A", "B", "C")),
                new Transaction(9, Arrays.asList("B", "C", "D")),
                new Transaction(10, Arrays.asList("A", "B"))
        );
    }
}
