package inc.haze.lib;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by nikita on 16.04.16.
 */
public class DehazeExecutable {
    public static void main(String[] args) throws IOException {
        HazeRemover remover = new HazeRemover(new GuidedFilter(), 3000, 3000);

        Files.newDirectoryStream(Paths.get("examples"), "*.{jpg,png}").forEach(path -> {
            try {
                BufferedImage image = ImageIO.read(path.toFile());
                int height = image.getHeight();
                int width = image.getWidth();
                int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
                DehazeResult dehaze = remover.dehaze(pixels, height, width);

                BufferedImage dehazed = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                dehazed.setRGB(0, 0, width, height, dehaze.getResult(), 0, width);
                ImageIO.write(dehazed, "PNG", path.resolveSibling(path.getFileName() + "-output.png").toFile());

                BufferedImage depth = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                depth.setRGB(0, 0, width, height, dehaze.getDepth(), 0, width);
                ImageIO.write(depth, "PNG", path.resolveSibling(path.getFileName() + "-depth.png").toFile());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}
