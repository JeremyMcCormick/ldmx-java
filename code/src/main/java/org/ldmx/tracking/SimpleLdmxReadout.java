package org.ldmx.tracking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtTimingConstants;

import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.event.base.BaseRawTrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.CDFSiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.SiElectrodeData;
import org.lcsim.recon.tracking.digitization.sisim.SiElectrodeDataCollection;
import org.lcsim.recon.tracking.digitization.sisim.SiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.config.SimTrackerHitReadoutDriver;

import org.hps.readout.ecal.ClockSingleton;
import org.hps.readout.ecal.ReadoutTimestamp;
import org.hps.readout.ecal.TriggerableDriver;
import org.hps.recon.tracking.PulseShape;
import org.hps.util.RandomGaussian;

/**
 * LDMX Tracker readout simulation.
 *
 * @author <a href="mailto:meeg@slac.stanford.edu">Sho Uemura</a>
 * @author <a href="mailto:omoreno@slac.stanford.edu">Omar Moreno</a> 
 */
public class SimpleLdmxReadout extends TriggerableDriver {

    private SimTrackerHitReadoutDriver readoutDriver = new SimTrackerHitReadoutDriver();
    
    // Charge deposition simulation
    private SiSensorSim siSimulation = new CDFSiSensorSim();

    // Default shaper output shape to use.
    private PulseShape shape = new PulseShape.FourPole();
   
    private Map<SiSensor, PriorityQueue<StripHit>[]> hitMap = new HashMap<SiSensor, PriorityQueue<StripHit>[]>();
    private List<SiSensor> sensors = null;
    
    // Sub-detector name 
    private String subdetectorName = "Tracker"; 

    // Default readout name
    private String readout = "TrackerHits";
    
    // Cuts settings
    private boolean dropBadChannels = true;
    private boolean enablePileupCut = true;
    private boolean enableThresholdCut = true;
    
    private double noiseThreshold = 2.0;
    private double pileupCutoff = 300.0;
    private double readoutOffset = 0.0;
    private double readoutLatency = 280.0;
    private int samplesAboveThreshold = 3;
    private double timeOffset = 30.0;

    // Collection Names
    private String outputCollection = "RawTrackerHits";
    private String trueHitRelationCollectionName = "TrueHitRelations";

    // Flags
    private boolean noPileup = false;
    private boolean addNoise = true;
    private boolean useTimingConditions = false;
    private boolean debug = false;
    private int verbosity = 0;

    // Default constructor
    public SimpleLdmxReadout() {
        add(readoutDriver);
        triggerDelay = 100.0;
    }

    /**
     * Enable/disable the addition of noise to the shaper output samples.
     * 
     * @param addNoise true to add noise, false to disable it.
     */
    public void setAddNoise(boolean addNoise) {
        this.addNoise = addNoise;
    }

    /**
     * Enable/disable debug output.
     * 
     * @param debug true to enable debug, false to disable.
     */
    public void setDebug(boolean debug) { 
        this.debug = debug;
    }
    
    public void setDropBadChannels(boolean dropBadChannels) {
        this.dropBadChannels = dropBadChannels;
    }

    public void setEnablePileupCut(boolean enablePileupCut) {
        this.enablePileupCut = enablePileupCut;
    }

    public void setEnableThresholdCut(boolean enableThresholdCut) {
        this.enableThresholdCut = enableThresholdCut;
    }

    public void setNoPileup(boolean noPileup) {
        this.noPileup = noPileup;
    }
    
    public void setNoiseThreshold(double noiseThreshold) {
        this.noiseThreshold = noiseThreshold;
    }

    public void setRawTrackerHitCollectionName(String outputCollection) { 
        this.outputCollection = outputCollection;
    }
     
    public void setPulseShape(String pulseShape) {
        switch (pulseShape) {
            case "CR-RC":
                shape = new PulseShape.CRRC();
                break;
            case "FourPole":
                shape = new PulseShape.FourPole();
                break;
            default:
                throw new RuntimeException("Unrecognized pulseShape: " + pulseShape);
        }
    }
    
    public void setReadout(String readout) { 
        this.readout = readout; 
    }
    
    public void setReadoutLatency(double readoutLatency) {
        this.readoutLatency = readoutLatency;
    }
    
    public void setSamplesAboveThreshold(int samplesAboveThreshold) {
        this.samplesAboveThreshold = samplesAboveThreshold;
    }

    public void setSubdetectorName(String subdetectorName) { 
        this.subdetectorName = subdetectorName;
    }
    
    public void setUseTimingConditions(boolean useTimingConditions) {
        this.useTimingConditions = useTimingConditions;
    }
   
