package org.openstreetmap.josm.plugins.markseen;

import java.io.IOException;
import java.lang.Boolean;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.locks.ReentrantReadWriteLock;


import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;

import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.josm.data.Bounds;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;
import org.junit.Rule;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;


@RunWith(Parameterized.class)
public class QuadTreeNodeTest extends BaseTest {
    @Rule public MockitoRule rule = MockitoJUnit.rule();

    @Mock private MarkSeenTileController tileController;
    @Mock private TileSource tileSource;

    private IndexColorModel indexColorModel;
    private BufferedImage EMPTY_MASK;
    private BufferedImage FULL_MASK;

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // parametrized variables
    private int tileSize;
    private Object[][] seenRects;
    private Object[][] referenceTiles;

    @Parameters
    public static Collection<Object[]> getParameters() throws IOException {
        return Arrays.asList(new Object[][] {
            {
                256,
                new Object [][] {
                    // bounds, minTilesWidth
                    { new Bounds(51.36, -0.35, 51.61, 0.10), 4. }
                },
                new Object[][] {
                    // zoom, xtile, ytile, expectedMask
                    // the expectedMask can either be a byte[] OR a boolean, true denoting FULL_MASK and false
                    // EMPTY_MASK (we can't reference these directly because they haven't been allocated at this point)
                    { 10, 50, 50, false },
                    { 15, 16365, 10867, false },
                    { 14, 8184, 5448, true },
                    { 11, 1023, 681, true },
                    { 16, 32730, 21762, byteArrayFromResource("QuadTreeNodeTest/testSingleRect/0/16-32730-21762.bin") },
                    { 11, 1024, 680, byteArrayFromResource("QuadTreeNodeTest/testSingleRect/0/11-1024-680.bin") },
                    { 7, 63, 42, byteArrayFromResource("QuadTreeNodeTest/testSingleRect/0/7-63-42.bin") }
                }
            },
            {
                256,
                new Object [][] {
                    { new Bounds(-24.68, -48.99, -14.9, -45.35), 4. },
                    { new Bounds(-19.39, -52.734375, -17.47, -45.6591797), 6. },
                    { new Bounds(-20.365, -51.152, -20.014, -50.581), 3. }
                },
                new Object[][] {
                    { 10, 50, 50, false },
                    { 3, 1, 4, false },
                    { 10, 375, 559, true },
                    { 15, 12018, 17916, true },
                    { 4, 5, 8, byteArrayFromResource("QuadTreeNodeTest/testSingleRect/1/4-5-8.bin") },
                    { 11, 749, 1119, true },
                    { 12, 1490, 2272, byteArrayFromResource("QuadTreeNodeTest/testSingleRect/1/12-1490-2272.bin") },
                    { 14, 5960, 9091, byteArrayFromResource("QuadTreeNodeTest/testSingleRect/1/14-5960-9091.bin") },
                    { 14, 5960, 9092, false },
                    { 0, 0, 0, byteArrayFromResource("QuadTreeNodeTest/testSingleRect/1/0-0-0.bin") }
                }
            },
            {
                127,
                new Object [][] {
                    { new Bounds(0., 0., 20., 20.), 7. },
                    { new Bounds(-10., -10., 0., 0.), 5. },
                    { new Bounds(0., 90., 10., 100.), 4. },
                    { new Bounds(-9., 81., 0., 90.), 9. }
                },
                new Object[][] {
                    { 10, 50, 50, false },
                    { 10, 512, 512, false },
                    { 10, 511, 511, false },
                    { 10, 512, 511, true },
                    { 10, 511, 512, true },
                    { 7, 96, 64, false },
                    { 7, 95, 63, false },
                    { 7, 96, 63, true },
                    { 7, 95, 64, true },
                    { 0, 0, 0, byteArrayFromResource("QuadTreeNodeTest/testSingleRect/2/0-0-0.bin") }
                }
            },
            {
                200,
                new Object [][] {
                    { new Bounds(80., 12.8, 88., 25.6), 7. },
                    { new Bounds(82., 25.6, 89., 26.6), 3. },
                    { new Bounds(60., -67., 89., -66.), 3. },
                    { new Bounds(-86.9, 10.6, -70., 21.6), 8. }
                },
                new Object[][] {
                    { 10, 50, 50, false },
                    { 10, 0, 0, false },
                    { 18, 140500, 0, true },
                    { 12, 1286, 10, true },
                    { 0, 0, 0, byteArrayFromResource("QuadTreeNodeTest/testSingleRect/3/0-0-0.bin") }
                }
            }
        });
    }

