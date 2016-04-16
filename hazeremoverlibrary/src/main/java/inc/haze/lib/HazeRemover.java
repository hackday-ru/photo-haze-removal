package inc.haze.lib;

public class HazeRemover {
    private static final float TRANSMISSION_THRESHOLD = 0.2f;
    private static final int MAX_ATMOSPHERE = 220;
    private static final int R = 0;
    private static final int G = 1;
    private static final int B = 2;
    private static final int CHANNELS = 3;
    private static final int DARK_CHANNEL_WINDOW_RADIUS = 7;
    private static final int GUIDED_FILTER_WINDOW_RADIUS = 40;
    private static final float OMEGA = 0.95f;
    private static final float EPS = 1e-3f;

    private float[][] channelMin(float[][][] source) {
        int height = source.length;
        int width = source[0].length;
        float[][] mins = new float[height][width];
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                mins[y][x] = Math.min(source[y][x][R], Math.min(source[y][x][G], source[y][x][B]));
            }
        }
        return mins;
    }

    private float[][] getDarkChannel(float[][] source, int windowRadius) {
        int height = source.length;
        int width = source[0].length;
        float[][] rowMins = new float[height][width];

        FloatMinQueue queue = new FloatMinQueue(Math.max(height, width));
        for (int y = 0; y < height; ++y) {
            queue.clear();
            for (int x = 0; x < windowRadius; ++x) {
                float darkness = source[y][x];
                queue.push(darkness);
            }
            for (int x = 0; x < width; ++x) {
                if (x - windowRadius > 0) {
                    float obsolete = source[y][x - windowRadius - 1];
                    queue.pop(obsolete);
                }
                if (x + windowRadius < width) {
                    float darkness = source[y][x + windowRadius];
                    queue.push(darkness);
                }
                rowMins[y][x] = queue.min();
            }
        }
        float[][] result = new float[height][width];
        for (int x = 0; x < width; ++x) {
            queue.clear();
            for (int y = 0; y < windowRadius; ++y) {
                float darkness = rowMins[y][x];
                queue.push(darkness);
            }
            for (int y = 0; y < height; ++y) {
                if (y - windowRadius > 0) {
                    float obsolete = rowMins[y - windowRadius - 1][x];
                    queue.pop(obsolete);
                }
                if (y + windowRadius < height) {
                    float darkness = rowMins[y + windowRadius][x];
                    queue.push(darkness);
                }
                result[y][x] = queue.min();
            }
        }
        return result;
    }

    private float[] getAtmosphere(float[][][] source, float[][] darkChannel) {
        float max = Float.MIN_VALUE; // todo replace with 5 percentile
        int maxX = -1, maxY = -1;
        int height = source.length;
        int width = source[0].length;
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                if (darkChannel[y][x] > max) {
                    max = darkChannel[y][x];
                    maxX = x;
                    maxY = y;
                }
            }
        }
        return source[maxY][maxX];
    }

    private float[][] getTransmission(float[][][] source, float[] atmosphere, float omega, int windowRadius) {
        int height = source.length;
        int width = source[0].length;

        float[][] darkChannelSource = new float[height][width];
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                float darkness = Float.MAX_VALUE;
                for (int c = 0; c < CHANNELS; ++c)
                    darkness = Math.min(darkness, source[y][x][c] / atmosphere[c]);
                darkChannelSource[y][x] = darkness;
            }
        }
        float[][] darkChannel = getDarkChannel(darkChannelSource, windowRadius);
        float[][] result = new float[height][width];
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                result[y][x] = 1 - omega * darkChannel[y][x];
            }
        }
        return result;
    }

    private float[][] mean(float[][] source, int windowRadius) {
        int height = source.length;
        int width = source[0].length;
        float[][] buf = new float[height][width];
        for (int y = 0; y < height; ++y) {
            buf[y][0] = source[y][0];
            for (int x = 1; x < width; ++x)
                buf[y][x] = buf[y][x - 1] + source[y][x];
        }
        for (int y = 1; y < height; ++y) {
            for (int x = 0; x < width; ++x)
                buf[y][x] = buf[y - 1][x] + buf[y][x];
        }
        float[][] result = new float[height][width];
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int leftExclusively = Math.max(-1, x - windowRadius - 1);
                int rightInclusively = Math.min(width - 1, x + windowRadius);
                int topExclusively = Math.max(-1, y - windowRadius - 1);
                int bottomInclusively = Math.min(height - 1, y + windowRadius);
                float sum = buf[bottomInclusively][rightInclusively];
                if (leftExclusively >= 0)
                    sum -= buf[bottomInclusively][leftExclusively];
                if (topExclusively >= 0)
                    sum -= buf[topExclusively][rightInclusively];
                if (leftExclusively >= 0 && topExclusively >= 0)
                    sum += buf[topExclusively][leftExclusively];
                int windowSize = (bottomInclusively - topExclusively) *
                        (rightInclusively - leftExclusively);
                result[y][x] = sum / windowSize;
            }
        }
        return result;
    }

    private float[][][] rotateDimensions(float[][][] source) {
        int height = source.length;
        int width = source[0].length;
        int depth = source[0][1].length;
        float[][][] result = new float[depth][height][width];
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                for (int z = 0; z < depth; ++z) {
                    result[z][y][x] = source[y][x][z];
                }
            }
        }
        return result;
    }

    private float[][] perElProduct(float[][] a, float[][] b) {
        int height = a.length;
        int width = a[0].length;
        float[][] result = new float[height][width];
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                result[y][x] = a[y][x] * b[y][x];
            }
        }
        return result;
    }

    private float[][] subtract(float[][] a, float[][] b) {
        int height = a.length;
        int width = a[0].length;
        float[][] result = new float[height][width];
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                result[y][x] = a[y][x] - b[y][x];
            }
        }
        return result;
    }

    private float[][] sum(float[][] a, float[][] b) {
        int height = a.length;
        int width = a[0].length;
        float[][] result = new float[height][width];
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                result[y][x] = a[y][x] + b[y][x];
            }
        }
        return result;
    }

    private float[][] inv3x3(float[][] m) { // todo optimize for symmetric matrix
        // computes the inverse of a matrix m
        float det = m[0][0] * (m[1][1] * m[2][2] - m[2][1] * m[1][2]) -
                m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0]) +
                m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);

        float invdet = 1 / det;

        float[][] minv = new float[3][3];
        minv[0][0] = (m[1][1] * m[2][2] - m[2][1] * m[1][2]) * invdet;
        minv[0][1] = (m[0][2] * m[2][1] - m[0][1] * m[2][2]) * invdet;
        minv[0][2] = (m[0][1] * m[1][2] - m[0][2] * m[1][1]) * invdet;
        minv[1][0] = (m[1][2] * m[2][0] - m[1][0] * m[2][2]) * invdet;
        minv[1][1] = (m[0][0] * m[2][2] - m[0][2] * m[2][0]) * invdet;
        minv[1][2] = (m[1][0] * m[0][2] - m[0][0] * m[1][2]) * invdet;
        minv[2][0] = (m[1][0] * m[2][1] - m[2][0] * m[1][1]) * invdet;
        minv[2][1] = (m[2][0] * m[0][1] - m[0][0] * m[2][1]) * invdet;
        minv[2][2] = (m[0][0] * m[1][1] - m[1][0] * m[0][1]) * invdet;
        return minv;
    }

    private float[][] guidedFilter(float[][][] guidance, float[][] guided, int windowRadius, float eps) {
        float[][][] guidanceMeans = new float[CHANNELS][][];
        float[][][] rotatedGuidance = rotateDimensions(guidance);
        for (int c = 0; c < CHANNELS; ++c) {
            guidanceMeans[c] = mean(rotatedGuidance[c], windowRadius);
        }
        float[][] guidedMean = mean(guided, windowRadius);
        float[][][] productMeans = new float[CHANNELS][][];
        for (int c = 0; c < CHANNELS; ++c) {
            productMeans[c] = mean(perElProduct(rotatedGuidance[c], guided), windowRadius);
        }
        float[][][] productCovariance = new float[CHANNELS][][];
        for (int c = 0; c < CHANNELS; ++c) {
            productCovariance[c] = subtract(productMeans[c], perElProduct(guidanceMeans[c], guidedMean));
        }

        float[][][][] var = new float[CHANNELS][CHANNELS][][];
        for (int i = 0; i < CHANNELS; ++i) {
            for (int j = i; j < CHANNELS; ++j) {
                var[i][j] = subtract(
                        mean(perElProduct(rotatedGuidance[i], rotatedGuidance[j]), windowRadius),
                        perElProduct(guidanceMeans[i], guidanceMeans[j])
                );
            }
        }

        int height = guidance.length;
        int width = guidance[0].length;
        float[][][] a = new float[CHANNELS][height][width];
        float[][] sigma = new float[3][3];
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                for (int i = 0; i < CHANNELS; ++i)
                    for (int j = i; j < CHANNELS; ++j)
                        sigma[i][j] = sigma[j][i] = var[i][j][y][x] + (i == j ? eps : 0);
                float[][] invSigma = inv3x3(sigma);
                float covR = productCovariance[R][y][x];
                float covG = productCovariance[G][y][x];
                float covB = productCovariance[B][y][x];
                for (int i = 0; i < CHANNELS; ++i)
                    a[i][y][x] = covR * invSigma[0][i] + covG * invSigma[1][i] + covB * invSigma[2][i];
            }
        }
        float[][] b = subtract(subtract(subtract(
                guidedMean,
                perElProduct(a[R], guidanceMeans[R])),
                perElProduct(a[G], guidanceMeans[G])),
                perElProduct(a[B], guidanceMeans[B]));
        return sum(sum(sum(
                perElProduct(mean(a[R], windowRadius), rotatedGuidance[R]),
                perElProduct(mean(a[G], windowRadius), rotatedGuidance[G])),
                perElProduct(mean(a[B], windowRadius), rotatedGuidance[B])),
                mean(b, windowRadius));
    }

    private float[][][] getRadiance(float[][][] source, float[] atmosphere, float[][] transmission) {
        int height = source.length;
        int width = source[0].length;
        float[][][] result = new float[height][width][CHANNELS];
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                for (int c = 0; c < CHANNELS; ++c) {
                    result[y][x][c] = (source[y][x][c] - atmosphere[c]) / transmission[y][x] + atmosphere[c]; // todo min by max
                }
            }
        }
        return result;
    }

    private int toColor(float value) {
        return Math.min(255, Math.max(0, Math.round(value)));
    }

    private int[] toColors(float[][][] source, int height, int width) {
        int[] colors = new int[height * width];
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                colors[y * width + x] = (toColor(source[y][x][R]) << 16) +
                        (toColor(source[y][x][G]) << 8) +
                        toColor(source[y][x][B]);
            }
        }
        return colors;
    }

    private float[][][] toHeatmap(float[][] depth, int height, int width) {
        float[][][] result = new float[height][width][CHANNELS];
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                result[y][x][0] = result[y][x][1] = result[y][x][2] = depth[y][x] * 255;
            }
        }
        return result;
    }

    private float[][][] toFloats(int[] pixels, int height, int width) {
        float[][][] source = new float[height][width][CHANNELS];
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int color = pixels[y * width + x];
                source[y][x][R] = ((color >>> 16) & 0xFF);
                source[y][x][G] = ((color >>> 8) & 0xFF);
                source[y][x][B] = (color & 0xFF);
            }
        }
        return source;
    }

    private float[][][] normalize(float[][][] source) {
        int height = source.length;
        int width = source[0].length;
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                min = Math.min(min, Math.min(source[y][x][R], Math.min(source[y][x][G], source[y][x][B])));
                max = Math.max(max, Math.max(source[y][x][R], Math.max(source[y][x][G], source[y][x][B])));
            }
        }
        float[][][] result = new float[height][width][CHANNELS];
        float length = Math.max(1, max - min);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                result[y][x][R] = (source[y][x][R] - min) / length;
                result[y][x][G] = (source[y][x][G] - min) / length;
                result[y][x][B] = (source[y][x][B] - min) / length;
            }
        }
        return result;
    }

    public DehazeResult dehaze(int[] pixels, int height, int width) {
        float[][][] source = toFloats(pixels, height, width);
        float[][] darknesses = channelMin(source);
        float[][] darkChannel = getDarkChannel(darknesses, DARK_CHANNEL_WINDOW_RADIUS);
        float[] atmosphere = getAtmosphere(source, darkChannel);
        for (int i = 0; i < CHANNELS; ++i)
            atmosphere[i] = Math.min(MAX_ATMOSPHERE, atmosphere[i]); // todo remove.. may be not...
        float[][] transmission = getTransmission(source, atmosphere, OMEGA, DARK_CHANNEL_WINDOW_RADIUS);
        for (int y = 0; y < height; ++y)
            for (int x = 0; x < width; ++x)
                transmission[y][x] = Math.max(transmission[y][x], TRANSMISSION_THRESHOLD); // todo threshold transmission remove?
        float[][] refinedTransmission = guidedFilter(normalize(source), transmission, GUIDED_FILTER_WINDOW_RADIUS, EPS);
        float[][][] radiance = getRadiance(source, atmosphere, refinedTransmission);
        return new DehazeResult(
                height,
                width,
                pixels,
                toColors(radiance, height, width),
                toColors(toHeatmap(refinedTransmission, height, width), height, width)
        );
    }

    private static class FloatMinQueue {
        // http://e-maxx.ru/algo/stacks_for_minima

        private final float[] queue;
        private int head, tail;

        FloatMinQueue(int maxSize) {
            this.queue = new float[maxSize];
        }

        void push(float value) {
            while (tail > head && queue[tail - 1] > value)
                tail--;
            queue[tail++] = value;
        }

        void pop(float value) {
            if (tail > head && queue[head] == value)
                head++;
        }

        float min() {
            return queue[head];
        }

        void clear() {
            head = tail = 0;
        }
    }
}