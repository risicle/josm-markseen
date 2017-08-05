package org.openstreetmap.josm.plugins.markseen;

import java.lang.Runnable;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;

import org.openstreetmap.josm.data.Bounds;

public class QuadTreeMeta {
    // is there a better way to create a properly read-only BufferedImage? don't know. for now, all we want to be able
    // to do is catch potential bugs where something attempts to write to a mask that is intended to be shared &
    // constant
    private static class WriteInhibitedBufferedImage extends BufferedImage {
        public boolean inhibitWrites = false;

        public WriteInhibitedBufferedImage(int width, int height, int imageType, IndexColorModel cm, Color constColor) {
            super(width, height, imageType, cm);
            Graphics2D g = this.createGraphics();
            g.setBackground(constColor);
            g.clearRect(0, 0, width, height);
            this.inhibitWrites = true;
        }

        @Override
        public Graphics2D createGraphics() {
            if (this.inhibitWrites) {
                throw new RuntimeException("Attempt to draw to WriteInhibitedBufferedImage with inhibitWrites set");
            } else {
                return super.createGraphics();
            }
        }

        @Override
        public WritableRaster getWritableTile(int tileX, int tileY) {
            if (this.inhibitWrites) {
                throw new RuntimeException("Attempt to draw to WriteInhibitedBufferedImage with inhibitWrites set");
            } else {
                return super.getWritableTile(tileX, tileY);
            }
        }
    }

    private class MarkBoundsSeenRequest implements Runnable {
        private final Bounds bounds;
        private final double minTilesAcross;

        public MarkBoundsSeenRequest(Bounds bounds_, double minTilesAcross_) {
            this.bounds = bounds_;
            this.minTilesAcross = minTilesAcross_;
        }

        public void run() {
            QuadTreeMeta.this.quadTreeRWLock.writeLock().lock();
            QuadTreeMeta.this.quadTreeRoot.markBoundsSeen(this.bounds, this.minTilesAcross);
            QuadTreeMeta.this.quadTreeRWLock.writeLock().unlock();
        }
    }
 
    protected final static Color UNMARK_COLOR = new Color(0, 0, 0, 0);
    protected final static Color MARK_COLOR = new Color(255, 255, 255, 255);

    public final ReentrantReadWriteLock quadTreeRWLock = new ReentrantReadWriteLock();

    protected IndexColorModel maskColorModel;

    protected final int tileSize;

    protected final BufferedImage EMPTY_MASK;
    protected final BufferedImage FULL_MASK;

    private final Executor boundsMarkingExecutor;

    public QuadTreeNode quadTreeRoot;

    public QuadTreeMeta(int tileSize_) {
        this.tileSize = tileSize_;
        this.maskColorModel = new IndexColorModel(
            1,
            2,
            new byte[]{(byte)0, (byte)255},
            new byte[]{(byte)0, (byte)0},
            new byte[]{(byte)0, (byte)255},
            new byte[]{(byte)0, (byte)128}
        );

        this.EMPTY_MASK = new WriteInhibitedBufferedImage(
            this.tileSize,
            this.tileSize,
            BufferedImage.TYPE_BYTE_BINARY,
            this.maskColorModel,
            UNMARK_COLOR
        );
        this.FULL_MASK = new WriteInhibitedBufferedImage(
            this.tileSize,
            this.tileSize,
            BufferedImage.TYPE_BYTE_BINARY,
            this.maskColorModel,
            MARK_COLOR
        );

        this.quadTreeRoot = new QuadTreeNode(this);
        this.boundsMarkingExecutor = Executors.newSingleThreadExecutor();
    }

    public void requestSeenBoundsMark(Bounds bounds, double minTilesAcross) {
        this.boundsMarkingExecutor.execute(new MarkBoundsSeenRequest(bounds, minTilesAcross));
    }
}
