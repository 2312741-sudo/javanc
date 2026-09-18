package vn.edu.dlu.dhopm.event;

import vn.edu.dlu.dhopm.model.Transaction;
import java.util.List;

/**
 * Observer interface lang nghe su kien khi co batch giao dich moi do ve qua stream.
 */
public interface StreamListener {
    void onBatchArrived(List<Transaction> newBatch, int currentTL);
}
