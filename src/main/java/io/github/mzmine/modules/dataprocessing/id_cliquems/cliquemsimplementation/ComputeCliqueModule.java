package io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;

public class ComputeCliqueModule {

  private Logger logger = Logger.getLogger(getClass().getName());

  private AnClique anClique;
  private PeakList peakList;
  private List<PeakData> peakDataList;
  private RawDataFile rawDataFile;
  private double[][] cosineCorrelation;

  public ComputeCliqueModule(PeakList peakList, RawDataFile rdf){
    this.rawDataFile = rdf;
    this.peakList =peakList;
    peakDataList = getPeakDatafromPeaks(peakList,rdf);
    anClique = new AnClique(peakDataList,rdf);
  }

  private List<PeakData> getPeakDatafromPeaks(PeakList peakList, RawDataFile dataFile){
    List<PeakData> peakDataList = new ArrayList<>();
    for(int i=0;i<peakList.getRows().size() ; i++){
      PeakListRow peak = peakList.getRows().get(i);
      double mz ;
      double mzmin ;
      double mzmax ;
      double rt;
      double rtmin ;
      double rtmax ;
      double intensity;
      int peakListRowID;
      mz = peak.getPeak(dataFile).getMZ();
      mzmin  = peak.getPeak(dataFile).getRawDataPointsMZRange().lowerEndpoint();
      mzmax = peak.getPeak(dataFile).getRawDataPointsMZRange().upperEndpoint();
      rt = peak.getPeak(dataFile).getRT();
      rtmin = peak.getPeak(dataFile).getRawDataPointsRTRange().lowerEndpoint();
      rtmax = peak.getPeak(dataFile).getRawDataPointsRTRange().upperEndpoint();
      intensity = peak.getPeak(dataFile).getHeight();
      peakListRowID = peak.getID();
      PeakData peakData = new PeakData(mz,mzmin,mzmax,rt,rtmin,rtmax,intensity,i+1,peakListRowID);
      peakDataList.add(peakData);
    }
    return peakDataList;
  }


  private double[][] getEIC(RawDataFile file, List<PeakData> peakDataList){
    List<List<DataPoint>> dataPoints = new ArrayList<>(); // contains m/z and intensity data
    List<Double> rts = new ArrayList<>(); // holds Retention Time values in seconds
    for(int z: file.getScanNumbers()){
      rts.add(file.getScan(z).getRetentionTime() * 60.0); // conversion for minutes to seconds
      List<DataPoint> dps = new ArrayList<DataPoint>(Arrays.asList(file.getScan(z).getDataPoints()));

      dataPoints.add(dps);
    }
    // nrows = #rts , ncols = # peaks, already transposed
    double EIC[][] = new double[file.getScanNumbers().length][peakDataList.size()];
    for(int i = 0; i<file.getScanNumbers().length ; i++){
      for(int j = 0; j<peakDataList.size();j++){
        EIC[i][j] = 0.0;
      }
    }

//    boolean flag = true;
    for(int i=0; i<peakDataList.size() ; i++){
      PeakData pd = peakDataList.get(i);
      int posrtmin = rts.indexOf(pd.getRtmin() * 60.0); // position where peak matches rtmin
      int posrtmax = rts.indexOf(pd.getRtmax() * 60.0); // position where peak matches rtmax

      for(int j = posrtmin ; j<posrtmax ; j+=1){
        List<Double> intensities = new ArrayList<>();
        for(DataPoint dp : dataPoints.get(j)){
          //TODO precision only for testing
//          Double mzmin = Double.parseDouble(String.format("%.5f", pd.getMzmin()));
//          Double mzmax = Double.parseDouble(String.format("%.5f", pd.getMzmax()));
//          Double dpmz = Double.parseDouble(String.format("%.5f", dp.getMZ()));
          Double mzmin = pd.getMzmin();
          Double mzmax = pd.getMzmax();
          Double dpmz =  dp.getMZ();
          if(dpmz<=mzmax && dpmz>=mzmin){
            intensities.add(dp.getIntensity());
          }
        }
        if(intensities.size() == 0){
          EIC[j][i] = 0.0; // no effect
        }
        else{
          Double meanInt = 0.0;
          for(Double d : intensities){
            meanInt+=d;
          }
          meanInt /= intensities.size();
          EIC[j][i] = meanInt;
        }


      }
    }

//    for(int i= 0 ;i< EIC[0].length ; i++){
//      for(int j = 0 ; j<EIC.length ; j++){
//        System.out.print(EIC[j][i]+" ");
//      }
//      System.out.println();
//    }
//    System.out.println();
    return EIC;

  }

  //TODO make use of sparse matrix, make algo time efficient
  private double[][] cosCorrbyColumn (double [][] data){
    int row = data.length, col = data[0].length;
    double [][] corr = new double[col][col];

    for(int i=0; i<col ; i++){
      for(int j=0; j<col; j++){
        corr[i][j] = 0.0;
      }
    }
    for(int i=0; i<col ; i++){
      for(int j=0; j<col; j++){
        double modi = 0.0, modj = 0.0;
        for(int k=0;k<row;k++){
          corr[i][j] += data[k][i] * data[k][j];
          modi += data[k][i]* data[k][i];
          modj += data[k][j]* data[k][j];
        }
        modi = Math.sqrt(modi);
        modj = Math.sqrt(modj);
        corr[i][j] = corr[i][j]/(modi*modj);
      }
    }
    return corr;
  }


