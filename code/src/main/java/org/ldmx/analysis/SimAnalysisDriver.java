package org.ldmx.analysis;

import static org.ldmx.EventConstants.ECAL;
import static org.ldmx.EventConstants.ECAL_SCORING_SIM_HITS;
import static org.ldmx.EventConstants.ECAL_SIM_HITS;
import static org.ldmx.EventConstants.MCPARTICLES;
import static org.ldmx.EventConstants.RECOIL;
import static org.ldmx.EventConstants.RECOIL_SIM_HITS;
import static org.ldmx.EventConstants.TAGGER;
import static org.ldmx.EventConstants.TAGGER_SIM_HITS;
import static org.ldmx.EventConstants.TRIGGER_PAD_SIM_HITS;
import hep.aida.ICloud1D;
import hep.aida.ICloud2D;
import hep.aida.IHistogram1D;
import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.geometry.Subdetector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Basic plots of data collections from LDMX detector simulation output.
 * @author jeremym
 */
public class SimAnalysisDriver extends Driver {
        
    private static final AIDA PLOT = AIDA.defaultInstance();
          
    /* Constant to disable conversion of clouds to histograms. */
    private static final int NO_CONVERT = 999999999;
    
    /* some handy PDG IDs */
    public static final int ELECTRON = 11;
    public static final int PHOTON = 22;
    
    /* unit conversion from GeV */
    public static double MeV = 1000.0;
    public static double KeV = 1000000.0;
            
    private IHistogram1D ecalHitE;
    private IHistogram1D ecalHitTime;
    private IHistogram1D ecalTotE;
    private ICloud2D ecalXY;
    private IHistogram1D ecalHitCount;
    private List<ICloud2D> ecalLayerXY;
    private IHistogram1D ecalLayerE;
    private IHistogram1D ecalLayer;
    private ICloud2D ecalLayerTime;
    private List<IHistogram1D> ecalLayerHitTime;
    private List<IHistogram1D> ecalLayerHitCount;
        
    private IHistogram1D recoilHitCount;
    private IHistogram1D recoilTime;
    private ICloud2D recoilXY;
    private ICloud2D recoilSensorXY;
    private ICloud2D recoilLayerTime;
    private IHistogram1D recoilLayer;
    private ICloud2D recoilHitXZ;
    private ICloud2D recoilHitYZ;    
    private IHistogram1D recoilHitEdep;
    private List<IHistogram1D> recoilLayerHitCount;
    private List<IHistogram1D> recoilLayerHitTime;
    private List<ICloud2D> recoilLayerXY;
    private List<ICloud2D> recoilLayerSensorXY;
    private List<IHistogram1D> recoilLayerEdep;
    private ICloud2D recoilParticleEdep;
           
    private IHistogram1D taggerHitCount;
    private IHistogram1D taggerTime;    
    private IHistogram1D taggerLayer;
    private ICloud2D taggerXY;
    private ICloud2D taggerHitXZ;
    private ICloud2D taggerHitYZ;   
    private ICloud2D taggerSensorXY;
    private IHistogram1D taggerHitEdep;
    private List<IHistogram1D> taggerLayerHitCount;
    private List<IHistogram1D> taggerLayerHitTime;
    private List<ICloud2D> taggerLayerXY;
    private List<ICloud2D> taggerLayerSensorXY;
    private List<IHistogram1D> taggerLayerEdep;
    private ICloud2D taggerParticleEdep;
    
    private IHistogram1D mcpCount;
    private ICloud2D mcpEndpoint;
    private ICloud1D mcpBackscatterCount;
    private ICloud1D mcpBackscatterProdTime;
    private ICloud1D mcpProdTime;
    private ICloud1D mcpPrimaryHitCount;
    private ICloud1D mcpPrimaryDauCount;
    private ICloud1D mcpPrimaryEndPointZ;
    private ICloud1D mcpDauHitCount;
    private ICloud1D mcpDauProdTime;
    private ICloud1D mcpDauOriginZ;
    private ICloud1D mcpElectronDauE;
    private ICloud1D mcpElectronDauCount;
    private ICloud1D mcpPhotonDauCount;
    private ICloud1D mcpPhotonDauE;
    
    private ICloud2D ecalScoringXY;
    private IHistogram1D ecalScoringMomentumZ;
    private IHistogram1D ecalScoringElectronCount;
    private IHistogram1D ecalScoringPhotonCount;
    private IHistogram1D ecalScoringElectronE;
    private IHistogram1D ecalScoringPhotonE;
    private IHistogram1D ecalScoringElectronMomentumZ;
    private IHistogram1D ecalScoringPhotonMomentumZ;
    private ICloud2D ecalScoringPrimaryXY;
    private ICloud2D ecalScoringDauXY;
    