    public QuadTreeNodeTest(int tileSize_, Object[][] seenRects_, Object[][] referenceTiles_) {
        this.tileSize = tileSize_;
        this.seenRects = seenRects_;
        this.referenceTiles = referenceTiles_;
    }

    @Before
    public void setUp() {
        Mockito.when(this.tileSource.getTileSize()).thenReturn(this.tileSize);

        this.indexColorModel = new IndexColorModel(
            1,
            2,
            new byte[]{(byte)0, (byte)255},
            new byte[]{(byte)0, (byte)0},
            new byte[]{(byte)0, (byte)255},
            new byte[]{(byte)0, (byte)128}
        );
        this.EMPTY_MASK = new MarkSeenTileController.WriteInhibitedBufferedImage(
            tileSize,
            tileSize,
            BufferedImage.TYPE_BYTE_BINARY,
            this.indexColorModel,
            new Color(0,0,0,0)
        );
        this.FULL_MASK = new MarkSeenTileController.WriteInhibitedBufferedImage(
            tileSize,
            tileSize,
            BufferedImage.TYPE_BYTE_BINARY,
            this.indexColorModel,
            new Color(255,255,255,255)
        );

        Mockito.when(this.tileController.getTileSource()).thenReturn(this.tileSource);
        Mockito.when(this.tileController.getMaskColorModel()).thenReturn(this.indexColorModel);
        Mockito.when(this.tileController.getEmptyMask()).thenReturn(this.EMPTY_MASK);
        Mockito.when(this.tileController.getFullMask()).thenReturn(this.FULL_MASK);
        Mockito.when(this.tileController.getQuadTreeRWLock()).thenReturn(this.lock);
    }

    @Test
    public void test() {
        QuadTreeNode quadTreeRoot = new QuadTreeNode(this.tileController);
        this.lock.writeLock().lock();

        for (int i = 0; i<this.seenRects.length; i++) {
            Object[] seenRectInfo = this.seenRects[i];
            System.out.format("Marking seen rect %d\n", i);
            Bounds bounds = (Bounds)seenRectInfo[0];
            double minTilesWidth = (double)seenRectInfo[1];

            quadTreeRoot.markRectSeen(bounds, minTilesWidth, this.tileController);
            quadTreeRoot.checkIntegrity();
        }

        for (int i = 0; i<this.referenceTiles.length; i++) {
            Object[] referenceTileInfo = this.referenceTiles[i];
            System.out.format("Checking reference tile %d\n", i);
            int zoom = (int)referenceTileInfo[0];
            int tilex = (int)referenceTileInfo[1];
            int tiley = (int)referenceTileInfo[2];
            byte[] maskBytes = Boolean.class.isInstance(referenceTileInfo[3]) ?
                (
                    (DataBufferByte) (
                        ((boolean)referenceTileInfo[3]) ?
                                      this.FULL_MASK :
                                      this.EMPTY_MASK
                    ).getData().getDataBuffer()
                ).getData() :
                (byte[])referenceTileInfo[3];

            BufferedImage result = quadTreeRoot.getNodeForTile(
                tilex,
                tiley,
                zoom,
                true,
                this.tileController
            ).getMask(true, this.tileController);
            quadTreeRoot.checkIntegrity();

            System.out.println(javax.xml.bind.DatatypeConverter.printHexBinary(
                ((DataBufferByte) result.getData().getDataBuffer()).getData())
            );

            assertArrayEquals(
                ((DataBufferByte) result.getData().getDataBuffer()).getData(),
                maskBytes
            );
        }

        this.lock.writeLock().unlock();
    }
}
