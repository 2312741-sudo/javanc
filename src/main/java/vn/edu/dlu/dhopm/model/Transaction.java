package vn.edu.dlu.dhopm.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Bieu dien mot giao dich trong co so du lieu stream.
 */
public class Transaction {
    private final int tid;
    private final List<String> items;

    public Transaction(int tid, List<String> items) {
        this.tid = tid;
        this.items = new ArrayList<>(Objects.requireNonNull(items));
        Collections.sort(this.items);
    }

    public int getTid() {
        return tid;
    }

    public List<String> getItems() {
        return Collections.unmodifiableList(items);
    }

    public int getLength() {
        return items.size();
    }

    public boolean containsItem(String item) {
        return items.contains(item);
    }

    public boolean containsAll(List<String> pattern) {
        return items.containsAll(pattern);
    }

    @Override
    public String toString() {
        return "T" + tid + " = " + items + " (|T|=" + items.size() + ")";
    }
}
