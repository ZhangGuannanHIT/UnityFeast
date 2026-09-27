package cn.zgnhit.unityfeast.block;

/** Clockwise, viewed from above: NW, NE, SE, SW. Pure and exhaustively tested. */
public final class TableSlotLogic {
    private TableSlotLogic() {}
    public static int corner(double x, double z) { return z < 0.5 ? (x < 0.5 ? 0 : 1) : (x < 0.5 ? 3 : 2); }
    public static int find(int mask, int start, boolean occupied) {
        for (int step = 0; step < 4; step++) {
            int slot = (start + step) & 3;
            if (((mask & (1 << slot)) != 0) == occupied) return slot;
        }
        return -1;
    }
}