  //  identify peaks with very similar cosine correlation, m/z, rt and intensity
  private List<Integer> similarFeatures(double[][] cosineCorr, List<PeakData> peakDataList, double mzdiff, double rtdiff,
      double  intdiff){
//    double mzdiff = 0.000005, rtdiff = 0.0001, intdiff = 0.0001; // constant parameters
    //find all elements in cosineCorr with i<j and value > 0.99
    List<Integer> edgeX = new ArrayList<>();
    List<Integer> edgeY = new ArrayList<>();
    for(int i=0; i<cosineCorr.length; i++){
      for(int j=i+1; j<cosineCorr[0].length; j++){
        if(cosineCorr[i][j]>0.99){
          edgeX.add(i);
          edgeY.add(j);
        }
      }
    }
    List<Integer> nodesToDelete = new ArrayList<>();
    if(edgeX.size() > 0){
      for(int i=0; i<edgeX.size() ; i++){
        PeakData p1 = peakDataList.get(edgeX.get(i));
        PeakData p2 = peakDataList.get(edgeY.get(i));
        double error_mz = (p1.getMz() - p2.getMz()) / p1.getMz() ;
        double error_rt = (p1.getRt()- p2.getRt()) / p1.getRt() ;
        double error_int = (p1.getIntensity() - p2.getIntensity()) / p1.getIntensity() ;
        if((error_mz < mzdiff) && (error_rt < rtdiff) && (error_int < intdiff)){
          Integer node = ( edgeX.get(i) < edgeY.get(i) ? edgeX.get(i) : edgeY.get(i) );
          nodesToDelete.add(node);
        }
      }
    }
    Collections.sort(nodesToDelete);
    return nodesToDelete;
  }


  private void filterFeatures(double[][] cosinus, List<PeakData> peakDL, double mzdiff, double rtdiff,
      double  intdiff){
    List<PeakData> modifiedPeakDataList = new ArrayList<>();
    List<Integer> deleteIndices = similarFeatures(cosinus, peakDL, mzdiff, rtdiff, intdiff);
    if(deleteIndices.size()==0){
      logger.log(Level.INFO,"No feature deleted");
      return;
    }
    for(PeakData pd : peakDataList){
      if(deleteIndices.contains(peakDataList.indexOf(pd))){
        continue;
      }
      PeakData pdmod = new PeakData(pd);
      modifiedPeakDataList.add(pdmod);
    }

    double[][] modifiedCosineCorr = new double [cosinus.length - deleteIndices.size()][cosinus[0].length - deleteIndices.size()];
    //deleting row and columns of indices in deleteIndices
    int colShift = 0;
    for(int i=0; i<cosinus.length ; i++){
      int rowShift = 0;
      if(colShift < deleteIndices.size() && i==deleteIndices.get(colShift)){
        colShift++;
        continue;
      }
      for(int j=0; j<cosinus[0].length ; j++){
        if(rowShift < deleteIndices.size() &&  j==deleteIndices.get(rowShift)){
          rowShift++;
          continue;
        }
        modifiedCosineCorr[i-colShift][j-rowShift] = cosinus[i][j];
      }
    }


    this.cosineCorrelation = modifiedCosineCorr;
    anClique.changePeakDataList(modifiedPeakDataList);
    logger.log(Level.INFO,deleteIndices.size()+" features deleted.");


  }

  /**
   * This function is to assign a clique group value to nodes that do not have links, because they
   * did not appear in the edgelists
   */
   private void updateCliques(){
    List<Pair<Integer,Integer>> nodeCliqueList = this.anClique.getNetwork().getResultNode_clique();
    List<PeakData> ungroupedFeatures = new ArrayList<>();
    Integer maxClique = 0;
    for(Pair nodeClique: nodeCliqueList){
      if(maxClique < (Integer) nodeClique.getKey()){
        maxClique = (Integer) nodeClique.getKey();
      }
    }
    for(PeakData pd : this.peakDataList){
      boolean present = false;
      for(Pair nodeClique : nodeCliqueList){
        if(nodeClique.getKey().equals(pd.getNodeID())){
          present = true;
          break;
        }
      }
      if(!present){
        ungroupedFeatures.add(pd);
      }
    }
    System.out.println("These many peaks not cliqued"+ ungroupedFeatures.size());
    for(PeakData pd : ungroupedFeatures){
      maxClique+=1;
      Pair <Integer, Integer> p = new Pair<>(pd.getNodeID(),maxClique);
      nodeCliqueList.add(p);
    }
    //TODO sorting if required
   }


  public AnClique getClique(boolean filter, double mzdiff, double rtdiff, double  intdiff,
      double tol){

    double EIC[][] = getEIC(rawDataFile, peakDataList);
    this.cosineCorrelation = cosCorrbyColumn(EIC);
    if(filter)
      filterFeatures(cosineCorrelation, peakDataList, mzdiff, rtdiff, intdiff);
    List<Integer> nodeIDList = new ArrayList<>();
    for(PeakData pd : peakDataList)
      nodeIDList.add(pd.getNodeID());
    anClique.getNetwork().returnCliques(cosineCorrelation, nodeIDList, tol, false );
    updateCliques();
    System.out.println(anClique.getNetwork().getResultNode_clique());
    this.anClique.cliquesFound = true;

    return anClique;
  }

  //TODO check filter
  public AnClique getClique() {
    return getClique(false, 0.000005, 0.0001, 0.0001, .00001);
  }
}