    private ICloud1D triggerPadEnergy;
    private ICloud1D triggerPadTime;
          
    private IDDecoder calID;
    private IDDecoder recoilID;
    private IDDecoder taggerID;
          
    public void startOfData() {
        
        /* ECal plots */
        ecalHitE = PLOT.histogram1D("/" + ECAL_SIM_HITS + "/Hit Energy", 1000, 0.0, 1000.0);
        ecalHitE.annotation().addItem("xAxisLabel", "Energy [KeV]");
        ecalHitTime = PLOT.histogram1D("/" + ECAL_SIM_HITS + "/Hit Time", 700, 3.0, 10.0);        
        ecalHitTime.annotation().addItem("xAxisLabel", "Time [ns]");        
        ecalTotE = PLOT.histogram1D("/" + ECAL_SIM_HITS + "/Tot Event Energy", 200, 0.0, 100.0);
        ecalTotE.annotation().addItem("xAxisLabel", "Energy [MeV]");
        ecalXY = PLOT.cloud2D("/" + ECAL_SIM_HITS + "/Hit X vs Y", NO_CONVERT);
        ecalXY.annotation().addItem("xAxisLabel", "X [mm]");
        ecalXY.annotation().addItem("yAxisLabel", "Y [mm]");       
        ecalHitCount = PLOT.histogram1D("/" + ECAL_SIM_HITS + "/Hit Count", 500, 0, 500);
        ecalHitCount.annotation().addItem("xAxisLabel", "Hit Count");
        ecalHitCount.annotation().addItem("yAxisLabel", "Number of Events");      
        ecalLayerTime = PLOT.cloud2D("/" + ECAL_SIM_HITS + "/Layer vs Hit Time", NO_CONVERT);
        ecalLayerTime.annotation().addItem("xAxisLabel", "Layer Number");
        ecalLayerTime.annotation().addItem("yAxisLabel", "Time [ns]");
        
        /* Recoil Tracker plots */
        recoilHitCount = PLOT.histogram1D("/" + RECOIL_SIM_HITS + "/Hit Count", 100, 0., 100.);
        recoilTime = PLOT.histogram1D("/" + RECOIL_SIM_HITS + "/Hit Time", 1000, 0., 10.);
        recoilTime.annotation().addItem("xAxisLabel", "Time [ns]");
        recoilXY = PLOT.cloud2D("/" + RECOIL_SIM_HITS + "/Hit XY", NO_CONVERT);
        recoilXY.annotation().addItem("xAxisLabel", "X [mm]");
        recoilXY.annotation().addItem("yAxisLabel", "Y [mm]");
        recoilHitXZ = PLOT.cloud2D("/" + RECOIL_SIM_HITS + "/Hit XZ", NO_CONVERT);
        recoilHitXZ.annotation().addItem("xAxisLabel", "X [mm]");
        recoilHitXZ.annotation().addItem("yAxisLabel", "Z [mm]");
        recoilHitYZ = PLOT.cloud2D("/" + RECOIL_SIM_HITS + "/Hit YZ", NO_CONVERT);
        recoilHitYZ.annotation().addItem("xAxisLabel", "Y [mm]");
        recoilHitYZ.annotation().addItem("yAxisLabel", "Z [mm]");
        recoilLayerTime = PLOT.cloud2D("/" + RECOIL_SIM_HITS + "/Layer vs Hit Time", NO_CONVERT);
        recoilLayerTime.annotation().addItem("xAxisLabel", "Layer Number");
        recoilLayerTime.annotation().addItem("yAxisLabel", "Time [ns]");
        recoilHitEdep = PLOT.histogram1D("/" + RECOIL_SIM_HITS + "/Hit Edep", 600, 0.0, 600.0);
        recoilHitEdep.annotation().addItem("xAxisLabel", "Edep [KeV]");
        recoilParticleEdep = PLOT.cloud2D("/" + RECOIL_SIM_HITS + "/Particle E vs Edep", NO_CONVERT);
        recoilParticleEdep.annotation().addItem("xAxisLabel", "Energy [GeV]");
        recoilParticleEdep.annotation().addItem("yAxisLabel", "Energy [KeV]");
        // Of course, this doesn't work in AIDA...
        //recoilParticleEdep.annotation().addItem("xAxisScale", "log");
        //recoilParticleEdep.annotation().addItem("yAxisScale", "log");
        
        /* Tagger plots */
        taggerHitCount = PLOT.histogram1D("/" + TAGGER_SIM_HITS + "/Hit Count", 100, 0., 100.);
        taggerTime = PLOT.histogram1D("/" + TAGGER_SIM_HITS + "/Hit Time", 1000, 0., 10.);
        taggerTime.annotation().addItem("xAxisLabel", "Time [ns]");
        taggerXY = PLOT.cloud2D("/" + TAGGER_SIM_HITS + "/Hit XY", NO_CONVERT);
        taggerXY.annotation().addItem("xAxisLabel", "X [mm]");
        taggerXY.annotation().addItem("yAxisLabel", "Y [mm]");
        taggerHitXZ = PLOT.cloud2D("/" + TAGGER_SIM_HITS + "/Hit XZ", NO_CONVERT);
        taggerHitXZ.annotation().addItem("xAxisLabel", "X [mm]");
        taggerHitXZ.annotation().addItem("yAxisLabel", "Z [mm]");
        taggerHitYZ = PLOT.cloud2D("/" + TAGGER_SIM_HITS + "/Hit YZ", NO_CONVERT);
        taggerHitYZ.annotation().addItem("xAxisLabel", "Y [mm]");
        taggerHitYZ.annotation().addItem("yAxisLabel", "Z [mm]");
        taggerHitEdep = PLOT.histogram1D("/" + TAGGER_SIM_HITS + "/Hit Edep", 600, 0.0, 600.0);
        taggerParticleEdep = PLOT.cloud2D("/" + TAGGER_SIM_HITS + "/Particle E vs Edep", NO_CONVERT);
        taggerParticleEdep.annotation().addItem("xAxisLabel", "Energy [GeV]");
        taggerParticleEdep.annotation().addItem("yAxisLabel", "Energy [KeV]");
        
        /* MCParticle plots */
        mcpCount = PLOT.histogram1D("/" + MCPARTICLES + "/MCParticle Count", 20, 0., 20.0);
        mcpEndpoint = PLOT.cloud2D("/" + MCPARTICLES + "/End Point XY", NO_CONVERT);
        mcpEndpoint.annotation().addItem("xAxisLabel", "X [mm]");
        mcpEndpoint.annotation().addItem("yAxisLabel", "Y [mm]");
        mcpBackscatterCount = PLOT.cloud1D("/" + MCPARTICLES + "/Backscatter Count", NO_CONVERT);
        mcpProdTime = PLOT.cloud1D("/" + MCPARTICLES + "/Production Time", NO_CONVERT);
        mcpProdTime.annotation().addItem("xAxisLabel", "Time [ns]");
        mcpPrimaryHitCount = PLOT.cloud1D("/" + MCPARTICLES + "/Primary Hit Count", NO_CONVERT);
        mcpPrimaryHitCount.annotation().addItem("xAxisLabel", "Hit Count");
        mcpPrimaryDauCount = PLOT.cloud1D("/" + MCPARTICLES + "/Daughter Particle Count", NO_CONVERT);
        mcpPrimaryDauCount.annotation().addItem("xAxisLabel", "Particle Count");        
        mcpDauHitCount = PLOT.cloud1D("/" + MCPARTICLES + "/Daughter Hit Count", NO_CONVERT);
        mcpDauHitCount.annotation().addItem("xAxisLabel", "Hit Count");        
        mcpBackscatterProdTime = PLOT.cloud1D("/" + MCPARTICLES + "/Backscatter Prod Time", NO_CONVERT);
        mcpBackscatterProdTime.annotation().addItem("xAxisLabel", "Time [ns]");
        mcpPrimaryEndPointZ = PLOT.cloud1D("/" + MCPARTICLES + "/Primary End Point Z", NO_CONVERT);
        mcpPrimaryEndPointZ.annotation().addItem("xAxisLabel", "Z [mm]");
        mcpElectronDauE = PLOT.cloud1D("/" + MCPARTICLES + "/Electron Daughter Energy", NO_CONVERT);
        mcpElectronDauE.annotation().addItem("xAxisLabel", "Energy [GeV]");
        mcpElectronDauCount = PLOT.cloud1D("/" + MCPARTICLES + "/Electron Daughter Count", NO_CONVERT);
        mcpElectronDauCount.annotation().addItem("xAxisLabel", "Number of Electrons");
        mcpPhotonDauCount = PLOT.cloud1D("/" + MCPARTICLES + "/Photon Daughter Count", NO_CONVERT);
        mcpPhotonDauCount.annotation().addItem("xAxisLabel", "Number of Photons");
        mcpDauProdTime = PLOT.cloud1D("/" + MCPARTICLES + "/Daughter Production Time", NO_CONVERT);
        mcpDauProdTime.annotation().addItem("xAxisLabel", "Time [ns]");
        mcpDauOriginZ = PLOT.cloud1D("/" + MCPARTICLES + "/Daughter Origin Z", NO_CONVERT);
        mcpDauOriginZ.annotation().addItem("xAxisLabel", "Z [mm]");
        mcpPhotonDauE = PLOT.cloud1D("/" + MCPARTICLES + "/Photon Daughter Energy", NO_CONVERT);
        mcpPhotonDauE.annotation().addItem("xAxisLabel", "Energy [GeV]");
        
        /* ECal scoring plane */
        ecalScoringXY = PLOT.cloud2D("/" + ECAL_SCORING_SIM_HITS + "/Hit XY", NO_CONVERT);
        ecalScoringXY.annotation().addItem("xAxisLabel", "X [mm]");
        ecalScoringXY.annotation().addItem("yAxisLabel", "Y [mm]");
        ecalScoringMomentumZ = PLOT.histogram1D("/" + ECAL_SCORING_SIM_HITS + "/PZ", 110, -5.5, 5.5);
        ecalScoringMomentumZ.annotation().addItem("xAxisLabel", "PZ [GeV]");
        ecalScoringElectronCount = PLOT.histogram1D("/" + ECAL_SCORING_SIM_HITS + "/Electron Count", 50, -0.5, 50.5);
        ecalScoringPhotonCount = PLOT.histogram1D("/" + ECAL_SCORING_SIM_HITS + "/Photon Count", 50, -0.5, 50.5);        
        ecalScoringElectronE = PLOT.histogram1D("/" + ECAL_SCORING_SIM_HITS + "/Electron E", 100, -0.5, 4.5);
        ecalScoringElectronE.annotation().addItem("xAxisLabel", "Energy [GeV]");        
        ecalScoringPhotonE = PLOT.histogram1D("/" + ECAL_SCORING_SIM_HITS + "/Photon E", 100, -0.5, 4.5);
        ecalScoringPhotonE.annotation().addItem("xAxisLabel", "Energy [GeV]");               
        ecalScoringElectronMomentumZ = PLOT.histogram1D("/" + ECAL_SCORING_SIM_HITS + "/Electron PZ", 110, -5.5, 5.5);
        ecalScoringElectronMomentumZ.annotation().addItem("xAxisLabel", "PZ [GeV]");
        ecalScoringPhotonMomentumZ = PLOT.histogram1D("/" + ECAL_SCORING_SIM_HITS + "/Photon PZ", 110, -5.5, 5.5);
        ecalScoringPhotonMomentumZ.annotation().addItem("xAxisLabel", "PZ [GeV]");
        ecalScoringPrimaryXY = PLOT.cloud2D("/" + ECAL_SCORING_SIM_HITS + "/Primary XY", NO_CONVERT);
        ecalScoringPrimaryXY.annotation().addItem("xAxisLabel", "X [mm]");
        ecalScoringPrimaryXY.annotation().addItem("yAxisLabel", "Y [mm]");
        ecalScoringDauXY = PLOT.cloud2D("/" + ECAL_SCORING_SIM_HITS + "/Dau XY", NO_CONVERT);
        ecalScoringDauXY.annotation().addItem("xAxisLabel", "X [mm]");
        ecalScoringDauXY.annotation().addItem("yAxisLabel", "Y [mm]");
        
        /* Trigger Pad plots */
        triggerPadEnergy = PLOT.cloud1D("/" + TRIGGER_PAD_SIM_HITS + "/Hit Energy");
        triggerPadTime = PLOT.cloud1D("/" + TRIGGER_PAD_SIM_HITS + "/Hit Time");
    }
    
