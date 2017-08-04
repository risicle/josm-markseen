package org.openstreetmap.josm.plugins.markseen;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.bbox.SlippyMapBBoxChooser;


public class MarkSeenSlippyMapBBoxChooser extends SlippyMapBBoxChooser {
    public MarkSeenSlippyMapBBoxChooser(QuadTreeMeta quadTreeMeta_) {
        this.tileController = new MarkSeenTileController(
            quadTreeMeta_,
            this.tileController.getTileSource(),
            this.tileController.getTileCache(),
            this
        );
    }

    public void markBoundsSeen(Bounds bbox, double minTilesAcross) {
        ((MarkSeenTileController) this.tileController).markBoundsSeen(bbox, minTilesAcross);
    }
}
