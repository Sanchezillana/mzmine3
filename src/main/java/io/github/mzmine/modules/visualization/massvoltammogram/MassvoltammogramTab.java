package io.github.mzmine.modules.visualization.massvoltammogram;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.javafx.FxIconUtil;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.Collection;
import java.util.List;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;

public class MassvoltammogramTab extends MZmineTab {

  private final Image MOVE_PLOT_ICON = FxIconUtil.loadImageFromResources(
      "icons/massvoltammogram/btnMove.png");
  private final Image RESET_PLOT_ICON = FxIconUtil.loadImageFromResources(
      "icons/massvoltammogram/btnReset.png");
  private final Image ROTATE_PLOT_ICON = FxIconUtil.loadImageFromResources(
      "icons/massvoltammogram/btnRotate.png");
  private final Image EXPORT_PLOT_ICON = FxIconUtil.loadImageFromResources("icons/exporticon.png");
  private final Image EDIT_MZ_RANGE_ICON = FxIconUtil.loadImageFromResources(
      "icons/massvoltammogram/btnEditMzRange.png");

  @Override
  public @NotNull Collection<? extends RawDataFile> getRawDataFiles() {
    return null;
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getFeatureLists() {
    return null;
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getAlignedFeatureLists() {
    return null;
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {

  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  private ExtendedPlot3DPanel plot;

  public MassvoltammogramTab(String title, ExtendedPlot3DPanel plot) {
    super(title);

    this.plot = plot;

    final ExtendedPlotToolBar plotToolBar = plot.getExtendedPlotToolBar();
    final BorderPane mainPane = new BorderPane();

    //Converting the swing object to JavaFX.
    final SwingNode swingNodePlot = new SwingNode();
    swingNodePlot.setContent(this.plot);

    //Creating a button to move the plot.
    final ToggleButton moveButton = new ToggleButton(null, new ImageView(MOVE_PLOT_ICON));
    moveButton.setTooltip(new Tooltip("Move the massvoltammogram."));
    moveButton.setOnAction(e -> plotToolBar.moveButton.doClick());
    moveButton.setMinSize(35, 35);

    //Creating a Button to rotate the plot.
    final ToggleButton rotateButton = new ToggleButton(null, new ImageView(ROTATE_PLOT_ICON));
    rotateButton.setSelected(true);
    rotateButton.setTooltip(new Tooltip("Rotate the massvoltammogram."));
    rotateButton.setOnAction(e -> plotToolBar.rotateButton.doClick());
    rotateButton.setMinSize(35, 35);

    //Connecting the move and rotate buttons in a toggle group
    final ToggleGroup toggleGroup = new ToggleGroup();
    moveButton.setToggleGroup(toggleGroup);
    rotateButton.setToggleGroup(toggleGroup);

    //Creating a button to reset the zoom.
    final Button resetButton = new Button(null, new ImageView(RESET_PLOT_ICON));
    resetButton.setTooltip(new Tooltip("Reset the view."));
    resetButton.setOnAction(e -> plotToolBar.resetPlotButton.doClick());
    resetButton.setMinSize(35, 35);

    //Creating a button to export the plot.
    final Button exportButton = new Button(null, new ImageView(EXPORT_PLOT_ICON));
    exportButton.setTooltip(new Tooltip("Export the massvoltammogram."));
    exportButton.setOnAction(e -> MassvoltammogramExport.exportPlot(this.plot));
    exportButton.setMinSize(35, 35);

    //Creating a button to edit the m/z-range.
    Button editMzRangeButton = new Button(null, new ImageView(EDIT_MZ_RANGE_ICON));
    editMzRangeButton.setTooltip(new Tooltip("Edit the massvoltammograms m/z-range."));
    editMzRangeButton.setOnAction(e -> {
      //Extracting the new m/z range from the list of raw scans.
      editMzRange(plot);
      //Exchanging the old plot for the new one.
      swingNodePlot.setContent(this.plot);
    });
    editMzRangeButton.setMinSize(35, 35);

    //Creating a new toolbar and adding the buttons.
    final ToolBar toolbar = new ToolBar();
    toolbar.setOrientation(Orientation.VERTICAL);
    toolbar.getItems()
        .addAll(moveButton, rotateButton, resetButton, exportButton, editMzRangeButton);

    //Adding the toolbar and the plot to the pane.
    mainPane.setCenter(swingNodePlot);
    mainPane.setRight(toolbar);

    //Setting the pane as the MassvoltammogramTabs content.
    setContent(mainPane);
  }

  /**
   * Opens up a dialog to enter a new m/z-range and exchanges the plots data with the new spectra
   * within the given range.
   *
   * @param plot The plot to be updated.
   */
  public void editMzRange(ExtendedPlot3DPanel plot) {

    //Getting user input for the new m/z-Range.
    final MassvoltammogramMzRangeParameter mzRangeParameter = new MassvoltammogramMzRangeParameter();
    if(mzRangeParameter.showSetupDialog(true) != ExitCode.OK){return;}
    final Range<Double> newMzRange = mzRangeParameter.getValue(
        MassvoltammogramMzRangeParameter.mzRange);

    //Getting the raw data from the plot.
    final List<double[][]> scans = plot.getRawScans();

    //Processing the raw data.
    List<double[][]> spectra = MassvoltamogramUtils.extractMZRangeFromScan(scans, newMzRange);
    final double maxIntensity = ScanUtils.getMaxIntensity(spectra);
    final List<double[][]> spectraWithoutNoise = MassvoltamogramUtils.removeNoise(spectra,
        maxIntensity);
    final List<double[][]> spectraWithoutZeros = MassvoltamogramUtils.removeExcessZeros(
        spectraWithoutNoise);

    //Adding the new list of scans to the plot for later export.
    plot.addRawScansInMzRange(spectra);

    //Getting the divisor and the min and max potential range to set up the axis correctly.
    final double divisor = MassvoltamogramUtils.getDivisor(maxIntensity);
    final double[][] firstScan = scans.get(0);
    final double[][] lastScan = scans.get(scans.size() - 1);
    final Range<Double> potentialRange = MassvoltammogramParameters.potentialRange.getValue();

    //Removing the old line plots from the plot panel
    plot.removeAllPlots();

    //Adding the new plots and setting the axis up correctly.
    MassvoltamogramUtils.addSpectraToPlot(spectraWithoutZeros, divisor, plot);
    plot.setFixedBounds(0, newMzRange.lowerEndpoint(), newMzRange.upperEndpoint());
    plot.setFixedBounds(1, potentialRange.lowerEndpoint(), potentialRange.upperEndpoint());

    this.plot = plot;
  }
}
