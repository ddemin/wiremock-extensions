package com.github.ddemin.wext.util;

import com.github.ddemin.wext.model.ResponseRule;
import org.apache.commons.lang3.RandomUtils;

import static org.apache.commons.lang3.RandomUtils.nextLong;

public class ExWiremockUtils {

    public static long getStatisticDelayForRule(final ResponseRule nextRule) {
        final int rnd100 = RandomUtils.nextInt(1, 101);
        if (rnd100 == 100) {
            return nextRule.getDelayMax();
        } else if (rnd100 >= 95) {
            return nextLong(nextRule.getDelay95(), nextRule.getDelayMax());
        } else if (rnd100 >= 90) {
            return nextLong(nextRule.getDelay90(), nextRule.getDelay95());
        } else if (rnd100 >= 50) {
            return nextLong(nextRule.getDelay50(), nextRule.getDelay90());
        } else if (rnd100 >= 1) {
            return nextLong(nextRule.getDelayMin(), nextRule.getDelay50());
        } else {
            return nextRule.getDelayMin();
        }
    }

}
