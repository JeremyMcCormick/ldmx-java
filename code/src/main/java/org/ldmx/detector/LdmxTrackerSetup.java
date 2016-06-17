package org.ldmx.detector;

import java.util.List; 
import java.util.logging.Logger; 

import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;

/**
 * Class used to load calibrations onto the sensors used by the LDMX Tagger 
 * and Recoil trackers.
 * 
 * @author <a href="mailto:omoreno@slac.stanford.edu">Omar Moreno</a> 
 */
public class LdmxTrackerSetup extends Driver {

    /** Initialize Logger */
    private static Logger LOGGER = Logger.getLogger(LdmxTrackerSetup.class.getPackage().getName());
    
    /** Name of the LDMX Tagger and Recoil subdetectors in the detector model. */
    private static final String TAGGER_TRACKER_NAME = "TaggerTracker";
    private static final String RECOIL_TRACKER_NAME = "RecoilTracker";

    /** Channel noise in ADC counts */
    private static final double[] CHANNEL_NOISE = new double[]{ 60, 60, 60, 60, 60, 60 };
    
    /** Channel baseline */
    private static final double[] CHANNEL_BASELINE = new double[]{ 3000, 3000, 3000, 3000, 3000, 3000 }; 
   
    /** Channel gain */
    private static final double CHANNEL_GAIN = 1; 
    
    /** Channel offset */ 
    private static final double CHANNEL_OFFSET = 0; 
    
    /** Channel t0 shift */
    private static final double CHANNEL_T0_SHIFT = 0; 
    
    /** Shaper fit parameters */
    private static final double[] SHAPER_FIT_PARAMETERS = new double[]{2000, 50, 10, 20};
    
    protected void detectorChanged(Detector detector) { 
        
        final List<HpsSiSensor> taggerSensors 
            = detector.getSubdetector(TAGGER_TRACKER_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);
        LOGGER.info("Setting up " + taggerSensors.size() + " Tagger tracker sensors");
       
        this.setup(taggerSensors);
        
        final List<HpsSiSensor> recoilSensors 
            = detector.getSubdetector(RECOIL_TRACKER_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);
        LOGGER.info("Setting up " + recoilSensors.size() + " Recoil tracker sensors");
    
        this.setup(recoilSensors);
        
    }
    
    private void setup(List<HpsSiSensor> sensors) {
       
        // Loop over all of the sensors
        for (final HpsSiSensor sensor : sensors) {

            // Reset possible existing conditions data on sensor.
            sensor.reset();
           
            // Get the total number of readout strips that this sensor contains
            double totalChannels = sensor.getReadoutElectrodes(ChargeCarrier.HOLE).getNCells(); 
            
            // Loop through all of the sensor channels and set some default 
            // conditions.
            for (int channelNumber = 0; channelNumber < totalChannels; channelNumber++) {
                
                // Set the channel baseline and noise
                sensor.setPedestal(channelNumber, CHANNEL_BASELINE);
                sensor.setNoise(channelNumber, CHANNEL_NOISE);
                
                // Set the channel gain and offset
                sensor.setGain(channelNumber, CHANNEL_GAIN);
                sensor.setOffset(channelNumber, CHANNEL_OFFSET);
            
                // Set the t0 shift for the sensor
                sensor.setT0Shift(CHANNEL_T0_SHIFT);

                // Set the shape fit parameters
                sensor.setShapeFitParameters(channelNumber, SHAPER_FIT_PARAMETERS);
            }
            LOGGER.info(sensor.toString());
        }
    }
}	
