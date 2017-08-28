package org.openstreetmap.josm.plugins.markseen;

import java.io.IOException;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.awt.Color;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


@RunWith(Parameterized.class)
public class QuadTreeMetaClearOrderTest extends BaseQuadTreeMetaTest {
    private static final int seenRectVariants = 8;
    private static final int referenceTileVariants = 2;

    @Parameters(name="{index}-scenario-{0}-seeds-{1}-{2}")
    public static Collection<Object[]> getParameters() throws IOException {
        ArrayList<Object[]> paramSets = new ArrayList<Object[]>();
        Object[][] scenarios = getTestScenarios();
        for (int i=0; i<scenarios.length; i++) {
            Object[] seenRects = (Object[])scenarios[i][1];
            Object[] referenceTiles = (Object[])scenarios[i][2];

            // we'd rather avoid testing against more permutations than exist for the number of seenRects
            int srFact = 1;
            for(int m=1; m<=seenRects.length; m++) {
                srFact = srFact*m;
            }

            // we'd rather avoid testing against more permutations than exist for the number of referenceTiles
            int rtFact = 1;
            for(int m=1; m<=referenceTiles.length; m++) {
                rtFact = rtFact*m;
            }

            for (int j=0; j<Math.min(srFact, seenRectVariants); j++) {
                for (int k=0; k<Math.min(rtFact, referenceTileVariants); k++) {
                    paramSets.add(new Object[] {i, j, k});
                }
            }
        }
        return paramSets;
    }

    public QuadTreeMetaClearOrderTest(int scenarioIndex_, Integer seenRectOrderSeed_, Integer referenceTileOrderSeed_)
    throws IOException {
        super(scenarioIndex_, seenRectOrderSeed_, referenceTileOrderSeed_);
    }

    @Test(timeout=10000)
    public void testClearUnseen()
    throws java.lang.InterruptedException, java.util.concurrent.ExecutionException {
        QuadTreeMeta quadTreeMeta = new QuadTreeMeta(this.tileSize, Color.PINK, 0.5);
        QuadTreeNodeDynamicReference[] dynamicReferences = createDynamicReferences(quadTreeMeta, this.referenceTiles);

        this.markRectsAsync(quadTreeMeta, this.seenRects, this.seenRectOrderSeed);
        quadTreeMeta.requestClear(true);

        // wait until the edits have properly started
        while (quadTreeMeta.getEditRequestQueueCompletedTaskCount() == 0);

        final ExecutorService executor = Executors.newFixedThreadPool(4);
        final List<Future<Object>> maskFutures = this.fetchTileMasksAsync(
            quadTreeMeta,
            dynamicReferences,
            executor,
            this.referenceTileOrderSeed
        );

        byte[] blankMaskBytes = getRefMaskBytes(quadTreeMeta, false);
        for (int i = 0; i < maskFutures.size(); i++) {
            System.out.format("(%d of %d) Checking reference tile %d\n", i, this.referenceTiles.length, i);
            byte[] resultMaskBytes = getRefMaskBytes(quadTreeMeta, maskFutures.get(i).get());
            try {
                assertArrayEquals(
                    resultMaskBytes,
                    blankMaskBytes
                );
            } catch (final AssertionError e) {
                System.out.format("assertArrayEquals failed on reference tile %d\n", i);
                System.out.println(javax.xml.bind.DatatypeConverter.printHexBinary(resultMaskBytes));
                throw e;
            }
        }
    }
}