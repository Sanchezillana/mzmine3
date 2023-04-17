/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.impl;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.nio.DoubleBuffer;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An implementation of MassSpectrum that stores the data points in a MemoryMapStorage.
 */
public abstract class AbstractStorableSpectrum extends AbstractMassSpectrum {

  private static final Logger logger = Logger.getLogger(AbstractStorableSpectrum.class.getName());
  private static final DoubleBuffer EMPTY_BUFFER = DoubleBuffer.wrap(new double[0]);

  protected DoubleBuffer mzValues;
  protected DoubleBuffer intensityValues;

  /**
   * Note: mz and intensity values for a scan shall only be set once and are enforced to be
   * immutable thereafter. These values shall ideally be set during instantiation of the given
   * object. However, if a the object represents an artificially generated object, e.g. a
   * {@link io.github.mzmine.datamodel.Frame} which is generated by merging multiple scans at the
   * same retention time, values can be set at a later stage, but are immutable thereafter.
   *
   * @param storage         If null, mz and intensity values will be stored in ram.
   * @param mzValues        If null, no values will be stored and values can be set at a later stage
   *                        by the implementing class. (e.g. {@link SimpleFrame}.
   * @param intensityValues If null, no values will be stored and values can be set at a later stage
   *                        by the implementing class. (e.g. {@link SimpleFrame}.
   */
  public AbstractStorableSpectrum(@Nullable MemoryMapStorage storage, @Nullable double[] mzValues,
      @Nullable double[] intensityValues) {
    setDataPoints(storage, mzValues, intensityValues);
  }

  public AbstractStorableSpectrum(@Nullable DoubleBuffer mzValues,
      @Nullable DoubleBuffer intensityValues) {
    this.mzValues = mzValues;
    this.intensityValues = intensityValues;
    //todo transfer checks
    onDataChangedEvent();
  }

  protected synchronized void setDataPoints(@Nullable MemoryMapStorage storage,
      @Nullable double[] mzValues, @Nullable double[] intensityValues) {

    if (mzValues == null && intensityValues == null) {
      return;
    }

    assert mzValues.length == intensityValues.length;
    // values shall not be reset, but can be set at a later stage
    if (!(this instanceof Frame)) {
      // allow re-generation of frame spectra
      assert this.mzValues == null;
      assert this.intensityValues == null;
    }

    // so many data sources have unsorted spectra - so better sort the spectrum here
    // this is only done if the mzs were unsorted
    var mzsIntensities = DataPointUtils.ensureSortingMzAscendingDefault(mzValues, intensityValues);

    this.mzValues = StorageUtils.storeValuesToDoubleBuffer(storage, mzsIntensities[0]);
    this.intensityValues = StorageUtils.storeValuesToDoubleBuffer(storage, mzsIntensities[1]);
    onDataChangedEvent();
  }

  private void onDataChangedEvent() {
    updateMzRangeAndTICValues();
  }

  DoubleBuffer getMzValues() {
    if (mzValues == null) {
      return EMPTY_BUFFER;
    } else {
      return mzValues;
    }
  }

  DoubleBuffer getIntensityValues() {
    if (intensityValues == null) {
      return EMPTY_BUFFER;
    } else {
      return intensityValues;
    }
  }

  @Override
  public double[] getMzValues(@NotNull double[] dst) {
    if (mzValues == null) {
      return new double[0];
    }
    if (dst.length < getNumberOfDataPoints()) {
      dst = new double[getNumberOfDataPoints()];
    }
    mzValues.get(0, dst, 0, getNumberOfDataPoints());
    return dst;
  }

  @Override
  public double[] getIntensityValues(@NotNull double[] dst) {
    if (intensityValues == null) {
      return new double[0];
    }

    if (dst.length < getNumberOfDataPoints()) {
      dst = new double[getNumberOfDataPoints()];
    }
    intensityValues.get(0, dst, 0, getNumberOfDataPoints());
    return dst;
  }

}

