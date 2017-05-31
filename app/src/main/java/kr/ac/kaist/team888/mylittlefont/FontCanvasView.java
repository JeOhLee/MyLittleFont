package kr.ac.kaist.team888.mylittlefont;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import kr.ac.kaist.team888.locator.Locator;
import kr.ac.kaist.team888.region.Region;
import kr.ac.kaist.team888.util.FeatureController;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;
import java.util.Collection;

public class FontCanvasView extends View implements FeatureController.OnFeatureChangeListener {
  private static final float CANVAS_OFFSET_RATIO = 0.05f;
  private static final float FIXED_POINT_RADIUS = 4f;
  private static final float CONTROL_POINT_RADIUS = 4f;
  private static final int PRIORITY = 2;
  private static final int FONT_SIZE_MAX = 160;
  private static final int FONT_SIZE_MIN = 24;
  private Paint contourPaint;
  private Paint contourLayoutPaint;
  private Paint skeletonPaint;
  private Paint fixedPaint;
  private Paint controlPaint;
  private Collection<Locator> locators;
  private ArrayList<Region> regions;
  private int fontSize = 72;
  private double lineMargin = 0.15;

  private Region canvasRegion = new Region(0, 0, 0, 0);

  private boolean skeletonView = false;

  public FontCanvasView(Context context) {
    super(context);
    initialize();
  }

  public FontCanvasView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public FontCanvasView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    initialize();
  }

  private void initialize() {
    regions = new ArrayList<>();

    contourPaint = new Paint();
    contourPaint.setColor(Color.BLACK);
    contourPaint.setStyle(Paint.Style.FILL);
    contourPaint.setStrokeWidth(1f);

    contourLayoutPaint = new Paint();
    contourLayoutPaint.setColor(Color.LTGRAY);
    contourLayoutPaint.setStyle(Paint.Style.STROKE);
    contourLayoutPaint.setStrokeWidth(1f);

    skeletonPaint = new Paint();
    skeletonPaint.setColor(Color.RED);
    skeletonPaint.setStyle(Paint.Style.STROKE);
    skeletonPaint.setStrokeWidth(3f);
    skeletonPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

    fixedPaint = new Paint();
    fixedPaint.setColor(Color.BLUE);
    fixedPaint.setStyle(Paint.Style.FILL);
    fixedPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

    controlPaint = new Paint();
    controlPaint.setColor(Color.GREEN);
    controlPaint.setStyle(Paint.Style.FILL);
    controlPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

    FeatureController.getInstance().registerOnFeatureChangeListener(this);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    float width = getWidth();
    float height = getHeight();

    if (width > height) {
      float offset = height * CANVAS_OFFSET_RATIO;
      canvasRegion.setMinX((width - height) / 2 + offset);
      canvasRegion.setMaxX((width + height) / 2 - offset);
      canvasRegion.setMinY(height - offset);
      canvasRegion.setMaxY(0 + offset);
    } else {
      float offset = width * CANVAS_OFFSET_RATIO;
      canvasRegion.setMinX(0 + offset);
      canvasRegion.setMaxX(width - offset);
      canvasRegion.setMinY((height + width) / 2 - offset);
      canvasRegion.setMaxY((height - width) / 2 + offset);
    }
  }

  @Override
  public void onDraw(Canvas canvas) {
    if (locators == null || locators.isEmpty()) {
      return;
    }

    calculateRegions();

    int count = 0;
    for (Locator locator : locators) {
      locator.invalidate(regions.get(count));
      if (skeletonView) {
        for (Path path : locator.getContourPaths()) {
          canvas.drawPath(path, contourLayoutPaint);
        }

        for (Path path : locator.getSkeletonPaths()) {
          canvas.drawPath(path, skeletonPaint);
        }

        for (Vector2D fixed : locator.getFixedCircles()) {
          canvas.drawCircle((float) fixed.getX(), (float) fixed.getY(),
              FIXED_POINT_RADIUS, fixedPaint);
        }

        for (Vector2D control : locator.getControlCircles()) {
          canvas.drawCircle((float) control.getX(), (float) control.getY(),
              CONTROL_POINT_RADIUS, controlPaint);
        }
      } else {
        for (Path path : locator.getContourPaths()) {
          canvas.drawPath(path, contourPaint);
        }
      }
      count++;
    }
  }

  /**
   * Draw given locators.
   *
   * @param locators locators to draw.
   */
  public void drawLocators(Collection<Locator> locators) {
    this.locators = locators;
    invalidate();
  }

  private void calculateRegions() {
    regions.clear();

    // ratio = (x2 - x1) / (y2 - y1)
    double regionRatio = (Locator.locatorRegion.getMaxX() - Locator.locatorRegion.getMinX())
        / (Locator.locatorRegion.getMaxY() - Locator.locatorRegion.getMinY());

    // getting one region's size
    double gap = FeatureController.getInstance().getGap();
    int regionHeight = ptToCanvas(fontSize);
    int regionWidth = (int) (regionHeight * regionRatio);

    int gapWidthSize = (int) (regionWidth * gap);
    int gapHeightSize = (int) (regionHeight * lineMargin);

    int maxCol = Math.max(1, getWidth() / (regionWidth + gapWidthSize));
    if (maxCol == 1) {
      regionWidth = getWidth();
      regionHeight = (int) (regionWidth / regionRatio);
      if (regionHeight > getHeight()) {
        regionHeight = getHeight();
        regionWidth = (int) (regionHeight * regionRatio);
      }
      gapWidthSize = 0;
    }

    int locatorsCount = locators.size();
    int rowCount = (int) (Math.ceil(locatorsCount / (double) maxCol));

    // getting base position of y
    int heightBase = (int) (getHeight() / 2 - (rowCount / 2.0 * (regionHeight + gapHeightSize)));
    if (heightBase < 0) {
      heightBase = 0;
    }

    for (int i = 0; i < rowCount; i++) {
      int curHeight = heightBase + (regionHeight + gapHeightSize) * i;

      int colCount;
      if (locatorsCount > maxCol) {
        colCount = maxCol;
        locatorsCount -= maxCol;
      } else {
        colCount = locatorsCount;
        locatorsCount = 0;
      }

      int widthBase = (int) (getWidth() / 2 - (colCount / 2.0) * (regionWidth + gapWidthSize));

      for (int j = 0; j < colCount; j++) {
        int curWidth = widthBase + (regionWidth + gapWidthSize) * j;
        Region region = new Region(curWidth, curWidth + regionWidth,
            curHeight + regionHeight, curHeight);
        regions.add(region);
      }
    }
  }

  private int ptToCanvas(double point) {
    double inches = point / 72;
    double xdpi = getResources().getDisplayMetrics().xdpi;
    return (int)(inches * xdpi);
  }

  /**
   * Set font size.
   *
   * <p> Minimum font size is 24pt and maximum font size is 160pt.
   *
   * @param value font size with in range 0 ~ 1
   *
   * @return actual size of font
   */
  public int setFontSize(double value) {
    fontSize = (int)((FONT_SIZE_MAX - FONT_SIZE_MIN) * value + FONT_SIZE_MIN);
    invalidate();
    return fontSize;
  }

  /**
   * Set viewing skeleton lines on/off.
   *
   * <p> On skeleton view - show skeleton curves of character with lines and points.
   * <br> Off skeleton view - show filled font.
   *
   * @param on on/off skeleton view
   */
  public void viewSkeleton(boolean on) {
    skeletonView = on;

    invalidate();
  }

  @Override
  public void onFeatureChange() {
    invalidate();
  }

  @Override
  public int getPriority() {
    return PRIORITY;
  }
}
