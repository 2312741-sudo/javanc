package vn.edu.dlu.dhopm.event;

import vn.edu.dlu.dhopm.model.PatternResult;
import java.util.List;

/**
 * Observer interface lang nghe su kien khi tien trinh khai pha DFS hoan tat.
 */
public interface MiningListener {
    void onMiningComplete(List<PatternResult> allVisited, List<PatternResult> dhops, long runtimeNs);
}
