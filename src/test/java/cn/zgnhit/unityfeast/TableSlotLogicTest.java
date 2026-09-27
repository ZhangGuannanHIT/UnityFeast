package cn.zgnhit.unityfeast;

import cn.zgnhit.unityfeast.block.TableSlotLogic;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TableSlotLogicTest {
    @Test void allMasksAndCornersConserveItems() {
        for (int mask = 0; mask < 16; mask++) for (int start = 0; start < 4; start++) {
            int put = TableSlotLogic.find(mask, start, false);
            int take = TableSlotLogic.find(mask, start, true);
            if (mask == 15) assertEquals(-1, put);
            else {
                assertTrue(put >= 0 && put < 4);
                assertEquals(0, mask & (1 << put));
                assertEquals(Integer.bitCount(mask)+1, Integer.bitCount(mask | (1 << put)));
                for (int step=0; step<((put-start+4)%4); step++) assertNotEquals(0, mask & (1<<((start+step)%4)));
            }
            if (mask == 0) assertEquals(-1, take);
            else {
                assertNotEquals(0, mask & (1 << take));
                assertEquals(Integer.bitCount(mask)-1, Integer.bitCount(mask & ~(1 << take)));
            }
        }
    }
    @Test void cornersMatchWorldCoordinates() {
        assertEquals(0, TableSlotLogic.corner(.2,.2));
        assertEquals(1, TableSlotLogic.corner(.8,.2));
        assertEquals(2, TableSlotLogic.corner(.8,.8));
        assertEquals(3, TableSlotLogic.corner(.2,.8));
        assertEquals(2, TableSlotLogic.corner(.5,.5));
    }
}
