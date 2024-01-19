/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection;

import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import org.jetbrains.annotations.NotNull;

public class MassDetectorUtils {

  /**
   * Create the isotope detector or an inactive version if not selected
   */
  @NotNull
  public static MassesIsotopeDetector createIsotopeDetector(
      final @NotNull OptionalModuleParameter<DetectIsotopesParameter> detectIsotopes) {

    // If isotopes are going to be detected get all the required parameters
    if (detectIsotopes.getValue()) {
      var isotopesParameters = detectIsotopes.getEmbeddedParameters();
      return isotopesParameters.create();
    } else {
      return MassesIsotopeDetector.createInactiveDefault();
    }
  }


  /**
   * Create new mass detector with the parameters provided in this {@link MZmineProcessingStep}
   */
  @NotNull
  public static MassDetector createMassDetector(
      final MZmineProcessingStep<MassDetector> massDetectorStep) {
    ParameterSet parameterSet = massDetectorStep.getParameterSet();
    // derive new mass detector with parameters
    return massDetectorStep.getModule().create(parameterSet);
  }
}
