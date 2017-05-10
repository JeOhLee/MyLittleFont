package kr.ac.kaist.team888.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class FeatureController {
  private ArrayList<WeakReference> listeners;
  private Comparator<WeakReference> comparator = new Comparator<WeakReference>() {
    @Override
    public int compare(WeakReference o1, WeakReference o2) {
      if (o1.get() == null || o2.get() == null) {
        return -1;
      }

      return ((OnFeatureChangeListener)(o1.get())).getPriority()
          - ((OnFeatureChangeListener)(o2.get())).getPriority();
    }
  };

  /**
   * Interface for listening change of hangul features.
   *
   * <p> If some feature(weight, curve, and etc)'s value are change,
   * {@link FeatureController} calls {@link #onFeatureChange()}.
   *
   */
  public interface OnFeatureChangeListener {
    /**
     * Method that will be executed on feature's value change.
     */
    void onFeatureChange();

    /**
     * Priority of listener.
     *
     * <p> The lower priority executed first.
     *
     * @return priority as integer
     */
    int getPriority();
  }

  private static class Singleton {
    private static final FeatureController instance = new FeatureController();
  }

  /**
   * Getter of singleton instance.
   *
   * @return singleton instance
   */
  public static FeatureController getInstance() {
    return FeatureController.Singleton.instance;
  }

  private FeatureController() {
    listeners = new ArrayList<>();
  }

  /**
   * Register {@link OnFeatureChangeListener}.
   *
   * @param listener listener to register
   */
  public void registerOnFeatureChangeListener(OnFeatureChangeListener listener) {
    listeners.add(new WeakReference<>(listener));
    Collections.sort(listeners, comparator);
  }

  private void onFeatureChange() {
    for (int i = 0; i < listeners.size(); i++) {
      if (listeners.get(i).get() != null) {
        ((OnFeatureChangeListener)listeners.get(i).get()).onFeatureChange();
      } else {
        listeners.remove(i);
        i--;
      }
    }
  }
}