    /**
     * Set the name of the collection containing the relations between 
     * RawTrackerHits and SimTrackerHits.
     * 
     * @param trueHitRelationCollectionName CollectionName
     */
    public void setTrueHitRelationCollectionName(String trueHitRelationCollectionName) { 
        this.trueHitRelationCollectionName = trueHitRelationCollectionName;
    }

    /**
     * Set the amount of printouts generated by this driver. 0 = silent, 1 =
     * normal, 2+ = debug
     *
     * @param verbosity
     */
    public void setVerbosity(int verbosity) {
        this.verbosity = verbosity;
    }

    /**
     *
     */
    @Override
    public void detectorChanged(Detector detector) {
        super.detectorChanged(detector);

        // Get the collection of all SiSensors from the SVT 
        sensors = detector.getSubdetector(subdetectorName).getDetectorElement().findDescendants(SiSensor.class);
        this.printDebug("Detector: " + subdetectorName + " Total sensors: " + sensors.size());

        String[] readouts = {readout};
        readoutDriver.setCollections(readouts);
        this.printDebug("Using readout: " + readouts.toString());

        if (!noPileup) {
            for (SiSensor sensor : sensors) {
                PriorityQueue<StripHit>[] hitQueues = new PriorityQueue[LdmxConstants.TOTAL_STRIPS_PER_SENSOR];
                hitMap.put(sensor, hitQueues);
            }
        }

        if (useTimingConditions) {
            SvtTimingConstants timingConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtTimingConstants.SvtTimingConstantsCollection.class, "svt_timing_constants").getCachedData().get(0);
            readoutOffset = 4 * (timingConstants.getOffsetPhase() + 3);
            readoutLatency = 248.0 + timingConstants.getOffsetTime();
        }
    }

    /**
     *
     */
    @Override
    public void process(EventHeader event) {
        super.process(event);

        this.printDebug("Running readout simulation.");
        List<StripHit> stripHits = this.doSiSimulation();

        if (!noPileup) {
            for (StripHit stripHit : stripHits) {
                SiSensor sensor = stripHit.sensor;
                int channel = stripHit.channel;

                PriorityQueue<StripHit>[] hitQueues = hitMap.get(sensor);
                if (hitQueues[channel] == null) {
                    hitQueues[channel] = new PriorityQueue<StripHit>();
                }
                hitQueues[channel].add(stripHit);
            }

            // dump stale hits
            for (SiSensor sensor : sensors) {
                PriorityQueue<StripHit>[] hitQueues = hitMap.get(sensor);
                for (int i = 0; i < hitQueues.length; i++) {
                    if (hitQueues[i] != null) {
                        while (!hitQueues[i].isEmpty() && hitQueues[i].peek().time < ClockSingleton.getTime() - (readoutLatency + pileupCutoff)) {
                            //System.out.format("Time %f: Dump stale hit with time %f\n",ClockSingleton.getTime(),hitQueues[i].peek().time);
                            hitQueues[i].poll();
                        }
                        if (hitQueues[i].isEmpty()) {
                            hitQueues[i] = null;
                        }
                    }
                }
            }

            // If an ECal trigger is received, make hits from pipelines
            checkTrigger(event);
        } else {

            // Create a list to hold the analog data
            List<RawTrackerHit> hits = new ArrayList<RawTrackerHit>();
            List<LCRelation> trueHitRelations = new ArrayList<LCRelation>();

            for (StripHit stripHit : stripHits) {
                HpsSiSensor sensor = (HpsSiSensor) stripHit.sensor;
                int channel = stripHit.channel;
                double amplitude = stripHit.amplitude;
                short[] samples = new short[6];

                double[] signal = new double[6];
                for (int sampleN = 0; sampleN < 6; sampleN++) {
                    signal[sampleN] = sensor.getPedestal(channel, sampleN);
                }
                if (addNoise) {
                    this.addNoise(sensor, channel, signal);
                }

                for (int sampleN = 0; sampleN < 6; sampleN++) {
                    double time = sampleN * LdmxConstants.SAMPLING_INTERVAL - timeOffset;
                    shape.setParameters(channel, (HpsSiSensor) sensor);
                    signal[sampleN] += amplitude * shape.getAmplitudePeakNorm(time);//add the pulse to the pedestal
                    samples[sampleN] = (short) Math.round(signal[sampleN]);
                    //this.printDebug("\t\tMaking samples: sample#" + sampleN + " has " + samples[sampleN] + " ADC counts");
                }

                long channel_id = sensor.makeChannelID(channel);
                RawTrackerHit hit = new BaseRawTrackerHit(0, channel_id, samples, new ArrayList<SimTrackerHit>(stripHit.simHits), sensor);
                if (readoutCuts(hit)) {
                    hits.add(hit);
                    for (SimTrackerHit simHit : hit.getSimTrackerHits()) {
                        LCRelation hitRelation = new BaseLCRelation(hit, simHit);
                        trueHitRelations.add(hitRelation);
                    }
                }
            }

            int flags = 1 << LCIOConstants.TRAWBIT_ID1;
            event.put(outputCollection, hits, RawTrackerHit.class, flags, readout);
            event.put(trueHitRelationCollectionName, trueHitRelations, LCRelation.class, 0);
        
            this.printDebug("Made " + hits.size() + " RawTrackerHits");
            this.printDebug("Made " + trueHitRelations.size() + " LCRelations");
        }
    }

    /**
     *
     * @return Collection of StripHits
     */
    private List<StripHit> doSiSimulation() {

        List<StripHit> stripHits = new ArrayList<StripHit>();

        for (SiSensor sensor : sensors) {
            
            this.printDebug("Processing hits on sensor " + sensor.toString());
            
            // Set the sensor to be used in the charge deposition simulation
            siSimulation.setSensor(sensor);

            // Perform the charge deposition simulation
            Map<ChargeCarrier, SiElectrodeDataCollection> electrodeDataMap = siSimulation.computeElectrodeData();

            SiElectrodeDataCollection electrodeDataCol = electrodeDataMap.get(ChargeCarrier.HOLE);

            // If there is no electrode data available create a new instance of electrode
            // data
            if (electrodeDataCol == null) {
                electrodeDataCol = new SiElectrodeDataCollection();
            }
            
            // Loop over all sensor channels
            for (Integer channel : electrodeDataCol.keySet()) {
            
                this.printDebug("Processing channel " + channel);
                
                // Get the electrode data for this channel
                SiElectrodeData electrodeData = electrodeDataCol.get(channel);
                Set<SimTrackerHit> simHits = electrodeData.getSimulatedHits();
                
                // compute hit time as the unweighted average of SimTrackerHit times; this
                // is dumb but okay since there's generally only one SimTrackerHit
                double time = 0.0;
                for (SimTrackerHit hit : simHits) {
                    time += hit.getTime();
                }
                time /= simHits.size();
                time += ClockSingleton.getTime();
                this.printDebug("Hit time: " + time);
                        
                // Get the charge in units of electrons
                double charge = electrodeData.getCharge();
                this.printDebug("Hit charge: " + charge);

                double resistorValue = 100; // Ohms
                double inputStageGain = 1.5;
                // FIXME: This should use the gains instead
                double amplitude = (charge / LdmxConstants.MIP) * resistorValue * inputStageGain * Math.pow(2, 14) / 2000;
                this.printDebug("Hit amplitude: " + amplitude);

                stripHits.add(new StripHit(sensor, channel, amplitude, time, simHits));
            }
            
            // Clear the sensors of all deposited charge
            siSimulation.clearReadout();
        }
        this.printDebug("Total strip hits: " + stripHits.size());
        return stripHits;
    }

    private void addNoise(SiSensor sensor, int channel, double[] signal) {
        for (int sampleN = 0; sampleN < 6; sampleN++) {
            signal[sampleN] += RandomGaussian.getGaussian(0, ((HpsSiSensor) sensor).getNoise(channel, sampleN));
        }
    }

    private boolean readoutCuts(RawTrackerHit hit) {
        if (enableThresholdCut && !samplesAboveThreshold(hit)) {
            if (verbosity > 1) {
                System.out.println("Failed threshold cut");
            }
            return false;
        }
        if (enablePileupCut && !pileupCut(hit)) {
            if (verbosity > 1) {
                System.out.println("Failed pileup cut");
            }
            return false;
        }
        if (dropBadChannels && !badChannelCut(hit)) {
            if (verbosity > 1) {
                System.out.println("Failed bad channel cut");
            }
            return false;
        }
        return true;
    }

    private boolean badChannelCut(RawTrackerHit hit) {
        HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
        int channel = hit.getIdentifierFieldValue("strip");
        return !sensor.isBadChannel(channel);
    }

    private boolean pileupCut(RawTrackerHit hit) {
        short[] samples = hit.getADCValues();
        return (samples[2] > samples[1] || samples[3] > samples[2]);
    }

    private boolean samplesAboveThreshold(RawTrackerHit hit) {
        HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
        int channel = hit.getIdentifierFieldValue("strip");
        double pedestal;
        double noise;
        int count = 0;
        short[] samples = hit.getADCValues();
        for (int sampleN = 0; sampleN < samples.length; sampleN++) {
            pedestal = sensor.getPedestal(channel, sampleN);
            noise = sensor.getNoise(channel, sampleN);
            if (verbosity > 1) {
                System.out.format("%f, %f\n", samples[sampleN] - pedestal, noise * noiseThreshold);
            }
            if (samples[sampleN] - pedestal > noise * noiseThreshold) {
                count++;
            }
        }
        return count >= samplesAboveThreshold;
    }

    @Override
    protected void processTrigger(EventHeader event) {
        if (noPileup) {
            return;
        }
        //System.out.println("Got trigger");

        // Create a list to hold the analog data
        List<RawTrackerHit> hits = new ArrayList<RawTrackerHit>();
        List<LCRelation> trueHitRelations = new ArrayList<LCRelation>();
        // Calculate time of first sample
        double firstSample = Math.floor((ClockSingleton.getTime() - readoutLatency - readoutOffset) / LdmxConstants.SAMPLING_INTERVAL) * LdmxConstants.SAMPLING_INTERVAL + readoutOffset;

        for (SiSensor sensor : sensors) {
            PriorityQueue<StripHit>[] hitQueues = hitMap.get(sensor);
            for (int channel = 0; channel < hitQueues.length; channel++) {
                if (!addNoise && (hitQueues[channel] == null || hitQueues[channel].isEmpty())) {
                    continue;
                }
                double[] signal = new double[6];
                for (int sampleN = 0; sampleN < 6; sampleN++) {
                    signal[sampleN] = ((HpsSiSensor) sensor).getPedestal(channel, sampleN);
                }
                if (addNoise) {
                    addNoise(sensor, channel, signal);
                }

                List<SimTrackerHit> simHits = new ArrayList<SimTrackerHit>();

                if (hitQueues[channel] != null) {
                    for (StripHit hit : hitQueues[channel]) {
                        double totalContrib = 0;
                        double meanNoise = 0;
                        for (int sampleN = 0; sampleN < 6; sampleN++) {
                            double sampleTime = firstSample + sampleN * LdmxConstants.SAMPLING_INTERVAL;
                            shape.setParameters(channel, (HpsSiSensor) sensor);
                            double signalAtTime = hit.amplitude * shape.getAmplitudePeakNorm(sampleTime - hit.time);
                            totalContrib += signalAtTime;
                            signal[sampleN] += signalAtTime;
                            meanNoise += ((HpsSiSensor) sensor).getNoise(channel, sampleN);
                            //System.out.format("new value of signal[%d] = %f\n", sampleN, signal[sampleN]);
                        }
                        meanNoise /= 6;
                        // Compare to the mean noise of the six samples instead
                        if (totalContrib > 4.0 * meanNoise) {
                            //System.out.format("adding %d simHits\n", hit.simHits.size());
                            simHits.addAll(hit.simHits);
                        }
                    }
                }

                short[] samples = new short[6];
                for (int sampleN = 0; sampleN < 6; sampleN++) {
                    samples[sampleN] = (short) Math.round(signal[sampleN]);
                }
                long channel_id = ((HpsSiSensor) sensor).makeChannelID(channel);
                RawTrackerHit hit = new BaseRawTrackerHit(0, channel_id, samples, simHits, sensor);
                if (readoutCuts(hit)) {
                    hits.add(hit);
                    //System.out.format("simHits: %d\n", simHits.size());
                    for (SimTrackerHit simHit : hit.getSimTrackerHits()) {
                        LCRelation hitRelation = new BaseLCRelation(hit, simHit);
                        trueHitRelations.add(hitRelation);
                    }
                }
            }
        }

        int flags = 1 << LCIOConstants.TRAWBIT_ID1;
        event.put(outputCollection, hits, RawTrackerHit.class, flags, readout);
        event.put(trueHitRelationCollectionName, trueHitRelations, LCRelation.class, 0);
        if (verbosity >= 1) {
            System.out.println("Made " + hits.size() + " RawTrackerHits");
            System.out.println("Made " + trueHitRelations.size() + " LCRelations");
        }
    }

    @Override
    public double readoutDeltaT() {
        double triggerTime = ClockSingleton.getTime() + triggerDelay;
        // Calculate time of first sample
        double firstSample = Math.floor((triggerTime - readoutLatency - readoutOffset) / LdmxConstants.SAMPLING_INTERVAL) * LdmxConstants.SAMPLING_INTERVAL + readoutOffset;

        return firstSample;
    }

    private class StripHit implements Comparable {

        SiSensor sensor;
        int channel;
        double amplitude;
        double time;
        Set<SimTrackerHit> simHits;

        public StripHit(SiSensor sensor, int channel, double amplitude, double time, Set<SimTrackerHit> simHits) {
            this.sensor = sensor;
            this.channel = channel;
            this.amplitude = amplitude;
            this.time = time;
            this.simHits = simHits;
        }

        @Override
        public int compareTo(Object o) {
            double deltaT = time - ((StripHit) o).time;
            if (deltaT > 0) {
                return 1;
            } else if (deltaT < 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    @Override
    public int getTimestampType() {
        return ReadoutTimestamp.SYSTEM_TRACKER;
    }
    
    private void printDebug(String message) { 
        if (this.debug) System.out.println("[ SimpleLdmxReadout ]: " + message);
    }
}
