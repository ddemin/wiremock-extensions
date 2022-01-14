package com.github.ddemin.wext.util;

import com.github.ddemin.wext.model.ResponseRule;
import org.apache.commons.lang3.RandomUtils;

public class ExWiremockUtils {

    public static long getStatisticDelayForRule(final ResponseRule nextRule) {
        final int rnd100 = RandomUtils.nextInt(1, 101);
        if (rnd100 > 95) {
            return calcRndValue(rnd100, 95, 100, nextRule.getDelay95(), nextRule.getDelayMax());
        } else if (rnd100 > 90) {
            return calcRndValue(rnd100, 90, 95, nextRule.getDelay90(), nextRule.getDelay95());
        } else if (rnd100 > 50) {
            return calcRndValue(rnd100, 50, 90, nextRule.getDelay50(), nextRule.getDelay90());
        } else if (rnd100 > 1) {
            return calcRndValue(rnd100, 1, 50, nextRule.getDelayMin(), nextRule.getDelay50());
        } else {
            return nextRule.getDelayMin();
        }
    }

    private static long calcRndValue(
            long rndLevel,
            long curLevel,
            long nextLevel,
            double currentLevelValue,
            double nextLevelValue
    ) {
        double deltaModRnd = (rndLevel - curLevel)/(double) (nextLevel - curLevel);
        double deltaRange = (nextLevelValue - currentLevelValue);
        double deltaValueRnd = deltaRange * deltaModRnd;
        return (long) (currentLevelValue + deltaValueRnd);

    }

}
