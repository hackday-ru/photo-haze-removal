package inc.haze.lib;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Created by nikita on 16.04.16.
 */
public class DehazeExecutable {
    public static void main(String[] args) throws IOException {
        BufferedImage image = ImageIO.read(DehazeExecutable.class.getResourceAsStream("/forest.png"));
        int height = image.getHeight();
        int width = image.getWidth();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        DehazeResult dehaze = new HazeRemover().dehaze(pixels, height, width);

        BufferedImage dehazed = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        dehazed.setRGB(0, 0, width, height, dehaze.getResult(), 0, width);
        ImageIO.write(dehazed, "PNG", new File("test-output.png"));

        BufferedImage depth = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        depth.setRGB(0, 0, width, height, dehaze.getDepth(), 0, width);
        ImageIO.write(depth, "PNG", new File("test-depth.png"));
    }
}