    public void detectorChanged(Detector detector) {
        
        /* ECal plot setup */
        Subdetector ecal = detector.getSubdetector(ECAL);
        calID = ecal.getIDDecoder();        
        int nEcalLayers = ecal.getLayering().getLayerCount();        
        
        ecalLayerE = PLOT.histogram1D("/" + ECAL_SIM_HITS + "/Layer Energy", nEcalLayers, -0.5, ((double) nEcalLayers) - 0.5);
        ecalLayer = PLOT.histogram1D("/" + ECAL_SIM_HITS + "/Layer Number", nEcalLayers, -0.5, ((double) nEcalLayers) - 0.5);
        
        ecalLayerXY = new ArrayList<ICloud2D>();
        ecalLayerHitTime = new ArrayList<IHistogram1D>();
        ecalLayerHitCount = new ArrayList<IHistogram1D>();
        for (int i = 0; i < nEcalLayers; i++) { /* ecal layers numbered from 0 */
            
            ICloud2D ecalLayerPlot = PLOT.cloud2D("/" + ECAL_SIM_HITS + "/Layer " + String.format("%02d", i) + " : X vs Y", NO_CONVERT);
            ecalLayerPlot.annotation().addItem("xAxisLabel", "X [mm]");
            ecalLayerPlot.annotation().addItem("yAxisLabel", "Y [mm]");
            ecalLayerXY.add(ecalLayerPlot);
                        
            IHistogram1D layerHitTimePlot = PLOT.histogram1D("/" + ECAL_SIM_HITS + "/Layer " + String.format("%02d", i) + " : Hit Time", 50, -0.5, 500.5);
            layerHitTimePlot.annotation().addItem("xAxisLabel", "Time [ns]");
            ecalLayerHitTime.add(layerHitTimePlot);
            
            IHistogram1D layerHitCountPlot = PLOT.histogram1D("/" + ECAL_SIM_HITS + "/Layer " + String.format("%02d", i) + " : Hit Count", 60, -0.5, 60.5);
            layerHitCountPlot.annotation().addItem("xAxisLabel", "Hit Count");
            ecalLayerHitCount.add(layerHitCountPlot);
            
        }               
                                
        /* Recoil Tracker plot setup */
        Subdetector recoilTracker = detector.getSubdetector(RECOIL);
        int nRecoilLayers = recoilTracker.getDetectorElement().getChildren().size();
        recoilID = recoilTracker.getIDDecoder();
        
        recoilLayerXY = new ArrayList<ICloud2D>();
        recoilLayerSensorXY = new ArrayList<ICloud2D>();
        recoilLayerHitTime = new ArrayList<IHistogram1D>();
        recoilLayerHitCount = new ArrayList<IHistogram1D>();
        recoilLayerEdep = new ArrayList<IHistogram1D>();
        for (int i = 1; i <= nRecoilLayers; i++) { /* tracker layers numbered from 1 */
            
            // XY layer plot in global coordinates
            ICloud2D recoilLayerPlot = PLOT.cloud2D("/" + RECOIL_SIM_HITS + "/Layer " + String.format("%02d", i) + " : X vs Y", NO_CONVERT);
            recoilLayerPlot.annotation().addItem("xAxisLabel", "X [mm]");
            recoilLayerPlot.annotation().addItem("yAxisLabel", "Y [mm]");
            recoilLayerXY.add(recoilLayerPlot);
            
            // XY layer plot in sensor coordinates
            ICloud2D recoilLayerSensorPlot = PLOT.cloud2D("/" + RECOIL_SIM_HITS + "/Layer " + String.format("%02d", i) + " : Sensor X vs Y", NO_CONVERT);
            recoilLayerSensorPlot.annotation().addItem("xAxisLabel", "X [mm]");
            recoilLayerSensorPlot.annotation().addItem("yAxisLabel", "Y [mm]");
            recoilLayerSensorXY.add(recoilLayerSensorPlot);
                        
            IHistogram1D layerHitTime  = PLOT.histogram1D("/" + RECOIL_SIM_HITS + "/Layer " + String.format("%02d", i) + " : Hit Time", 110, -0.5, 10.5);
            layerHitTime.annotation().addItem("xAxisLabel", "Time [ns]");
            recoilLayerHitTime.add(layerHitTime);
            
            IHistogram1D layerHitCount  = PLOT.histogram1D("/" + RECOIL_SIM_HITS + "/Layer " + String.format("%02d", i) + " : Hit Count", 51, -0.5, 50.5);
            layerHitCount.annotation().addItem("xAxisLabel", "Hit Count");
            recoilLayerHitCount.add(layerHitCount);
            
            IHistogram1D layerEdep  = PLOT.histogram1D("/" + RECOIL_SIM_HITS + "/Layer " + String.format("%02d", i) + " : Edep", 600, 0.0, 600);
            layerEdep.annotation().addItem("xAxisLabel", "Energy [KeV]");
            recoilLayerEdep.add(layerEdep);            
        }
        
        recoilLayer = PLOT.histogram1D("/" + RECOIL_SIM_HITS + "/Layer Number", nRecoilLayers, 0.5, (nRecoilLayers + 0.5));        
        recoilSensorXY = PLOT.cloud2D("/" + RECOIL_SIM_HITS + "/Sensor XY", NO_CONVERT);
        
        /* Tagger Tracker plot setup */
        Subdetector tagger = detector.getSubdetector(TAGGER);
        taggerID = tagger.getIDDecoder();
        int nTaggerLayers = tagger.getDetectorElement().getChildren().size();
        taggerID = tagger.getIDDecoder();
                
        taggerLayer = PLOT.histogram1D("/" + TAGGER_SIM_HITS + "/Layer Number", nTaggerLayers, 0.5, (nTaggerLayers + 0.5));
        taggerSensorXY = PLOT.cloud2D("/" + TAGGER_SIM_HITS + "/Sensor XY", NO_CONVERT);
        
        taggerLayerXY = new ArrayList<ICloud2D>();
        taggerLayerSensorXY = new ArrayList<ICloud2D>();
        taggerLayerHitTime = new ArrayList<IHistogram1D>();
        taggerLayerHitCount = new ArrayList<IHistogram1D>();
        taggerLayerEdep = new ArrayList<IHistogram1D>();
        for (int i = 1; i <= nTaggerLayers; i++) { /* tracker layers numbered from 1 */
            
            // XY layer plot in global coordinates
            ICloud2D taggerLayerPlot = PLOT.cloud2D("/" + TAGGER_SIM_HITS + "/Layer " + String.format("%02d", i) + " : X vs Y", NO_CONVERT);
            taggerLayerPlot.annotation().addItem("xAxisLabel", "X [mm]");
            taggerLayerPlot.annotation().addItem("yAxisLabel", "Y [mm]");
            taggerLayerXY.add(taggerLayerPlot);

            // XY layer plot in sensor coordinates
            ICloud2D taggerLayerSensorPlot = PLOT.cloud2D("/" + TAGGER_SIM_HITS + "/Layer " + String.format("%02d", i) + " : Sensor X vs Y", NO_CONVERT);
            taggerLayerSensorPlot.annotation().addItem("xAxisLabel", "X [mm]");
            taggerLayerSensorPlot.annotation().addItem("yAxisLabel", "Y [mm]");
            taggerLayerSensorXY.add(taggerLayerSensorPlot);
            
            // hit time by layer
            IHistogram1D layerHitTime = PLOT.histogram1D("/" + TAGGER_SIM_HITS + "/Layer " + String.format("%02d", i) + " : Hit Time", 110, -0.5, 10.5);
            layerHitTime.annotation().addItem("xAxisLabel", "Time [ns]");
            taggerLayerHitTime.add(layerHitTime);
            
            // hit count by layer
            IHistogram1D layerHitCount  = PLOT.histogram1D("/" + TAGGER_SIM_HITS + "/Layer " + String.format("%02d", i) + " : Hit Count", 51, -0.5, 50.5);
            layerHitCount.annotation().addItem("xAxisLabel", "Hit Count");
            taggerLayerHitCount.add(layerHitCount);
            
            IHistogram1D layerEdep  = PLOT.histogram1D("/" + TAGGER_SIM_HITS + "/Layer " + String.format("%02d", i) + " : Edep", 600, 0.0, 600);
            layerEdep.annotation().addItem("xAxisLabel", "Energy [KeV]");
            taggerLayerEdep.add(layerEdep);
        }
    }
    
