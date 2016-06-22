package org.ldmx;

/**
 * Constants for LDMX event collections and subdetector names derived from
 * initial 'ldmx-det-v0' detector description.
 * 
 * @author jeremym
 */
public final class EventConstants {
    
    /* detector names */
    public static final String TAGGER = "TaggerTracker";
    public static final String RECOIL_TRACKER = "RecoilTracker";
    public static final String ECAL = "Ecal";
    public static final String ECAL_SCORING = "EcalScoring";
    public static final String TARGET = "Target";
    
    /* hits collection names */
    public static final String TAGGER_SIM_HITS = "TaggerTrackerHits";
    public static final String TAGGER_RAW_HITS = "TaggerRawTrackerHits";
    public static final String RECOIL_TRACKER_SIM_HITS = "RecoilTrackerHits";
    public static final String RECOIL_TRACKER_RAW_HITS = "RecoilRawTrackerHits";
    public static final String ECAL_SIM_HITS = "EcalHits";
    public static final String ECAL_SCORING_SIM_HITS = "EcalScoringHits";
        
    /* MCParticle collection */
    public static final String MCPARTICLES = "MCParticle";
    
    /* unit conversion from GeV */
    public static double MeV = 1000.0;
    public static double KeV = 1000000.0;
           
    private EventConstants() {
        throw new RuntimeException("Do not instantiate this class!");
    }
}
