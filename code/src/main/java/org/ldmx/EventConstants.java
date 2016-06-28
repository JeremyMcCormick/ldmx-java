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
    public static final String RECOIL = "RecoilTracker";
    public static final String ECAL = "Ecal";
    public static final String ECAL_SCORING = "EcalScoring";
    public static final String TARGET = "Target";
    
    /* hits collection names */
    public static final String TAGGER_SIM_HITS = "TaggerTrackerHits";
    public static final String TAGGER_RAW_HITS = "TaggerRawTrackerHits";
    public static final String RECOIL_SIM_HITS = "RecoilTrackerHits";
    public static final String RECOIL_RAW_HITS = "RecoilRawTrackerHits";
    public static final String ECAL_SIM_HITS = "EcalHits";
    public static final String ECAL_SCORING_SIM_HITS = "EcalScoringHits";
        
    /* MCParticle collection */
    public static final String MCPARTICLES = "MCParticle";
              
    private EventConstants() {
        throw new RuntimeException("Do not instantiate this class!");
    }
}
