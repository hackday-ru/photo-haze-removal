package inc.haze.lib;

/**
 * Created by nikita on 16.04.16.
 */
public class DehazeResult {
    private final int height;
    private final int width;
    private final int[] source;
    private final int[] result;
    private final int[] depth;

    public DehazeResult(int height, int width, int[] source, int[] result, int[] depth) {
        this.height = height;
        this.width = width;
        this.source = source;
        this.result = result;
        this.depth = depth;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int[] getSource() {
        return source;
    }

    public int[] getResult() {
        return result;
    }

    public int[] getDepth() {
        return depth;
    }
}