    public void process(EventHeader event) {
        
        System.out.println("SimAnalysisDriver: processing event " + event.getEventNumber());
        
        /**
         * ECal plots
         */        
        final List<SimCalorimeterHit> calHits = event.get(SimCalorimeterHit.class, ECAL_SIM_HITS);
        ecalHitCount.fill(calHits.size());
        Map<Integer, Double> ecalLayerEnergyMap = new HashMap<Integer, Double>();
        Map<Integer, Integer> ecalLayerHitCountMap = new HashMap<Integer, Integer>();
        double totCalE = 0;
        for (SimCalorimeterHit calHit : calHits) {
                        
            calID.setID(calHit.getCellID());
            int layer = calID.getValue("layer");
            double hitE = calHit.getRawEnergy();

            totCalE += hitE * MeV;
            
            double time = calHit.getTime();
            double x = calHit.getPosition()[0];
            double y = calHit.getPosition()[1];

            ecalHitE.fill(hitE * KeV);
            ecalHitTime.fill(calHit.getTime());
            ecalXY.fill(x, y);
            ecalLayerXY.get(layer).fill(x, y);
            ecalXY.fill(x, y);
            ecalLayer.fill(layer);
            ecalLayerTime.fill(layer, time);
            
            if (!ecalLayerEnergyMap.containsKey(layer)) {
                ecalLayerEnergyMap.put(layer, 0.0);
            }
            ecalLayerEnergyMap.put(layer, ecalLayerEnergyMap.get(layer) + hitE);
            
            ecalLayerHitTime.get(layer).fill(time);
            
            if (!ecalLayerHitCountMap.containsKey(layer)) {
                ecalLayerHitCountMap.put(layer, 0);
            }
            ecalLayerHitCountMap.put(layer, ecalLayerHitCountMap.get(layer) + 1);
        }
        
        for (Entry<Integer, Double> entry : ecalLayerEnergyMap.entrySet()) {
            ecalLayerE.fill(entry.getKey(), entry.getValue());
        }
        
        for (Entry<Integer, Integer> entry : ecalLayerHitCountMap.entrySet()) {
            ecalLayerHitCount.get(entry.getKey()).fill(entry.getValue());
        }
        
        ecalTotE.fill(totCalE);
        
        /**
         * Recoil Tracker plots
         */        
        final List<SimTrackerHit> recoilHits = event.get(SimTrackerHit.class, RECOIL_SIM_HITS);
        recoilHitCount.fill(recoilHits.size());
        Map<Integer, Integer> recoilLayerHitCountMap = new HashMap<Integer, Integer>();
        for (SimTrackerHit recoilHit : recoilHits) {
            
            recoilID.setID(recoilHit.getCellID64());
            
            int layer = recoilID.getValue("layer");
            double x = recoilHit.getPosition()[0];
            double y = recoilHit.getPosition()[1];
            double z = recoilHit.getPosition()[2];
            double time = recoilHit.getTime();
            double edep = recoilHit.getdEdx();
            MCParticle mcp = recoilHit.getMCParticle();
           
            recoilHitEdep.fill(edep * KeV);
            recoilTime.fill(recoilHit.getTime());
                        
            Hep3Vector globalPos = recoilHit.getPositionVec();
            Hep3Vector localPos = recoilHit.getDetectorElement().getGeometry().transformGlobalToLocal(globalPos);
            recoilLayerSensorXY.get(layer - 1).fill(localPos.x(), localPos.y());
            recoilSensorXY.fill(localPos.x(), localPos.y());
                        
            recoilLayerXY.get(layer - 1).fill(x, y);
            recoilLayerEdep.get(layer - 1).fill(edep * KeV);
            recoilXY.fill(x, y);
            recoilHitXZ.fill(x, z);
            recoilHitYZ.fill(y, z);
            recoilLayer.fill(layer);
            recoilLayerTime.fill(layer, time);
                        
            if (!recoilLayerHitCountMap.containsKey(layer)) {
                recoilLayerHitCountMap.put(layer, 0);
            }
            recoilLayerHitCountMap.put(layer, recoilLayerHitCountMap.get(layer) + 1);
            
            recoilLayerHitTime.get(layer - 1).fill(time);
            
            recoilParticleEdep.fill(mcp.getEnergy(), recoilHit.getdEdx() * KeV);
        }
        
        for (Entry<Integer, Integer> entry : recoilLayerHitCountMap.entrySet()) {
            recoilLayerHitCount.get(entry.getKey() - 1).fill(entry.getValue());
        }
               
        /**
         * Tagger Tracker plots
         */
        Map<Integer, Integer> taggerLayerHitCountMap = new HashMap<Integer, Integer>();
        final List<SimTrackerHit> taggerHits = event.get(SimTrackerHit.class, TAGGER_SIM_HITS);
        taggerHitCount.fill(taggerHits.size());
        for (SimTrackerHit taggerHit : taggerHits) {
            
            taggerID.setID(taggerHit.getCellID64());
            
            int layer = taggerID.getValue("layer");
            double x = taggerHit.getPosition()[0];
            double y = taggerHit.getPosition()[1];
            double z = taggerHit.getPosition()[2];
            double time = taggerHit.getTime();
            double edep = taggerHit.getdEdx();
            MCParticle mcp = taggerHit.getMCParticle();
            
            taggerHitEdep.fill(edep * KeV);            
            taggerTime.fill(time);
            
            Hep3Vector globalPos = taggerHit.getPositionVec();
            Hep3Vector localPos = taggerHit.getDetectorElement().getGeometry().transformGlobalToLocal(globalPos);
            taggerLayerSensorXY.get(layer - 1).fill(localPos.x(), localPos.y());
            taggerSensorXY.fill(localPos.x(), localPos.y());
                                    
            taggerLayerXY.get(layer - 1).fill(x, y);
            taggerLayerEdep.get(layer - 1).fill(edep * KeV);
            taggerXY.fill(x, y);
            taggerHitXZ.fill(x, z);
            taggerHitYZ.fill(y, z);
            taggerLayer.fill(layer);
            
            taggerLayerHitTime.get(layer - 1).fill(time);
            
            if (!taggerLayerHitCountMap.containsKey(layer)) {
                taggerLayerHitCountMap.put(layer, 0);
            }
            taggerLayerHitCountMap.put(layer, taggerLayerHitCountMap.get(layer) + 1);
            
            recoilParticleEdep.fill(mcp.getEnergy(), taggerHit.getdEdx() * KeV);
        }
        
        for (Entry<Integer, Integer> entry : taggerLayerHitCountMap.entrySet()) {
            taggerLayerHitCount.get(entry.getKey() - 1).fill(entry.getValue());
        }
        
        /**
         * MCParticle plots
         */
        final List<MCParticle> mcps = event.get(MCParticle.class, MCPARTICLES);
        mcpCount.fill(mcps.size());
        int nBackscatter = 0;
        int nPhotonDau = 0;
        int nElectronDau = 0;
        for (MCParticle mcp : mcps) {
            Hep3Vector endPoint = mcp.getEndPoint();
            mcpEndpoint.fill(endPoint.x(), endPoint.y());
            if (mcp.getSimulatorStatus().isBackscatter()) {
                ++nBackscatter;
            }
            mcpProdTime.fill(mcp.getProductionTime());
            if (!mcp.getParents().isEmpty()) {
                mcpDauHitCount.fill(findHits(event.get(SimTrackerHit.class), mcp).size());
                mcpDauProdTime.fill(mcp.getProductionTime());
                if (mcp.getPDGID() == ELECTRON) {
                    mcpElectronDauE.fill(mcp.getEnergy());
                    ++nElectronDau;
                } else if (mcp.getPDGID() == PHOTON) {
                    mcpPhotonDauE.fill(mcp.getEnergy());
                    ++nPhotonDau;
                }
                mcpDauOriginZ.fill(mcp.getOriginZ());
            }
            
            if (mcp.getSimulatorStatus().isBackscatter()) {
                mcpBackscatterProdTime.fill(mcp.getProductionTime());
            }
        }
        mcpBackscatterCount.fill(nBackscatter);
        mcpElectronDauCount.fill(nElectronDau);
        mcpPhotonDauCount.fill(nPhotonDau);
        
        MCParticle primary = mcps.get(0);
        int nPrimaryHits = findHits(event.get(SimTrackerHit.class), primary).size();
        mcpPrimaryHitCount.fill(nPrimaryHits);        
        mcpPrimaryDauCount.fill(primary.getDaughters().size());
        mcpPrimaryEndPointZ.fill(primary.getEndPoint().z());
        
        /**
         * ECal scoring plane
         */
        final List<SimTrackerHit> ecalScoringHits = event.get(SimTrackerHit.class, ECAL_SCORING_SIM_HITS);
        
        final Map<Integer, Integer> ecalScoringPdgCounts = new HashMap<Integer, Integer>();
        ecalScoringPdgCounts.put(ELECTRON, 0);
        ecalScoringPdgCounts.put(PHOTON, 0);        
        for (SimTrackerHit ecalScoringHit : ecalScoringHits) {
            double[] p = ecalScoringHit.getMomentum();
            double[] startPoint = ecalScoringHit.getStartPoint();            
            ecalScoringXY.fill(startPoint[0], startPoint[1]);
            ecalScoringMomentumZ.fill(p[2]);
            
            // Only secondary particles are counted for PZ and E plots (primary is ignored or plotted separately).
            MCParticle mcp = ecalScoringHit.getMCParticle();
            if (mcp.getParents().size() != 0) { /* secondary or daughter particle */
                int pdgid = ecalScoringHit.getMCParticle().getPDGID();
                if (ecalScoringPdgCounts.get(pdgid) == null) {
                    ecalScoringPdgCounts.put(pdgid, 0);
                }
                ecalScoringPdgCounts.put(ecalScoringHit.getMCParticle().getPDGID(), ecalScoringPdgCounts.get(pdgid) + 1);
                
                if (pdgid == ELECTRON) {
                    this.ecalScoringElectronE.fill(mcp.getEnergy());
                    this.ecalScoringElectronMomentumZ.fill(ecalScoringHit.getMomentum()[2]);
                } else if (pdgid == PHOTON) {
                    this.ecalScoringPhotonE.fill(mcp.getEnergy());
                    this.ecalScoringPhotonMomentumZ.fill(ecalScoringHit.getMomentum()[2]);
                }
                
                ecalScoringDauXY.fill(startPoint[0], startPoint[1]);
                
            } else { /* primary particle */
                this.ecalScoringPrimaryXY.fill(startPoint[0], startPoint[1]);
            }
        }

        ecalScoringElectronCount.fill(ecalScoringPdgCounts.get(ELECTRON));
        ecalScoringPhotonCount.fill(ecalScoringPdgCounts.get(PHOTON));
        
        /**
         * Trigger Pad plots
         */
        final List<SimCalorimeterHit> triggerPadHits = event.get(SimCalorimeterHit.class, TRIGGER_PAD_SIM_HITS);
        for (SimCalorimeterHit hit : triggerPadHits) {
            triggerPadEnergy.fill(hit.getRawEnergy());
            triggerPadTime.fill(hit.getTime());
        }
    }
    
    public void endOfData() {
    }
        
    private Collection<SimTrackerHit> findHits(List<List<SimTrackerHit>> hits, MCParticle particle) {
        Set<SimTrackerHit> foundHits = new HashSet<SimTrackerHit>();
        for (List<SimTrackerHit> hitsCollection : hits) {
            for (SimTrackerHit hit : hitsCollection) {
                if (hit.getMCParticle().equals(particle)) {
                    foundHits.add(hit);
                }
            }
        }
        return foundHits;
    }
}
