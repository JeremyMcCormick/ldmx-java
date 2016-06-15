package org.ldmx.tracking;

/**
 * Constants used by the LDMX tracker readout simulation.
 *  
 * @author <a href="mailto:omoreno@slac.stanford.edu">Omar Moreno</a> 
 */
public class LdmxConstants {

    // Total number of strips per sensor
    public static final int TOTAL_STRIPS_PER_SENSOR = 639;

    // Time intervals at which an APV25 shaper output is sampled at.
    public static final double SAMPLING_INTERVAL = 24.0; // [ns]
    
    // Total number of shaper signal samples obtained.
    public static final int TOTAL_NUMBER_OF_SAMPLES = 6;

    // Approximate number of electron-hole pairs created by a min. ionizing 
    // particle in 300 micrometers of Si.
    public static final int MIP = 25000; // electrons
}
